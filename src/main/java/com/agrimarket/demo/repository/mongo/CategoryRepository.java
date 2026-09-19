package com.agrimarket.demo.repository.mongo;

import com.agrimarket.demo.model.mongo.Category;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface CategoryRepository extends ReactiveMongoRepository<Category, Long> {

    Flux<Category> findByIsActive(Boolean isActive);
}
