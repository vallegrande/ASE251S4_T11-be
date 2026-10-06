package com.agrimarket.demo.service;

import com.agrimarket.demo.model.mongo.Supplier;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SupplierService {

    Flux<Supplier> findAll();

    Mono<Supplier> findById(Long id);

    Mono<Supplier> create(Supplier supplier);
}
