package com.agrimarket.demo.service;

import com.agrimarket.demo.dto.StaffUserRequest;
import com.agrimarket.demo.model.mongo.StaffUser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StaffUserService {
    Flux<StaffUser> findAll();
    Mono<StaffUser> create(StaffUserRequest request);
    Mono<StaffUser> update(String id, StaffUserRequest request);
    Mono<StaffUser> deactivate(String id);
}
