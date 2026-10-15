package com.agrimarket.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class CreateOrderRequest {

    @NotNull(message = "orderDate es obligatorio")
    private LocalDateTime orderDate;

    @NotBlank(message = "status es obligatorio")
    private String status;

    @NotBlank(message = "deliveryType es obligatorio")
    private String deliveryType;

    private String deliveryAddress;

    private LocalDateTime deliveryDate;

    private BigDecimal totalAmount;

    private Integer storeId;

    @NotBlank(message = "customerId es obligatorio")
    private String customerId;

    @Valid
    @NotEmpty(message = "items no puede estar vacío")
    private List<OrderItemDTO> items;
}
