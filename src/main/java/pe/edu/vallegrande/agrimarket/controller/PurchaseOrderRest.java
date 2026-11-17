package pe.edu.vallegrande.agrimarket.controller;

import pe.edu.vallegrande.agrimarket.dto.CreatePurchaseOrderRequest;
import pe.edu.vallegrande.agrimarket.dto.PurchaseOrderWithDetailsDTO;
import pe.edu.vallegrande.agrimarket.dto.StatusUpdateRequest;
import pe.edu.vallegrande.agrimarket.dto.UpdatePurchaseOrderRequest;
import pe.edu.vallegrande.agrimarket.service.PurchaseOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import pe.edu.vallegrande.agrimarket.validation.ValidationLimits;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
@Validated
public class PurchaseOrderRest {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderRest(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

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
            @Size(max = ValidationLimits.MAX_BATCH_SIZE,
                    message = "El lote no puede superar " + ValidationLimits.MAX_BATCH_SIZE + " órdenes de compra")
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
