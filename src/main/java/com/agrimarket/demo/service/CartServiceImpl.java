package com.agrimarket.demo.service;

import com.agrimarket.demo.dto.CheckoutResponse;
import com.agrimarket.demo.dto.OrderWithDetailsDTO;
import com.agrimarket.demo.dto.PaymentResponse;
import com.agrimarket.demo.model.mongo.Cart;
import com.agrimarket.demo.model.mongo.CartItem;
import com.agrimarket.demo.model.mongo.Customer;
import com.agrimarket.demo.model.mongo.Product;
import com.agrimarket.demo.model.sql.Order;
import com.agrimarket.demo.model.sql.OrderDetail;
import com.agrimarket.demo.model.sql.Payment;
import com.agrimarket.demo.repository.mongo.CartRepository;
import com.agrimarket.demo.repository.mongo.CustomerRepository;
import com.agrimarket.demo.repository.mongo.ProductRepository;
import com.agrimarket.demo.repository.sql.OrderDetailRepository;
import com.agrimarket.demo.repository.sql.OrderRepository;
import com.agrimarket.demo.repository.sql.PaymentRepository;
import com.agrimarket.demo.repository.sql.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.security.SecureRandom;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private static final int MAX_ITEM_QUANTITY = 10_000;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CartRepository cartRepository;
    private final ReactiveMongoTemplate mongoTemplate;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final StoreRepository storeRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final PaymentRepository paymentRepository;
    private final TransactionalOperator transactionalOperator;

    @Override
    public Mono<Cart> getActiveCart(String email, String sessionToken) {
        if (email != null && !email.isBlank()) {
            return findCustomer(email).flatMap(customer -> cartRepository
                    .findByCustomerId(String.valueOf(customer.getId()))
                    .switchIfEmpty(Mono.defer(() -> createCart(String.valueOf(customer.getId()), null))));
        }
        if (sessionToken == null || sessionToken.isBlank()) {
            return createCart(null, UUID.randomUUID().toString());
        }
        if (!isValidSessionToken(sessionToken)) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "sessionToken debe tener entre 16 y 128 caracteres"));
        }
        return cartRepository.findBySessionToken(sessionToken)
                .switchIfEmpty(Mono.defer(() -> createCart(null, sessionToken)));
    }

    @Override
    public Mono<Cart> addItem(String email, String sessionToken, String productId, Integer quantity) {
        if (!isValidQuantity(quantity)) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "quantity debe estar entre 1 y " + MAX_ITEM_QUANTITY));
        }
        return findOrCreateActiveCart(email, sessionToken)
                .flatMap(cart -> findActiveProduct(productId).flatMap(product -> {
                    ensureCartIsOpen(cart);
                    CartItem existing = cart.getItems().stream()
                            .filter(item -> item.getProductId().equals(productId))
                            .findFirst().orElse(null);
                    if (existing == null) {
                        existing = CartItem.builder()
                                .productId(productId)
                                .productName(product.getName())
                                .unitPrice(product.getBasePrice())
                                .quantity(quantity)
                                .build();
                        cart.getItems().add(existing);
                    } else {
                        if ((long) existing.getQuantity() + quantity > MAX_ITEM_QUANTITY) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "La cantidad del producto supera el máximo permitido");
                        }
                        existing.setQuantity(existing.getQuantity() + quantity);
                        existing.setProductName(product.getName());
                        existing.setUnitPrice(product.getBasePrice());
                    }
                    updateCartTotals(cart);
                    return cartRepository.save(cart);
                }));
    }

    @Override
    public Mono<Cart> updateItem(String email, String sessionToken, String productId, Integer quantity) {
        if (!isValidQuantity(quantity)) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "quantity debe estar entre 1 y " + MAX_ITEM_QUANTITY));
        }
        return findOrCreateActiveCart(email, sessionToken)
                .flatMap(cart -> {
                    ensureCartIsOpen(cart);
                    CartItem item = cart.getItems().stream()
                            .filter(cartItem -> cartItem.getProductId().equals(productId))
                            .findFirst()
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.NOT_FOUND, "El producto no está en el carrito"));
                    item.setQuantity(quantity);
                    updateCartTotals(cart);
                    return cartRepository.save(cart);
                });
    }

    @Override
    public Mono<Cart> removeItem(String email, String sessionToken, String productId) {
        return findOrCreateActiveCart(email, sessionToken)
                .flatMap(cart -> {
                    ensureCartIsOpen(cart);
                    boolean removed = cart.getItems().removeIf(item -> item.getProductId().equals(productId));
                    if (!removed) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "El producto no está en el carrito"));
                    }
                    updateCartTotals(cart);
                    return cartRepository.save(cart);
                });
    }

    @Override
    public Mono<Void> mergeGuestCart(String sessionToken, String customerId) {
        return cartRepository.findBySessionToken(sessionToken)
                .flatMap(guestCart -> cartRepository.findByCustomerId(customerId)
                        .flatMap(customerCart -> {
                            if (guestCart.getId().equals(customerCart.getId())) {
                                guestCart.setSessionToken(null);
                                guestCart.setUpdatedAt(LocalDateTime.now());
                                return cartRepository.save(guestCart).thenReturn(Boolean.TRUE);
                            }
                            for (CartItem guestItem : guestCart.getItems()) {
                                CartItem existing = customerCart.getItems().stream()
                                        .filter(item -> item.getProductId().equals(guestItem.getProductId()))
                                        .findFirst().orElse(null);
                                if (existing == null) {
                                    customerCart.getItems().add(guestItem);
                                } else {
                                    existing.setQuantity(existing.getQuantity() + guestItem.getQuantity());
                                    existing.setSubtotal(existing.getUnitPrice()
                                            .multiply(BigDecimal.valueOf(existing.getQuantity())));
                                }
                            }
                            updateCartTotals(customerCart);
                            return cartRepository.save(customerCart)
                                    .then(cartRepository.delete(guestCart))
                                    .thenReturn(Boolean.TRUE);
                        })
                        .switchIfEmpty(Mono.defer(() -> {
                            guestCart.setCustomerId(customerId);
                            guestCart.setSessionToken(null);
                            guestCart.setUpdatedAt(LocalDateTime.now());
                            return cartRepository.save(guestCart).thenReturn(Boolean.TRUE);
                        })))
                .then();
    }

    @Override
    public Mono<CheckoutResponse> checkout(String email, Integer storeId, String paymentMethod) {
        if (!List.of("YAPE", "TARJETA_CREDITO", "PAGO_EFECTIVO").contains(paymentMethod)) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Método de pago no soportado"));
        }
        return findCustomer(email)
                .flatMap(customer -> storeRepository.existsByStoreIdAndIsActiveTrue(storeId)
                        .filter(Boolean::booleanValue)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "La tienda indicada no existe o no está disponible")))
                        .then(cartRepository.findByCustomerId(String.valueOf(customer.getId()))
                                .switchIfEmpty(Mono.error(new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST, "No hay un carrito activo"))))
                        .flatMap(cart -> checkout(customer, cart, storeId, paymentMethod)));
    }

    private Mono<CheckoutResponse> checkout(
            Customer customer, Cart cart, Integer storeId, String paymentMethod) {
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "El carrito está vacío"));
        }
        return orderRepository.findByCheckoutKey(cart.getId())
                .flatMap(order -> findCheckoutResponse(order)
                        .flatMap(result -> cartRepository.deleteById(cart.getId()).thenReturn(result)))
                .switchIfEmpty(Mono.defer(() -> refreshAndValidateProducts(cart)
                .then(validateStock(cart.getItems(), storeId))
                .then(reserveCartForCheckout(cart.getId(), cart.getVersion()))
                .flatMap(reservedCart -> {
                    LocalDateTime now = LocalDateTime.now();
                    Order order = Order.builder()
                            .orderDate(now)
                            .status("PENDING_PAYMENT")
                            .deliveryType("recogida")
                            .totalAmount(cart.getTotalAmount())
                            .storeId(storeId)
                            .customerId(String.valueOf(customer.getId()))
                            .checkoutKey(cart.getId())
                            .createdAt(now)
                            .updatedAt(now)
                            .build();

                    Mono<CheckoutResponse> sqlFlow = orderRepository.save(order)
                            .flatMap(savedOrder -> Flux.fromIterable(cart.getItems())
                                    .concatMap(item -> {
                                        OrderDetail detail = OrderDetail.builder()
                                                .quantity(item.getQuantity())
                                                .unitPrice(item.getUnitPrice())
                                                .productId(item.getProductId())
                                                .orderId(savedOrder.getOrderId())
                                                .build();
                                        return orderDetailRepository.save(detail);
                                    })
                                    .collectList()
                                    .flatMap(details -> Flux.fromIterable(cart.getItems())
                                            .concatMap(item -> inventoryService.decreaseStock(
                                                    item.getProductId(), storeId, item.getQuantity()))
                                            .then(Mono.defer(() -> createSimulatedPayment(
                                                    savedOrder, paymentMethod, now)))
                                            .map(payment -> new CheckoutResponse(
                                                    OrderWithDetailsDTO.from(savedOrder, details),
                                                    PaymentResponse.from(payment)))));
                    return transactionalOperator.transactional(sqlFlow)
                            .onErrorResume(sqlError -> orderRepository.findByCheckoutKey(cart.getId())
                                    .flatMap(this::findCheckoutResponse)
                                    .switchIfEmpty(Mono.defer(() -> reopenCart(cart.getId())
                                            .then(Mono.error(sqlError)))))
                            .flatMap(createdOrder -> cartRepository.deleteById(cart.getId())
                                    .thenReturn(createdOrder));
                })));
    }

    private Mono<Payment> createSimulatedPayment(Order order, String paymentMethod, LocalDateTime now) {
        boolean cashAtAgent = "PAGO_EFECTIVO".equals(paymentMethod);
        LocalDateTime expiresAt = now.plus(cashAtAgent
                ? java.time.Duration.ofHours(24)
                : java.time.Duration.ofMinutes(15));
        String cipCode = cashAtAgent
                ? String.format(java.util.Locale.ROOT, "%08d", SECURE_RANDOM.nextInt(100_000_000))
                : null;
        String referenceCode = cashAtAgent ? cipCode : "SIM-" + UUID.randomUUID().toString().substring(0, 8);
        Payment payment = Payment.builder()
                .orderId(order.getOrderId())
                .paymentMethod(paymentMethod)
                .paymentStatus("PENDING")
                .amount(order.getTotalAmount())
                .referenceCode(referenceCode)
                .cipCode(cipCode)
                .expiresAt(expiresAt)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return paymentRepository.save(payment);
    }

    private Mono<CheckoutResponse> findCheckoutResponse(Order order) {
        return findOrderWithDetails(order)
                .flatMap(orderDTO -> paymentRepository.findByOrderId(order.getOrderId())
                        .map(payment -> new CheckoutResponse(orderDTO, PaymentResponse.from(payment)))
                        .switchIfEmpty(Mono.just(new CheckoutResponse(orderDTO, null))));
    }

    private Mono<Void> refreshAndValidateProducts(Cart cart) {
        List<CartItem> items = cart.getItems();
        List<Long> productIds = new ArrayList<>();
        try {
            for (CartItem item : items) {
                productIds.add(Long.valueOf(item.getProductId()));
            }
        } catch (NumberFormatException exception) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El carrito contiene un productId inválido"));
        }
        return productRepository.findAllById(productIds.stream().distinct().toList())
                .collectMap(Product::getId)
                .flatMap(products -> {
                    if (products.size() != productIds.stream().distinct().count()) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Uno o más productos del carrito ya no existen"));
                    }
                    for (CartItem item : items) {
                        Product product = products.get(Long.valueOf(item.getProductId()));
                        if (!Boolean.TRUE.equals(product.getIsActive())) {
                            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "El producto no está disponible: " + item.getProductId()));
                        }
                        item.setProductName(product.getName());
                        item.setUnitPrice(product.getBasePrice());
                    }
                    updateCartTotals(cart);
                    return Mono.empty();
                });
    }

    private Mono<Cart> reserveCartForCheckout(String cartId, Long expectedVersion) {
        LocalDateTime leaseExpiredAt = LocalDateTime.now().minusMinutes(2);
        Query query = Query.query(new Criteria().andOperator(
                Criteria.where("_id").is(cartId),
                Criteria.where("version").is(expectedVersion),
                new Criteria().orOperator(
                        Criteria.where("checkoutState").ne("PROCESSING"),
                        Criteria.where("checkoutStartedAt").lt(leaseExpiredAt))));
        return mongoTemplate.findAndModify(
                        query,
                        new Update()
                                .set("checkoutState", "PROCESSING")
                                .set("checkoutStartedAt", LocalDateTime.now())
                                .set("updatedAt", LocalDateTime.now())
                                .inc("version", 1),
                        FindAndModifyOptions.options().returnNew(true),
                        Cart.class)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.CONFLICT, "El checkout de este carrito ya está en proceso; reintente")));
    }

    private Mono<Void> reopenCart(String cartId) {
        Query query = Query.query(Criteria.where("_id").is(cartId)
                .and("checkoutState").is("PROCESSING"));
        return mongoTemplate.updateFirst(query,
                new Update().set("checkoutState", "OPEN")
                        .unset("checkoutStartedAt")
                        .set("updatedAt", LocalDateTime.now())
                        .inc("version", 1),
                Cart.class).then();
    }

    private Mono<OrderWithDetailsDTO> findOrderWithDetails(Order order) {
        return orderDetailRepository.findByOrderId(order.getOrderId())
                .collectList()
                .map(details -> OrderWithDetailsDTO.from(order, details));
    }

    private void ensureCartIsOpen(Cart cart) {
        if ("PROCESSING".equals(cart.getCheckoutState())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El carrito está en proceso de checkout");
        }
    }

    private boolean isValidQuantity(Integer quantity) {
        return quantity != null && quantity > 0 && quantity <= MAX_ITEM_QUANTITY;
    }

    private Mono<Void> validateStock(List<CartItem> items, Integer storeId) {
        return Flux.fromIterable(items)
                .concatMap(item -> inventoryService.hasStock(item.getProductId(), storeId, item.getQuantity())
                        .flatMap(hasStock -> hasStock
                                ? Mono.empty()
                                : Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Stock insuficiente para el producto " + item.getProductId()))))
                .then();
    }

    private Mono<Customer> findCustomer(String email) {
        return customerRepository.findByEmailIgnoreCase(email)
                .filter(customer -> Boolean.TRUE.equals(customer.getIsActive()))
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Customer no encontrado o inactivo")));
    }

    private Mono<Cart> findOrCreateActiveCart(String email, String sessionToken) {
        if (email != null && !email.isBlank()) {
            return findCustomer(email).flatMap(customer -> cartRepository
                    .findByCustomerId(String.valueOf(customer.getId()))
                    .switchIfEmpty(Mono.defer(() -> createCart(String.valueOf(customer.getId()), null))));
        }
        if (!isValidSessionToken(sessionToken)) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Envía el token del carrito en el header X-Session-Token"));
        }
        return cartRepository.findBySessionToken(sessionToken)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe un carrito para ese X-Session-Token")));
    }

    private boolean isValidSessionToken(String sessionToken) {
        return sessionToken != null && sessionToken.length() >= 16 && sessionToken.length() <= 128;
    }

    private Mono<Product> findActiveProduct(String productId) {
        try {
            long id = Long.parseLong(productId);
            return productRepository.findById(id)
                    .filter(product -> Boolean.TRUE.equals(product.getIsActive()))
                    .switchIfEmpty(Mono.error(new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Producto no encontrado o inactivo: " + productId)));
        } catch (NumberFormatException exception) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "productId inválido: " + productId));
        }
    }

    private Mono<Cart> createCart(String customerId, String sessionToken) {
        LocalDateTime now = LocalDateTime.now();
        Cart cart = Cart.builder()
                .customerId(customerId)
                .sessionToken(sessionToken)
                .items(new ArrayList<>())
                .totalAmount(BigDecimal.ZERO)
                .checkoutState("OPEN")
                .checkoutStartedAt(null)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return cartRepository.save(cart);
    }

    private void updateCartTotals(Cart cart) {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            item.setSubtotal(subtotal);
            total = total.add(subtotal);
        }
        cart.setTotalAmount(total);
        cart.setUpdatedAt(LocalDateTime.now());
    }
}
