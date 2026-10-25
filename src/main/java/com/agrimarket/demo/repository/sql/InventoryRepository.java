package com.agrimarket.demo.repository.sql;

import com.agrimarket.demo.model.sql.Inventory;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface InventoryRepository extends ReactiveCrudRepository<Inventory, Integer> {

    Mono<Inventory> findByProductIdAndStoreId(String productId, Integer storeId);

    Mono<Boolean> existsByProductIdAndStoreId(String productId, Integer storeId);

    Flux<Inventory> findAllByProductId(String productId);

    Flux<Inventory> findAllByStoreId(Integer storeId);

    Flux<Inventory> findAllByProductIdAndStoreId(String productId, Integer storeId);

    @Modifying
    @Query("UPDATE inventory SET current_stock = current_stock - :quantity, updated_at = :updatedAt " +
            "WHERE product_id = :productId AND store_id = :storeId AND current_stock >= :quantity")
    Mono<Integer> decreaseStock(
            @Param("productId") String productId,
            @Param("storeId") Integer storeId,
            @Param("quantity") Integer quantity,
            @Param("updatedAt") java.time.LocalDateTime updatedAt);

    @Modifying
    @Query("UPDATE inventory SET current_stock = current_stock + :quantity, updated_at = :updatedAt " +
            "WHERE product_id = :productId AND store_id = :storeId")
    Mono<Integer> increaseStock(
            @Param("productId") String productId,
            @Param("storeId") Integer storeId,
            @Param("quantity") Integer quantity,
            @Param("updatedAt") java.time.LocalDateTime updatedAt);
}
