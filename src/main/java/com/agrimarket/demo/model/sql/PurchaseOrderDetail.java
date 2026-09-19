package com.agrimarket.demo.model.sql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("purchase_order_detail")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderDetail {

    @Id
    private Integer purchaseOrderDetailId;

    private Integer quantity;
    private BigDecimal unitPrice;
    private String productId;
    private Integer purchaseOrderId;
}
