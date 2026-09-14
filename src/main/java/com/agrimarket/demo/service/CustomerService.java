package com.agrimarket.demo.service;

import com.agrimarket.demo.model.Customer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CustomerService {

    Flux<Customer> findAll();

    Flux<Customer> findByStatus(Boolean isActive);

    Mono<Customer> findById(Long id);

    Mono<Customer> create(Customer customer);

    Mono<Customer> update(Long id, Customer customer);

    Mono<Customer> logicalDelete(Long id);

    Mono<Customer> restore(Long id);
}
