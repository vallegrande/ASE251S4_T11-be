package com.agrimarket.demo.rest;

import com.agrimarket.demo.dto.InventoryRequest;
import com.agrimarket.demo.model.sql.Inventory;
import com.agrimarket.demo.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // ADMIN/VENDEDOR: consulta el stock registrado por producto y tienda.
    @GetMapping
    public Flux<Inventory> findAll() {
        return inventoryService.findAll();
    }

    @GetMapping("/{id}")
    public Mono<Inventory> findById(@PathVariable Integer id) {
        return inventoryService.findById(id);
    }

    // ADMIN/VENDEDOR: registra un stock inicial para un producto en una tienda activa.
    @PostMapping
    public Mono<Inventory> create(@Valid @RequestBody InventoryRequest request) {
        return inventoryService.create(toInventory(request));
    }

    // ADMIN/VENDEDOR: modifica los datos y cantidades del registro de inventario.
    @PutMapping("/{id}")
    public Mono<Inventory> update(@PathVariable Integer id, @Valid @RequestBody InventoryRequest request) {
        return inventoryService.update(id, toInventory(request));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Integer id) {
        return inventoryService.delete(id).thenReturn(ResponseEntity.noContent().build());
    }

    private Inventory toInventory(InventoryRequest request) {
        return Inventory.builder()
                .productId(request.getProductId())
                .storeId(request.getStoreId())
                .currentStock(request.getCurrentStock())
                .minimumStock(request.getMinimumStock())
                .build();
    }
}
