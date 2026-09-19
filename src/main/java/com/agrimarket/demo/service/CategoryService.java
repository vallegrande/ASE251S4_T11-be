package com.agrimarket.demo.service;

import com.agrimarket.demo.model.mongo.Category;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CategoryService {

    Flux<Category> findAll();

    Flux<Category> findByStatus(Boolean isActive);

    Mono<Category> findById(Long id);

    Mono<Category> create(Category category);

    Mono<Category> update(Long id, Category category);

    Mono<Category> logicalDelete(Long id);

    Mono<Category> restore(Long id);
}
