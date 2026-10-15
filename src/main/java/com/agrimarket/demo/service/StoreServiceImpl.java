package com.agrimarket.demo.service;

import com.agrimarket.demo.dto.StoreRequest;
import com.agrimarket.demo.exception.ResourceNotFoundException;
import com.agrimarket.demo.model.sql.Store;
import com.agrimarket.demo.repository.mongo.UbigeoRepository;
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
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;
    private final UbigeoRepository ubigeoRepository;

    @Override
    public Flux<Store> findAll() {
        return storeRepository.findByIsActiveTrue();
    }

    @Override
    public Mono<Store> findById(Integer storeId) {
        return storeRepository.findByStoreIdAndIsActiveTrue(storeId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        "Store no encontrado o no disponible con id: " + storeId)));
    }

    @Override
    public Flux<Store> findAllForManagement() {
        return storeRepository.findAll();
    }

    @Override
    public Mono<Store> findByIdForManagement(Integer storeId) {
        return findExistingStore(storeId);
    }

    @Override
    public Mono<Store> create(StoreRequest request) {
        return validateUbigeo(request.getUbigeoId())
                .then(Mono.defer(() -> {
                    LocalDateTime now = LocalDateTime.now();
                    Store store = Store.builder()
                            .storeName(request.getStoreName().trim())
                            .address(normalize(request.getAddress()))
                            .phone(normalize(request.getPhone()))
                            .ubigeoId(request.getUbigeoId())
                            .isActive(true)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    return storeRepository.save(store);
                }));
    }

    @Override
    public Mono<Store> update(Integer storeId, StoreRequest request) {
        return validateUbigeo(request.getUbigeoId())
                .then(findExistingStore(storeId))
                .flatMap(store -> {
                    store.setStoreName(request.getStoreName().trim());
                    store.setAddress(normalize(request.getAddress()));
                    store.setPhone(normalize(request.getPhone()));
                    store.setUbigeoId(request.getUbigeoId());
                    store.setUpdatedAt(LocalDateTime.now());
                    return storeRepository.save(store);
                });
    }

    @Override
    public Mono<Store> updateAvailability(Integer storeId, Boolean isActive) {
        if (isActive == null) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "isActive es obligatorio"));
        }
        return findExistingStore(storeId)
                .flatMap(store -> {
                    store.setIsActive(isActive);
                    store.setUpdatedAt(LocalDateTime.now());
                    return storeRepository.save(store);
                });
    }

    @Override
    public Mono<Store> deactivate(Integer storeId) {
        return updateAvailability(storeId, false);
    }

    private Mono<Store> findExistingStore(Integer storeId) {
        return storeRepository.findById(storeId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        "Store no encontrado con id: " + storeId)));
    }

    private Mono<Void> validateUbigeo(Integer ubigeoId) {
        if (ubigeoId == null) {
            return Mono.empty();
        }
        return ubigeoRepository.findById(ubigeoId.longValue())
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "El ubigeo indicado no existe")))
                .then();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
