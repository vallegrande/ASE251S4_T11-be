package com.agrimarket.demo.repository.sql;

import com.agrimarket.demo.model.sql.Order;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends ReactiveCrudRepository<Order, Integer> {
}
