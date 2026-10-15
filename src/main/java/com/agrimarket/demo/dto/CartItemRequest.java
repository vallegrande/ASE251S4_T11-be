package com.agrimarket.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CartItemRequest {
    @NotBlank
    private String productId;

    @NotNull
    @Positive
    private Integer quantity;
}
