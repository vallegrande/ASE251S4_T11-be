package com.agrimarket.demo.service;

import com.agrimarket.demo.model.sql.Inventory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface InventoryService {
    Flux<Inventory> findAll();
    Mono<Inventory> findById(Integer inventoryId);
    Mono<Inventory> create(Inventory inventory);
    Mono<Inventory> update(Integer inventoryId, Inventory inventory);
    Mono<Void> delete(Integer inventoryId);
    Mono<Boolean> hasStock(String productId, Integer storeId, Integer quantity);
    Mono<Void> decreaseStock(String productId, Integer storeId, Integer quantity);
    Mono<Void> increaseStock(String productId, Integer storeId, Integer quantity);
}
