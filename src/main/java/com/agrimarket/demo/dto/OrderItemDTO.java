package com.agrimarket.demo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {

    @NotNull(message = "quantity es obligatorio")
    @Positive(message = "quantity debe ser mayor a 0")
    private Integer quantity;

    @NotNull(message = "unitPrice es obligatorio")
    @DecimalMin(value = "0.00", inclusive = false, message = "unitPrice debe ser mayor a 0")
    private BigDecimal unitPrice;

    @NotBlank(message = "productId es obligatorio")
    private String productId;
}
