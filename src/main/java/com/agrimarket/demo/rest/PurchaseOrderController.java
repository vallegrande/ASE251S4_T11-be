package com.agrimarket.demo.rest;

import com.agrimarket.demo.dto.CreatePurchaseOrderRequest;
import com.agrimarket.demo.dto.PurchaseOrderWithDetailsDTO;
import com.agrimarket.demo.dto.UpdatePurchaseOrderRequest;
import com.agrimarket.demo.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    public Flux<PurchaseOrderWithDetailsDTO> findAllWithDetails() {
        return purchaseOrderService.findAllWithDetails();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<PurchaseOrderWithDetailsDTO>> findByIdWithDetails(@PathVariable Integer id) {
        return purchaseOrderService.findByIdWithDetails(id)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PostMapping
    public Mono<PurchaseOrderWithDetailsDTO> create(@Valid @RequestBody CreatePurchaseOrderRequest req) {
        return purchaseOrderService.create(req);
    }

    @PutMapping("/{id}")
    public Mono<PurchaseOrderWithDetailsDTO> update(@PathVariable Integer id, @Valid @RequestBody UpdatePurchaseOrderRequest req) {
        return purchaseOrderService.update(id, req);
    }
}
