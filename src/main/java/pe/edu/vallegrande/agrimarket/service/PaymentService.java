package pe.edu.vallegrande.agrimarket.service;

import pe.edu.vallegrande.agrimarket.dto.PaymentResponse;
import reactor.core.publisher.Mono;

public interface PaymentService {
    Mono<PaymentResponse> findForCustomer(Integer paymentId, String email);
    Mono<PaymentResponse> confirmSimulatedPayment(Integer paymentId, String email);
    Mono<Integer> expireDuePayments();
}
