package com.agrimarket.demo.repository.sql;

import com.agrimarket.demo.model.sql.Store;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface StoreRepository extends ReactiveCrudRepository<Store, Integer> {

    Mono<Boolean> existsById(Integer storeId);
}
