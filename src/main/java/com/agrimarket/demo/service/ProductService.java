package com.agrimarket.demo.service;

import com.agrimarket.demo.model.mongo.Product;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductService {

    Flux<Product> findAll();

    Flux<Product> findByStatus(Boolean isActive);

    Mono<Product> findById(Long id);

    Mono<Product> create(Product product);

    Mono<Product> update(Long id, Product product);

    Mono<Product> logicalDelete(Long id);

    Mono<Product> restore(Long id);
}
