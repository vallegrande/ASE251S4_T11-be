package com.agrimarket.demo.service;

import com.agrimarket.demo.model.mongo.Brand;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BrandService {

    Flux<Brand> findAll();

    Flux<Brand> findByStatus(Boolean isActive);

    Mono<Brand> findById(Long id);

    Mono<Brand> create(Brand brand);

    Mono<Brand> update(Long id, Brand brand);

    Mono<Brand> logicalDelete(Long id);

    Mono<Brand> restore(Long id);
}
