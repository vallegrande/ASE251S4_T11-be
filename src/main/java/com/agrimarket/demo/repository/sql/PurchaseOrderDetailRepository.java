package com.agrimarket.demo.repository.sql;

import com.agrimarket.demo.model.sql.PurchaseOrderDetail;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface PurchaseOrderDetailRepository extends ReactiveCrudRepository<PurchaseOrderDetail, Integer> {

    Flux<PurchaseOrderDetail> findByPurchaseOrderId(Integer purchaseOrderId);

    Mono<Void> deleteByPurchaseOrderId(Integer purchaseOrderId);
}
