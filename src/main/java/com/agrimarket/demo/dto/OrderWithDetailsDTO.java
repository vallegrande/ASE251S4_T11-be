package com.agrimarket.demo.dto;

import com.agrimarket.demo.model.sql.Order;
import com.agrimarket.demo.model.sql.OrderDetail;
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
public class OrderWithDetailsDTO {

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
    private List<OrderDetail> items;

    public static OrderWithDetailsDTO from(Order order, List<OrderDetail> items) {
        return OrderWithDetailsDTO.builder()
                .orderId(order.getOrderId())
                .orderDate(order.getOrderDate())
                .status(order.getStatus())
                .deliveryType(order.getDeliveryType())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryDate(order.getDeliveryDate())
                .totalAmount(order.getTotalAmount())
                .storeId(order.getStoreId())
                .customerId(order.getCustomerId())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(items)
                .build();
    }
}
