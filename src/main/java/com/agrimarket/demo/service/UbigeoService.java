package com.agrimarket.demo.service;

import com.agrimarket.demo.model.mongo.Ubigeo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UbigeoService {

    Flux<Ubigeo> findAll();

    Mono<Ubigeo> findById(Long id);

    Flux<Ubigeo> findByDepartment(String department);
}
