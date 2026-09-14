package com.agrimarket.demo.repository;

import com.agrimarket.demo.model.Customer;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface CustomerRepository extends ReactiveMongoRepository<Customer, Long> {

    Flux<Customer> findByIsActive(Boolean isActive);
}
