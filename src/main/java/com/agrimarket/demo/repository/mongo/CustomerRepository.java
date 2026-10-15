package com.agrimarket.demo.repository.mongo;

import com.agrimarket.demo.model.mongo.Customer;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CustomerRepository extends ReactiveMongoRepository<Customer, Long> {

    Flux<Customer> findByIsActive(Boolean isActive);

    Mono<Customer> findByEmailIgnoreCase(String email);

    Mono<Boolean> existsByEmailIgnoreCase(String email);

    Mono<Boolean> existsByDocTypeIgnoreCaseAndDocNumber(String docType, String docNumber);

    Mono<Customer> findByDocTypeIgnoreCaseAndDocNumber(String docType, String docNumber);
}
