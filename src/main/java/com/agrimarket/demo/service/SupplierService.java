package com.agrimarket.demo.service;

import com.agrimarket.demo.model.Supplier;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SupplierService {

    Flux<Supplier> findAll();

    Flux<Supplier> findByStatus(Boolean isActive);

    Mono<Supplier> findById(Long id);

    Mono<Supplier> create(Supplier supplier);

    Mono<Supplier> update(Long id, Supplier supplier);

    Mono<Supplier> logicalDelete(Long id);

    Mono<Supplier> restore(Long id);
}
