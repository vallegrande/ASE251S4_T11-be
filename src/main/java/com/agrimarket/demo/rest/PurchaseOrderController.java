package com.agrimarket.demo.rest;

import com.agrimarket.demo.dto.CreatePurchaseOrderRequest;
import com.agrimarket.demo.dto.PurchaseOrderWithDetailsDTO;
import com.agrimarket.demo.dto.StatusUpdateRequest;
import com.agrimarket.demo.dto.UpdatePurchaseOrderRequest;
import com.agrimarket.demo.service.PurchaseOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
@Validated
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

    // ADMIN/VENDEDOR: registra un lote completo; si una compra falla, se revierte todo el lote.
    @PostMapping("/batch")
    public Mono<List<PurchaseOrderWithDetailsDTO>> createBatch(
            @RequestBody
            @NotEmpty(message = "El lote debe incluir al menos una orden de compra")
            @Size(max = 100, message = "El lote no puede superar 100 órdenes de compra")
            List<@Valid CreatePurchaseOrderRequest> requests) {
        return purchaseOrderService.createBatch(requests);
    }

    @PutMapping("/{id}")
    public Mono<PurchaseOrderWithDetailsDTO> update(@PathVariable Integer id, @Valid @RequestBody UpdatePurchaseOrderRequest req) {
        return purchaseOrderService.update(id, req);
    }

    // ADMIN/VENDEDOR: recibe o cancela una compra; al recibirla, suma sus cantidades al inventario.
    @PatchMapping("/{id}/status")
    public Mono<PurchaseOrderWithDetailsDTO> updateStatus(
            @PathVariable Integer id,
            @Valid @RequestBody StatusUpdateRequest req) {
        return purchaseOrderService.updateStatus(id, req);
    }
}
