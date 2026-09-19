package com.agrimarket.demo.dto;

import java.math.BigDecimal;

public record PurchaseOrderItemDTO(
        Integer quantity,
        BigDecimal unitPrice,
        String productId
) {
}
