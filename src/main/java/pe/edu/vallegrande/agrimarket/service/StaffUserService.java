package pe.edu.vallegrande.agrimarket.service;

import pe.edu.vallegrande.agrimarket.dto.StaffUserRequest;
import pe.edu.vallegrande.agrimarket.model.mongo.StaffUser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StaffUserService {
    Flux<StaffUser> findAll();
    Mono<StaffUser> create(StaffUserRequest request);
    Mono<StaffUser> update(String id, StaffUserRequest request);
    Mono<StaffUser> deactivate(String id);
}
