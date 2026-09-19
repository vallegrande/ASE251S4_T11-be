package com.agrimarket.demo.model.sql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table("order")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    private Integer orderId;

    private LocalDateTime orderDate;
    private String status;
    private String deliveryType;
    private String deliveryAddress;
    private LocalDateTime deliveryDate;
    private BigDecimal totalAmount;
    private Integer storeId;
    private String customerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
