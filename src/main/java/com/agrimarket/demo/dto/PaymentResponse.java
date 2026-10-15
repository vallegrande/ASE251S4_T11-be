package com.agrimarket.demo.dto;

import com.agrimarket.demo.model.sql.Payment;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class PaymentResponse {
    Integer paymentId;
    Integer orderId;
    String paymentMethod;
    String paymentStatus;
    BigDecimal amount;
    String referenceCode;
    String cipCode;
    LocalDateTime expiresAt;
    boolean simulated;

    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .amount(payment.getAmount())
                .referenceCode(payment.getReferenceCode())
                .cipCode(payment.getCipCode())
                .expiresAt(payment.getExpiresAt())
                .simulated(true)
                .build();
    }
}
