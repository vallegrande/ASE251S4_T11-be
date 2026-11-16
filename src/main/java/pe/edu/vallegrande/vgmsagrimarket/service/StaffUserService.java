package pe.edu.vallegrande.vgmsagrimarket.service;

import pe.edu.vallegrande.vgmsagrimarket.dto.StaffUserRequest;
import pe.edu.vallegrande.vgmsagrimarket.model.mongo.StaffUser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StaffUserService {
    Flux<StaffUser> findAll();
    Mono<StaffUser> create(StaffUserRequest request);
    Mono<StaffUser> update(String id, StaffUserRequest request);
    Mono<StaffUser> deactivate(String id);
}
