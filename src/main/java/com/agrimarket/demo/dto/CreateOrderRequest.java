package com.agrimarket.demo.dto;

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

    private LocalDateTime orderDate;
    private String status;
    private String deliveryType;
    private String deliveryAddress;
    private LocalDateTime deliveryDate;
    private BigDecimal totalAmount;
    private Integer storeId;
    private String customerId;
    private List<OrderItemDTO> items;
}
