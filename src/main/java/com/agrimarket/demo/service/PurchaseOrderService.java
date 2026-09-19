package com.agrimarket.demo.service;

import com.agrimarket.demo.dto.CreatePurchaseOrderRequest;
import com.agrimarket.demo.dto.PurchaseOrderWithDetailsDTO;
import com.agrimarket.demo.dto.UpdatePurchaseOrderRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PurchaseOrderService {

    Flux<PurchaseOrderWithDetailsDTO> findAllWithDetails();

    Mono<PurchaseOrderWithDetailsDTO> findByIdWithDetails(Integer id);

    Mono<PurchaseOrderWithDetailsDTO> create(CreatePurchaseOrderRequest req);

    Mono<PurchaseOrderWithDetailsDTO> update(Integer id, UpdatePurchaseOrderRequest req);
}
