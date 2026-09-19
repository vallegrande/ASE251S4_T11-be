package com.agrimarket.demo.repository.sql;

import com.agrimarket.demo.model.sql.PurchaseOrder;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseOrderRepository extends ReactiveCrudRepository<PurchaseOrder, Integer> {
}
