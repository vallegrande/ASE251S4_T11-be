package com.agrimarket.demo.repository.mongo;

import com.agrimarket.demo.model.mongo.StaffUser;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface StaffUserRepository extends ReactiveMongoRepository<StaffUser, String> {

    Mono<StaffUser> findByEmailIgnoreCase(String email);

    Mono<Boolean> existsByEmailIgnoreCase(String email);
}
