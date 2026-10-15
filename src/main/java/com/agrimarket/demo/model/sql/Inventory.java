package com.agrimarket.demo.model.sql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

@Table("inventory")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    @Id
    @Column("inventory_id")
    private Integer inventoryId;

    @Column("product_id")
    @NotBlank
    private String productId;

    @Column("store_id")
    @NotNull
    @Positive
    private Integer storeId;

    @Column("current_stock")
    @NotNull
    @PositiveOrZero
    private Integer currentStock;

    @Column("minimum_stock")
    @NotNull
    @PositiveOrZero
    private Integer minimumStock;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
