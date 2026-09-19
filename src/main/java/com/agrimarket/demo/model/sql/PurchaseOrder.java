package com.agrimarket.demo.model.sql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table("purchase_order")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrder {

    @Id
    private Integer purchaseOrderId;

    private LocalDateTime purchaseOrderDate;
    private String status;
    private BigDecimal totalAmount;
    private String supplierId;
    private Integer storeId;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
