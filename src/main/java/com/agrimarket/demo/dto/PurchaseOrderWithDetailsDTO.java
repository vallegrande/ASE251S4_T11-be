package com.agrimarket.demo.dto;

import com.agrimarket.demo.model.sql.PurchaseOrderDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderWithDetailsDTO {

    private Integer purchaseOrderId;
    private LocalDateTime purchaseOrderDate;
    private String status;
    private BigDecimal totalAmount;
    private String supplierId;
    private Integer storeId;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PurchaseOrderDetail> items;
}
