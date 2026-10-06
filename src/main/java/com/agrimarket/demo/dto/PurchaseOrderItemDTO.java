package com.agrimarket.demo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PurchaseOrderItemDTO(
        @NotNull(message = "quantity es obligatorio")
        @Positive(message = "quantity debe ser mayor a 0")
        Integer quantity,

        @NotNull(message = "unitPrice es obligatorio")
        @DecimalMin(value = "0.00", inclusive = false, message = "unitPrice debe ser mayor a 0")
        BigDecimal unitPrice,

        @NotBlank(message = "productId es obligatorio")
        String productId
) {
}
