package com.agrimarket.demo.repository.mongo;

import com.agrimarket.demo.model.mongo.Product;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ProductRepository extends ReactiveMongoRepository<Product, Long> {

    Flux<Product> findByIsActive(Boolean isActive);
}
