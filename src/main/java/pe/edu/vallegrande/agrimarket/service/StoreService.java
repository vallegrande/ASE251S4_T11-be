package pe.edu.vallegrande.agrimarket.service;

import pe.edu.vallegrande.agrimarket.dto.StoreRequest;
import pe.edu.vallegrande.agrimarket.model.sql.Store;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StoreService {
    Flux<Store> findAll();
    Mono<Store> findById(Integer storeId);
    Flux<Store> findAllForManagement();
    Mono<Store> findByIdForManagement(Integer storeId);
    Mono<Store> create(StoreRequest request);
    Mono<Store> update(Integer storeId, StoreRequest request);
    Mono<Store> updateAvailability(Integer storeId, Boolean isActive);
    Mono<Store> deactivate(Integer storeId);
}
