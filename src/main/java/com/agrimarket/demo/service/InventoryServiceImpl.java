package com.agrimarket.demo.service;

import com.agrimarket.demo.exception.ResourceNotFoundException;
import com.agrimarket.demo.model.sql.Inventory;
import com.agrimarket.demo.repository.mongo.ProductRepository;
import com.agrimarket.demo.repository.sql.InventoryRepository;
import com.agrimarket.demo.repository.sql.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

    @Override
    public Flux<Inventory> findAll() {
        return inventoryRepository.findAll();
    }

    @Override
    public Mono<Inventory> findById(Integer inventoryId) {
        return inventoryRepository.findById(inventoryId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        "Inventory no encontrado con id: " + inventoryId)));
    }

    @Override
    public Mono<Inventory> create(Inventory inventory) {
        return validateProductAndStore(inventory.getProductId(), inventory.getStoreId())
                .then(inventoryRepository.existsByProductIdAndStoreId(
                        inventory.getProductId(), inventory.getStoreId()))
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.CONFLICT, "Ya existe inventario para ese producto y tienda"));
                    }
                    inventory.setInventoryId(null);
                    inventory.setUpdatedAt(LocalDateTime.now());
                    return inventoryRepository.save(inventory);
                });
    }

    @Override
    public Mono<Inventory> update(Integer inventoryId, Inventory inventory) {
        return validateProductAndStore(inventory.getProductId(), inventory.getStoreId())
                .then(findById(inventoryId))
                .flatMap(existing -> inventoryRepository.findByProductIdAndStoreId(
                                inventory.getProductId(), inventory.getStoreId())
                        .filter(other -> !other.getInventoryId().equals(inventoryId))
                        .flatMap(other -> Mono.<Inventory>error(new ResponseStatusException(
                                HttpStatus.CONFLICT, "Ya existe inventario para ese producto y tienda")))
                        .switchIfEmpty(Mono.defer(() -> {
                            existing.setProductId(inventory.getProductId());
                            existing.setStoreId(inventory.getStoreId());
                            existing.setCurrentStock(inventory.getCurrentStock());
                            existing.setMinimumStock(inventory.getMinimumStock());
                            existing.setUpdatedAt(LocalDateTime.now());
                            return inventoryRepository.save(existing);
                        })));
    }

    @Override
    public Mono<Void> delete(Integer inventoryId) {
        return findById(inventoryId).flatMap(inventoryRepository::delete);
    }

    @Override
    public Mono<Boolean> hasStock(String productId, Integer storeId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "quantity debe ser mayor que cero"));
        }
        return inventoryRepository.findByProductIdAndStoreId(productId, storeId)
                .map(inventory -> inventory.getCurrentStock() != null && inventory.getCurrentStock() >= quantity)
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Void> decreaseStock(String productId, Integer storeId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "quantity debe ser mayor que cero"));
        }
        return inventoryRepository.decreaseStock(productId, storeId, quantity, LocalDateTime.now())
                .flatMap(updated -> updated > 0
                        ? Mono.empty()
                        : Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Stock insuficiente para el producto " + productId + " en la tienda " + storeId)));
    }

    @Override
    public Mono<Void> increaseStock(String productId, Integer storeId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "quantity debe ser mayor que cero"));
        }
        return inventoryRepository.increaseStock(productId, storeId, quantity, LocalDateTime.now())
                .flatMap(updated -> updated > 0
                        ? Mono.empty()
                        : Mono.error(new ResourceNotFoundException(
                                "Inventory no encontrado para producto " + productId + " y tienda " + storeId)));
    }

    private Mono<Void> validateProductAndStore(String productId, Integer storeId) {
        long productNumericId;
        try {
            productNumericId = Long.parseLong(productId);
        } catch (NumberFormatException exception) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "productId debe ser un ID numérico válido"));
        }
        return Mono.zip(productRepository.existsById(productNumericId),
                        storeRepository.existsByStoreIdAndIsActiveTrue(storeId))
                .flatMap(exists -> {
                    if (!exists.getT1()) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "El producto indicado no existe"));
                    }
                    if (!exists.getT2()) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "La tienda indicada no existe o no está disponible"));
                    }
                    return Mono.empty();
                });
    }
}
