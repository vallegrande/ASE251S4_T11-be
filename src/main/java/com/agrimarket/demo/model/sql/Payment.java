package com.agrimarket.demo.model.sql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table("payment")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @Column("payment_id")
    private Integer paymentId;

    @Column("order_id")
    private Integer orderId;

    @Column("payment_method")
    private String paymentMethod;

    @Column("payment_status")
    private String paymentStatus;

    private BigDecimal amount;

    @Column("reference_code")
    private String referenceCode;

    @Column("cip_code")
    private String cipCode;

    @Column("expires_at")
    private LocalDateTime expiresAt;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("paid_at")
    private LocalDateTime paidAt;
}
