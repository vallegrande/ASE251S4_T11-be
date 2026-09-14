package com.agrimarket.demo.repository;

import com.agrimarket.demo.model.Supplier;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface SupplierRepository extends ReactiveMongoRepository<Supplier, Long> {

    Flux<Supplier> findByIsActive(Boolean isActive);
}
