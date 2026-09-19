package com.agrimarket.demo.service;

import com.agrimarket.demo.dto.CreateOrderRequest;
import com.agrimarket.demo.dto.OrderItemDTO;
import com.agrimarket.demo.dto.OrderWithDetailsDTO;
import com.agrimarket.demo.dto.UpdateOrderRequest;
import com.agrimarket.demo.exception.ResourceNotFoundException;
import com.agrimarket.demo.model.sql.Order;
import com.agrimarket.demo.model.sql.OrderDetail;
import com.agrimarket.demo.repository.mongo.CustomerRepository;
import com.agrimarket.demo.repository.sql.OrderDetailRepository;
import com.agrimarket.demo.repository.sql.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CustomerRepository customerRepository;
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
        return customerRepository.findById(Long.valueOf(req.getCustomerId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Customer no existe con id: " + req.getCustomerId())))
                .flatMap(customer -> {
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
                });
    }

    @Override
    public Mono<OrderWithDetailsDTO> update(Integer id, UpdateOrderRequest req) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Order no encontrado con id: " + id)))
                .flatMap(existingOrder -> {
                    Mono<OrderWithDetailsDTO> flow = Mono.just(existingOrder)
                            .map(order -> {
                                order.setOrderDate(req.getOrderDate());
                                order.setStatus(req.getStatus());
                                order.setDeliveryType(req.getDeliveryType());
                                order.setDeliveryAddress(req.getDeliveryAddress());
                                order.setDeliveryDate(req.getDeliveryDate());
                                order.setTotalAmount(req.getTotalAmount());
                                order.setStoreId(req.getStoreId());
                                order.setCustomerId(req.getCustomerId());
                                order.setUpdatedAt(LocalDateTime.now());
                                return order;
                            })
                            .flatMap(orderRepository::save)
                            .flatMap(savedOrder -> {
                                Mono<Void> delete = orderDetailRepository.deleteByOrderId(id);
                                Mono<java.util.List<OrderDetail>> saveItems = delete.thenMany(
                                        Flux.fromIterable(req.getItems())
                                                .map(item -> mapToOrderDetail(item, savedOrder.getOrderId()))
                                                .flatMap(orderDetailRepository::save)
                                ).collectList();
                                return saveItems.map(details -> OrderWithDetailsDTO.from(savedOrder, details));
                            });

                    return transactionalOperator.transactional(flow);
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
