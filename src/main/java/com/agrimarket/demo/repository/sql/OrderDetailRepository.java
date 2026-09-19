package com.agrimarket.demo.repository.sql;

import com.agrimarket.demo.model.sql.OrderDetail;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface OrderDetailRepository extends ReactiveCrudRepository<OrderDetail, Integer> {

    Flux<OrderDetail> findByOrderId(Integer orderId);

    Mono<Void> deleteByOrderId(Integer orderId);
}
