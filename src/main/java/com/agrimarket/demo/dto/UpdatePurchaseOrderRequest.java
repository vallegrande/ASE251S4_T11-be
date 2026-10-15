package com.agrimarket.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record UpdatePurchaseOrderRequest(
        @NotNull(message = "purchaseOrderDate es obligatorio")
        LocalDateTime purchaseOrderDate,

        @NotBlank(message = "status es obligatorio")
        String status,

        BigDecimal totalAmount,

        @NotBlank(message = "supplierId es obligatorio")
        String supplierId,

        Integer storeId,

        String notes,

        @Valid
        @NotEmpty(message = "items no puede estar vacío")
        List<PurchaseOrderItemDTO> items
) {
}
