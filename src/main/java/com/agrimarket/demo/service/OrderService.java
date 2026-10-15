package com.agrimarket.demo.service;

import com.agrimarket.demo.dto.CreateOrderRequest;
import com.agrimarket.demo.dto.OrderWithDetailsDTO;
import com.agrimarket.demo.dto.StatusUpdateRequest;
import com.agrimarket.demo.dto.UpdateOrderRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderService {

    Flux<OrderWithDetailsDTO> findAllWithDetails();

    Mono<OrderWithDetailsDTO> findByIdWithDetails(Integer id);

    Flux<OrderWithDetailsDTO> findByCustomerEmail(String email);

    Mono<OrderWithDetailsDTO> findByIdWithDetailsForCustomer(Integer id, String email);

    Mono<OrderWithDetailsDTO> create(CreateOrderRequest req);

    Mono<OrderWithDetailsDTO> update(Integer id, UpdateOrderRequest req);

    Mono<OrderWithDetailsDTO> updateStatus(Integer id, StatusUpdateRequest req);
}
