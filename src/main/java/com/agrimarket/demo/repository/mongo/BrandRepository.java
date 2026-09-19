package com.agrimarket.demo.repository.mongo;

import com.agrimarket.demo.model.mongo.Brand;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface BrandRepository extends ReactiveMongoRepository<Brand, Long> {

    Flux<Brand> findByIsActive(Boolean isActive);
}
