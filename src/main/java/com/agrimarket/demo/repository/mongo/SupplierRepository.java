package com.agrimarket.demo.repository.mongo;

import com.agrimarket.demo.model.mongo.Supplier;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface SupplierRepository extends ReactiveMongoRepository<Supplier, Long> {

    Flux<Supplier> findByIsActive(Boolean isActive);
}
