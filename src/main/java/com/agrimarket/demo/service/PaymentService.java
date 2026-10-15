package com.agrimarket.demo.service;

import com.agrimarket.demo.dto.PaymentResponse;
import reactor.core.publisher.Mono;

public interface PaymentService {
    Mono<PaymentResponse> findForCustomer(Integer paymentId, String email);
    Mono<PaymentResponse> confirmSimulatedPayment(Integer paymentId, String email);
    Mono<Integer> expireDuePayments();
}
