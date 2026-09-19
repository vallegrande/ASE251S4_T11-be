package com.agrimarket.demo.repository.mongo;

import com.agrimarket.demo.model.mongo.Customer;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface CustomerRepository extends ReactiveMongoRepository<Customer, Long> {

    Flux<Customer> findByIsActive(Boolean isActive);
}
