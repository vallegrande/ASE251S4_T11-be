package com.agrimarket.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CheckoutRequest {
    @NotNull
    @Positive
    private Integer storeId;

    @NotBlank
    @Pattern(regexp = "YAPE|TARJETA_CREDITO|PAGO_EFECTIVO",
            message = "paymentMethod debe ser YAPE, TARJETA_CREDITO o PAGO_EFECTIVO")
    private String paymentMethod;
}
