package pe.edu.vallegrande.agrimarket.service.impl;

import pe.edu.vallegrande.agrimarket.service.*;
import pe.edu.vallegrande.agrimarket.dto.CreateOrderRequest;
import pe.edu.vallegrande.agrimarket.dto.OrderItemDTO;
import pe.edu.vallegrande.agrimarket.dto.OrderWithDetailsDTO;
import pe.edu.vallegrande.agrimarket.dto.StatusUpdateRequest;
import pe.edu.vallegrande.agrimarket.dto.UpdateOrderRequest;
import pe.edu.vallegrande.agrimarket.exception.ResourceNotFoundException;
import pe.edu.vallegrande.agrimarket.model.sql.Order;
import pe.edu.vallegrande.agrimarket.model.sql.OrderDetail;
import pe.edu.vallegrande.agrimarket.model.sql.OrderStatus;
import pe.edu.vallegrande.agrimarket.repository.mongo.CustomerRepository;
import pe.edu.vallegrande.agrimarket.repository.sql.OrderDetailRepository;
import pe.edu.vallegrande.agrimarket.repository.sql.OrderRepository;
import pe.edu.vallegrande.agrimarket.repository.sql.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final String PAYMENT_STATUS_PAID = "PAID";
    private static final Set<OrderStatus> ALLOWED_TARGET_STATUSES = Set.of(
            OrderStatus.PREPARING, OrderStatus.READY_FOR_PICKUP, OrderStatus.COMPLETED);

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final OrderReferenceValidator orderReferenceValidator;
    private final TransactionalOperator transactionalOperator;

    @Override
    public Flux<OrderWithDetailsDTO> findAllWithDetails() {
        return orderRepository.findAll()
                .flatMap(order -> Mono.zip(
                        Mono.just(order),
                        orderDetailRepository.findByOrderId(order.getOrderId()).collectList()
                ).map(tuple -> OrderWithDetailsDTO.from(tuple.getT1(), tuple.getT2())));
    }

    @Override
    public Mono<OrderWithDetailsDTO> findByIdWithDetails(Integer id) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Order no encontrado con id: " + id)))
                .flatMap(this::findOrderWithDetails);
    }

    @Override
    public Flux<OrderWithDetailsDTO> findByCustomerEmail(String email) {
        return customerRepository.findByEmailIgnoreCase(email)
                .filter(customer -> Boolean.TRUE.equals(customer.getIsActive()))
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Customer no encontrado o inactivo")))
                .flatMapMany(customer -> orderRepository.findByCustomerId(String.valueOf(customer.getId()))
                        .concatMap(this::findOrderWithDetails));
    }

    @Override
    public Mono<OrderWithDetailsDTO> findByIdWithDetailsForCustomer(Integer id, String email) {
        return customerRepository.findByEmailIgnoreCase(email)
                .filter(customer -> Boolean.TRUE.equals(customer.getIsActive()))
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Customer no encontrado o inactivo")))
                .flatMap(customer -> orderRepository.findById(id)
                        .filter(order -> String.valueOf(customer.getId()).equals(order.getCustomerId()))
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                "Order no encontrado con id: " + id)))
                        .flatMap(this::findOrderWithDetails));
    }

    private Mono<OrderWithDetailsDTO> findOrderWithDetails(Order order) {
        return orderDetailRepository.findByOrderId(order.getOrderId())
                .collectList()
                .map(details -> OrderWithDetailsDTO.from(order, details));
    }

    @Override
    public Mono<OrderWithDetailsDTO> create(CreateOrderRequest req) {
        return createOrder(req).as(transactionalOperator::transactional);
    }

    @Override
    public Mono<List<OrderWithDetailsDTO>> createBatch(List<CreateOrderRequest> requests) {
        return Flux.fromIterable(requests)
                .concatMap(this::createOrder)
                .collectList()
                .as(transactionalOperator::transactional);
    }

    private Mono<OrderWithDetailsDTO> createOrder(CreateOrderRequest req) {
        return validateCreateOrderRequest(req)
                .then(Mono.defer(() -> {
                    LocalDateTime now = LocalDateTime.now();
                    Order order = Order.builder()
                            .orderDate(req.getOrderDate())
                            .status(req.getStatus())
                            .deliveryType(req.getDeliveryType())
                            .deliveryAddress(req.getDeliveryAddress())
                            .deliveryDate(req.getDeliveryDate())
                            .totalAmount(calculateTotal(req.getItems()))
                            .storeId(req.getStoreId())
                            .customerId(req.getCustomerId())
                            .createdAt(now)
                            .updatedAt(now)
                            .build();

                    Mono<OrderWithDetailsDTO> flow = orderRepository.save(order)
                            .flatMap(savedOrder -> Flux.fromIterable(req.getItems())
                                    .map(item -> mapToOrderDetail(item, savedOrder.getOrderId()))
                                    .flatMap(orderDetailRepository::save)
                                    .collectList()
                                    .map(details -> OrderWithDetailsDTO.from(savedOrder, details)));

                    return flow;
                }));
    }

    @Override
    public Mono<OrderWithDetailsDTO> update(Integer id, UpdateOrderRequest req) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Order no encontrado con id: " + id)))
                .flatMap(existingOrder -> validateUpdateOrderRequest(req)
                        .then(Mono.defer(() -> {
                            existingOrder.setOrderDate(req.getOrderDate());
                            existingOrder.setDeliveryType(req.getDeliveryType());
                            existingOrder.setDeliveryAddress(req.getDeliveryAddress());
                            existingOrder.setDeliveryDate(req.getDeliveryDate());
                            existingOrder.setTotalAmount(calculateTotal(req.getItems()));
                            existingOrder.setStoreId(req.getStoreId());
                            existingOrder.setCustomerId(req.getCustomerId());
                            existingOrder.setUpdatedAt(LocalDateTime.now());

                            Mono<OrderWithDetailsDTO> flow = Mono.just(existingOrder)
                                    .flatMap(orderRepository::save)
                                    .flatMap(savedOrder -> orderDetailRepository.deleteByOrderId(id)
                                            .then(Mono.just(savedOrder))
                                    )
                                    .flatMap(savedOrder -> Flux.fromIterable(req.getItems())
                                            .map(item -> mapToOrderDetail(item, savedOrder.getOrderId()))
                                            .flatMap(orderDetailRepository::save)
                                            .collectList()
                                            .map(details -> OrderWithDetailsDTO.from(savedOrder, details)));

                            return transactionalOperator.transactional(flow);
                        }))
                );
    }

    @Override
    public Mono<OrderWithDetailsDTO> updateStatus(Integer id, StatusUpdateRequest req) {
        return parseNextStatus(req.status())
                .flatMap(nextStatus -> orderRepository.findById(id)
                        .switchIfEmpty(Mono.error(
                                new ResourceNotFoundException("Order no encontrado con id: " + id)))
                        .flatMap(order -> applyStatusTransition(order, nextStatus)));
    }

    private Mono<OrderStatus> parseNextStatus(String status) {
        OrderStatus nextStatus;
        try {
            nextStatus = OrderStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException exception) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "status debe ser uno de: " + List.of(OrderStatus.values())));
        }
        if (!ALLOWED_TARGET_STATUSES.contains(nextStatus)) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo puedes cambiar el pedido a PREPARING, READY_FOR_PICKUP o COMPLETED"));
        }
        return Mono.just(nextStatus);
    }

    private Mono<OrderWithDetailsDTO> applyStatusTransition(Order order, OrderStatus nextStatus) {
        if (nextStatus.name().equals(order.getStatus())) {
            return findOrderWithDetails(order);
        }
        OrderStatus currentStatus;
        try {
            currentStatus = OrderStatus.valueOf(order.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException exception) {
            return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "El pedido tiene un estado desconocido"));
        }
        if (!isValidTransition(currentStatus, nextStatus)) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Transición inválida. El pedido debe estar pagado y avanzar: " +
                            "PAID -> PREPARING -> READY_FOR_PICKUP -> COMPLETED"));
        }
        return ensurePaymentConfirmed(order, nextStatus);
    }

    private boolean isValidTransition(OrderStatus currentStatus, OrderStatus nextStatus) {
        return switch (currentStatus) {
            case PAID -> nextStatus == OrderStatus.PREPARING;
            case PREPARING -> nextStatus == OrderStatus.READY_FOR_PICKUP;
            case READY_FOR_PICKUP -> nextStatus == OrderStatus.COMPLETED;
            default -> false;
        };
    }

    private Mono<OrderWithDetailsDTO> ensurePaymentConfirmed(Order order, OrderStatus nextStatus) {
        return paymentRepository.existsByOrderIdAndPaymentStatus(order.getOrderId(), PAYMENT_STATUS_PAID)
                .flatMap(isPaid -> {
                    if (!isPaid) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.CONFLICT, "No se puede avanzar un pedido sin pago confirmado"));
                    }
                    order.setStatus(nextStatus.name());
                    order.setUpdatedAt(LocalDateTime.now());
                    return orderRepository.save(order).flatMap(this::findOrderWithDetails);
                });
    }

    private Mono<Void> validateCreateOrderRequest(CreateOrderRequest req) {
        return validateOrderStatus(req.getStatus())
                .then(validateDeliveryType(req.getDeliveryType()))
                .then(orderReferenceValidator.validateStoreExists(req.getStoreId()))
                .then(validateCustomerExists(req.getCustomerId()))
                .then(orderReferenceValidator.validateProductsExist(
                        req.getItems() == null ? null
                                : req.getItems().stream().map(OrderItemDTO::getProductId).toList(),
                        "Debe registrar al menos un item en el pedido"));
    }

    private Mono<Void> validateOrderStatus(String status) {
        try {
            OrderStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException exception) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "status no es válido para una orden"));
        }
        return Mono.empty();
    }

    private Mono<Void> validateUpdateOrderRequest(UpdateOrderRequest req) {
        return validateDeliveryType(req.getDeliveryType())
                .then(orderReferenceValidator.validateStoreExists(req.getStoreId()))
                .then(validateCustomerExists(req.getCustomerId()))
                .then(orderReferenceValidator.validateProductsExist(
                        req.getItems() == null ? null
                                : req.getItems().stream().map(OrderItemDTO::getProductId).toList(),
                        "Debe registrar al menos un item en el pedido"));
    }

    private Mono<Void> validateDeliveryType(String deliveryType) {
        if (!"recogida".equals(deliveryType)) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "deliveryType solo puede ser 'recogida'"));
        }
        return Mono.empty();
    }

    private Mono<Void> validateCustomerExists(String customerId) {
        Long id;
        try {
            id = Long.valueOf(customerId);
        } catch (NumberFormatException ex) {
            return Mono.error(new ResourceNotFoundException("Customer con id inválido: " + customerId));
        }
        return customerRepository.existsById(id)
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Customer no existe con id: " + customerId)))
                .then();
    }

    private OrderDetail mapToOrderDetail(OrderItemDTO item, Integer orderId) {
        return OrderDetail.builder()
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .productId(item.getProductId())
                .orderId(orderId)
                .build();
    }

    private BigDecimal calculateTotal(List<OrderItemDTO> items) {
        return items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
