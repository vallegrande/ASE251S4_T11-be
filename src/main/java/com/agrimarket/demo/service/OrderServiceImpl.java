package com.agrimarket.demo.service;

import com.agrimarket.demo.dto.CreateOrderRequest;
import com.agrimarket.demo.dto.OrderItemDTO;
import com.agrimarket.demo.dto.OrderWithDetailsDTO;
import com.agrimarket.demo.dto.UpdateOrderRequest;
import com.agrimarket.demo.exception.ResourceNotFoundException;
import com.agrimarket.demo.model.sql.Order;
import com.agrimarket.demo.model.sql.OrderDetail;
import com.agrimarket.demo.repository.mongo.CustomerRepository;
import com.agrimarket.demo.repository.mongo.ProductRepository;
import com.agrimarket.demo.repository.sql.OrderDetailRepository;
import com.agrimarket.demo.repository.sql.OrderRepository;
import com.agrimarket.demo.repository.sql.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
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
                .flatMap(order -> Mono.zip(
                        Mono.just(order),
                        orderDetailRepository.findByOrderId(order.getOrderId()).collectList()
                ).map(tuple -> OrderWithDetailsDTO.from(tuple.getT1(), tuple.getT2())));
    }

    @Override
    public Mono<OrderWithDetailsDTO> create(CreateOrderRequest req) {
        return validateCreateOrderRequest(req)
                .then(Mono.defer(() -> {
                    LocalDateTime now = LocalDateTime.now();
                    Order order = Order.builder()
                            .orderDate(req.getOrderDate())
                            .status(req.getStatus())
                            .deliveryType(req.getDeliveryType())
                            .deliveryAddress(req.getDeliveryAddress())
                            .deliveryDate(req.getDeliveryDate())
                            .totalAmount(req.getTotalAmount())
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

                    return transactionalOperator.transactional(flow);
                }));
    }

    @Override
    public Mono<OrderWithDetailsDTO> update(Integer id, UpdateOrderRequest req) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Order no encontrado con id: " + id)))
                .flatMap(existingOrder -> validateUpdateOrderRequest(req)
                        .then(Mono.defer(() -> {
                            existingOrder.setOrderDate(req.getOrderDate());
                            existingOrder.setStatus(req.getStatus());
                            existingOrder.setDeliveryType(req.getDeliveryType());
                            existingOrder.setDeliveryAddress(req.getDeliveryAddress());
                            existingOrder.setDeliveryDate(req.getDeliveryDate());
                            existingOrder.setTotalAmount(req.getTotalAmount());
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

    private Mono<Void> validateCreateOrderRequest(CreateOrderRequest req) {
        return validateStoreExists(req.getStoreId())
                .then(validateCustomerExists(req.getCustomerId()))
                .then(validateProductsExist(req.getItems()));
    }

    private Mono<Void> validateUpdateOrderRequest(UpdateOrderRequest req) {
        return validateStoreExists(req.getStoreId())
                .then(validateCustomerExists(req.getCustomerId()))
                .then(validateProductsExist(req.getItems()));
    }

    private Mono<Void> validateStoreExists(Integer storeId) {
        if (storeId == null) {
            return Mono.empty();
        }
        return storeRepository.existsById(storeId)
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Store no existe con id: " + storeId)))
                .then();
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

    private Mono<Void> validateProductsExist(List<OrderItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return Mono.error(new ResourceNotFoundException("Debe registrar al menos un item en el pedido"));
        }
        List<Long> ids;
        try {
            ids = items.stream()
                    .map(i -> Long.valueOf(i.getProductId()))
                    .distinct()
                    .collect(Collectors.toList());
        } catch (NumberFormatException ex) {
            return Mono.error(new ResourceNotFoundException("Existe un productId con formato inválido en los items"));
        }
        return productRepository.findAllById(ids).collectList()
                .flatMap(found -> {
                    int expected = ids.size();
                    int actual = found.size();
                    if (actual != expected) {
                        return Mono.error(new ResourceNotFoundException(
                                "Algunos productId no existen en el catálogo (encontrados: " + actual + "/" + expected + ")"));
                    }
                    return Mono.empty();
                });
    }

    private OrderDetail mapToOrderDetail(OrderItemDTO item, Integer orderId) {
        return OrderDetail.builder()
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .productId(item.getProductId())
                .orderId(orderId)
                .build();
    }
}
