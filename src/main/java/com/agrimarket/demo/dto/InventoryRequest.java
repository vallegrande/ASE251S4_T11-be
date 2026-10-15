package com.agrimarket.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class InventoryRequest {

    @NotBlank
    private String productId;

    @NotNull
    @Positive
    private Integer storeId;

    @NotNull
    @PositiveOrZero
    private Integer currentStock;

    @NotNull
    @PositiveOrZero
    private Integer minimumStock;
}
