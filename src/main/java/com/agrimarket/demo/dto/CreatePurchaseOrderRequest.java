package com.agrimarket.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CreatePurchaseOrderRequest(
        LocalDateTime purchaseOrderDate,
        String status,
        BigDecimal totalAmount,
        String supplierId,
        Integer storeId,
        String notes,
        List<PurchaseOrderItemDTO> items
) {
}
