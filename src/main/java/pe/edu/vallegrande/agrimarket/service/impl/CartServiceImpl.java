package pe.edu.vallegrande.agrimarket.service.impl;

import pe.edu.vallegrande.agrimarket.service.*;
import pe.edu.vallegrande.agrimarket.dto.CheckoutResponse;
import pe.edu.vallegrande.agrimarket.dto.OrderWithDetailsDTO;
import pe.edu.vallegrande.agrimarket.dto.PaymentResponse;
import pe.edu.vallegrande.agrimarket.model.mongo.Cart;
import pe.edu.vallegrande.agrimarket.model.mongo.CartItem;
import pe.edu.vallegrande.agrimarket.model.mongo.Customer;
import pe.edu.vallegrande.agrimarket.model.mongo.Product;
import pe.edu.vallegrande.agrimarket.model.sql.Order;
import pe.edu.vallegrande.agrimarket.model.sql.OrderDetail;
import pe.edu.vallegrande.agrimarket.model.sql.Payment;
import pe.edu.vallegrande.agrimarket.repository.mongo.CartRepository;
import pe.edu.vallegrande.agrimarket.repository.mongo.CustomerRepository;
import pe.edu.vallegrande.agrimarket.repository.mongo.ProductRepository;
import pe.edu.vallegrande.agrimarket.repository.sql.OrderDetailRepository;
import pe.edu.vallegrande.agrimarket.repository.sql.OrderRepository;
import pe.edu.vallegrande.agrimarket.repository.sql.PaymentRepository;
import pe.edu.vallegrande.agrimarket.repository.sql.StoreRepository;
import pe.edu.vallegrande.agrimarket.validation.ValidationLimits;
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
import java.util.Map;
import java.util.Set;
import java.security.SecureRandom;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private static final int MAX_ITEM_QUANTITY = 10_000;
    private static final int MIN_SESSION_TOKEN_LENGTH = 16;
    private static final int CHECKOUT_LEASE_MINUTES = 2;
    private static final int CASH_PAYMENT_EXPIRATION_HOURS = 24;
    private static final int ONLINE_PAYMENT_EXPIRATION_MINUTES = 15;
    private static final int CIP_CODE_DIGITS = 8;
    private static final int CIP_CODE_BOUND = 100_000_000;
    private static final String CASH_PAYMENT_METHOD = "PAGO_EFECTIVO";
    private static final String PENDING_ORDER_STATUS = "PENDING_PAYMENT";
    private static final String PENDING_PAYMENT_STATUS = "PENDING";
    private static final String PROCESSING_CHECKOUT_STATE = "PROCESSING";
    private static final String OPEN_CART_STATE = "OPEN";
    private static final String SIMULATED_REFERENCE_PREFIX = "SIM-";
    private static final String PICKUP_DELIVERY_TYPE = "recogida";
    private static final String YAPE_PAYMENT_METHOD = "YAPE";
    private static final String CREDIT_CARD_PAYMENT_METHOD = "TARJETA_CREDITO";
    private static final Set<String> SUPPORTED_PAYMENT_METHODS = Set.of(
            YAPE_PAYMENT_METHOD, CREDIT_CARD_PAYMENT_METHOD, CASH_PAYMENT_METHOD);
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
                    HttpStatus.BAD_REQUEST,
                    "sessionToken debe tener entre " + MIN_SESSION_TOKEN_LENGTH + " y "
                            + ValidationLimits.MAX_SESSION_TOKEN_LENGTH + " caracteres"));
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
                .flatMap(cart -> findActiveProduct(productId)
                        .flatMap(product -> addProductToCart(cart, product, productId, quantity)));
    }

    private Mono<Cart> addProductToCart(Cart cart, Product product, String productId, Integer quantity) {
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
            updateExistingCartItem(existing, product, quantity);
        }
        updateCartTotals(cart);
        return cartRepository.save(cart);
    }

    private void updateExistingCartItem(CartItem item, Product product, Integer quantity) {
        if ((long) item.getQuantity() + quantity > MAX_ITEM_QUANTITY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La cantidad del producto supera el máximo permitido");
        }
        item.setQuantity(item.getQuantity() + quantity);
        item.setProductName(product.getName());
        item.setUnitPrice(product.getBasePrice());
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
                        .flatMap(customerCart -> mergeIntoCustomerCart(guestCart, customerCart))
                        .switchIfEmpty(Mono.defer(() -> attachGuestCart(guestCart, customerId))))
                .then();
    }

    private Mono<Boolean> mergeIntoCustomerCart(Cart guestCart, Cart customerCart) {
        if (guestCart.getId().equals(customerCart.getId())) {
            return unlinkGuestSession(guestCart);
        }
        mergeGuestItems(guestCart, customerCart);
        updateCartTotals(customerCart);
        return cartRepository.save(customerCart)
                .then(cartRepository.delete(guestCart))
                .thenReturn(Boolean.TRUE);
    }

    private Mono<Boolean> unlinkGuestSession(Cart guestCart) {
        guestCart.setSessionToken(null);
        guestCart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(guestCart).thenReturn(Boolean.TRUE);
    }

    private void mergeGuestItems(Cart guestCart, Cart customerCart) {
        for (CartItem guestItem : guestCart.getItems()) {
            CartItem existing = customerCart.getItems().stream()
                    .filter(item -> item.getProductId().equals(guestItem.getProductId()))
                    .findFirst().orElse(null);
            if (existing == null) {
                customerCart.getItems().add(guestItem);
            } else {
                mergeCartItem(existing, guestItem);
            }
        }
    }

    private void mergeCartItem(CartItem existing, CartItem guestItem) {
        existing.setQuantity(existing.getQuantity() + guestItem.getQuantity());
        existing.setSubtotal(existing.getUnitPrice()
                .multiply(BigDecimal.valueOf(existing.getQuantity())));
    }

    private Mono<Boolean> attachGuestCart(Cart guestCart, String customerId) {
        guestCart.setCustomerId(customerId);
        guestCart.setSessionToken(null);
        guestCart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(guestCart).thenReturn(Boolean.TRUE);
    }

    @Override
    public Mono<CheckoutResponse> checkout(String email, Integer storeId, String paymentMethod) {
        if (!SUPPORTED_PAYMENT_METHODS.contains(paymentMethod)) {
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
                .switchIfEmpty(Mono.defer(() -> prepareCheckout(customer, cart, storeId, paymentMethod)));
    }

    private Mono<CheckoutResponse> prepareCheckout(
            Customer customer, Cart cart, Integer storeId, String paymentMethod) {
        return refreshAndValidateProducts(cart)
                .then(validateStock(cart.getItems(), storeId))
                .then(reserveCartForCheckout(cart.getId(), cart.getVersion()))
                .then(Mono.defer(() -> createOrderAndPayment(customer, cart, storeId, paymentMethod)));
    }

    private Mono<CheckoutResponse> createOrderAndPayment(
            Customer customer, Cart cart, Integer storeId, String paymentMethod) {
        LocalDateTime now = LocalDateTime.now();
        Order order = buildPendingOrder(customer, cart, storeId, now);
        return transactionalOperator.transactional(
                        saveOrderDetailsAndPayment(order, cart, storeId, paymentMethod, now))
                .onErrorResume(error -> recoverFailedCheckout(cart, error))
                .flatMap(response -> cartRepository.deleteById(cart.getId()).thenReturn(response));
    }

    private Order buildPendingOrder(Customer customer, Cart cart, Integer storeId, LocalDateTime now) {
        return Order.builder()
                .orderDate(now)
                .status(PENDING_ORDER_STATUS)
                .deliveryType(PICKUP_DELIVERY_TYPE)
                .totalAmount(cart.getTotalAmount())
                .storeId(storeId)
                .customerId(String.valueOf(customer.getId()))
                .checkoutKey(cart.getId())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private Mono<CheckoutResponse> saveOrderDetailsAndPayment(
            Order order, Cart cart, Integer storeId, String paymentMethod, LocalDateTime now) {
        return orderRepository.save(order)
                .flatMap(savedOrder -> saveOrderDetails(savedOrder, cart, storeId, paymentMethod, now));
    }

    private Mono<CheckoutResponse> saveOrderDetails(
            Order order, Cart cart, Integer storeId, String paymentMethod, LocalDateTime now) {
        return Flux.fromIterable(cart.getItems())
                .concatMap(item -> orderDetailRepository.save(buildOrderDetail(order, item)))
                .collectList()
                .flatMap(details -> decreaseInventory(cart, storeId)
                        .then(createSimulatedPayment(order, paymentMethod, now))
                        .map(payment -> new CheckoutResponse(
                                OrderWithDetailsDTO.from(order, details), PaymentResponse.from(payment))));
    }

    private OrderDetail buildOrderDetail(Order order, CartItem item) {
        return OrderDetail.builder()
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .productId(item.getProductId())
                .orderId(order.getOrderId())
                .build();
    }

    private Mono<Void> decreaseInventory(Cart cart, Integer storeId) {
        return Flux.fromIterable(cart.getItems())
                .concatMap(item -> inventoryService.decreaseStock(
                        item.getProductId(), storeId, item.getQuantity()))
                .then();
    }

    private Mono<CheckoutResponse> recoverFailedCheckout(Cart cart, Throwable error) {
        return orderRepository.findByCheckoutKey(cart.getId())
                .flatMap(this::findCheckoutResponse)
                .switchIfEmpty(Mono.defer(() -> reopenCart(cart.getId()).then(Mono.error(error))));
    }

    private Mono<Payment> createSimulatedPayment(Order order, String paymentMethod, LocalDateTime now) {
        boolean cashAtAgent = CASH_PAYMENT_METHOD.equals(paymentMethod);
        LocalDateTime expiresAt = now.plus(cashAtAgent
                ? java.time.Duration.ofHours(CASH_PAYMENT_EXPIRATION_HOURS)
                : java.time.Duration.ofMinutes(ONLINE_PAYMENT_EXPIRATION_MINUTES));
        String cipCode = cashAtAgent
                ? String.format(java.util.Locale.ROOT, "%0" + CIP_CODE_DIGITS + "d",
                        SECURE_RANDOM.nextInt(CIP_CODE_BOUND))
                : null;
        String referenceCode = cashAtAgent ? cipCode
                : SIMULATED_REFERENCE_PREFIX + UUID.randomUUID().toString().substring(0, CIP_CODE_DIGITS);
        Payment payment = Payment.builder()
                .orderId(order.getOrderId())
                .paymentMethod(paymentMethod)
                .paymentStatus(PENDING_PAYMENT_STATUS)
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
        List<Long> productIds;
        try {
            productIds = parseProductIds(items);
        } catch (NumberFormatException exception) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El carrito contiene un productId inválido"));
        }
        return productRepository.findAllById(productIds.stream().distinct().toList())
                .collectMap(Product::getId)
                .flatMap(products -> updateCartFromProducts(cart, items, productIds, products));
    }

    private List<Long> parseProductIds(List<CartItem> items) {
        List<Long> productIds = new ArrayList<>();
        for (CartItem item : items) {
            productIds.add(Long.valueOf(item.getProductId()));
        }
        return productIds;
    }

    private Mono<Void> updateCartFromProducts(
            Cart cart, List<CartItem> items, List<Long> productIds, Map<Long, Product> products) {
        long distinctProductCount = productIds.stream().distinct().count();
        if (products.size() != distinctProductCount) {
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
    }

    private Mono<Cart> reserveCartForCheckout(String cartId, Long expectedVersion) {
        LocalDateTime leaseExpiredAt = LocalDateTime.now().minusMinutes(CHECKOUT_LEASE_MINUTES);
        Query query = Query.query(new Criteria().andOperator(
                Criteria.where("_id").is(cartId),
                Criteria.where("version").is(expectedVersion),
                new Criteria().orOperator(
                        Criteria.where("checkoutState").ne(PROCESSING_CHECKOUT_STATE),
                        Criteria.where("checkoutStartedAt").lt(leaseExpiredAt))));
        return mongoTemplate.findAndModify(
                        query,
                        new Update()
                                .set("checkoutState", PROCESSING_CHECKOUT_STATE)
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
                .and("checkoutState").is(PROCESSING_CHECKOUT_STATE));
        return mongoTemplate.updateFirst(query,
                new Update().set("checkoutState", OPEN_CART_STATE)
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
        if (PROCESSING_CHECKOUT_STATE.equals(cart.getCheckoutState())) {
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
        return sessionToken != null
                && sessionToken.length() >= MIN_SESSION_TOKEN_LENGTH
                && sessionToken.length() <= ValidationLimits.MAX_SESSION_TOKEN_LENGTH;
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
                .checkoutState(OPEN_CART_STATE)
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
