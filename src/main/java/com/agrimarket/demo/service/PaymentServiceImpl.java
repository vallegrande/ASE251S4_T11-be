package com.agrimarket.demo.service;

import com.agrimarket.demo.dto.PaymentResponse;
import com.agrimarket.demo.model.mongo.Customer;
import com.agrimarket.demo.model.sql.Order;
import com.agrimarket.demo.model.sql.Payment;
import com.agrimarket.demo.repository.mongo.CustomerRepository;
import com.agrimarket.demo.repository.sql.OrderDetailRepository;
import com.agrimarket.demo.repository.sql.OrderRepository;
import com.agrimarket.demo.repository.sql.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CustomerRepository customerRepository;
    private final InventoryService inventoryService;
    private final TransactionalOperator transactionalOperator;

    @Override
    public Mono<PaymentResponse> findForCustomer(Integer paymentId, String email) {
        return findOwnedPayment(paymentId, email).map(PaymentResponse::from);
    }

    @Override
    public Mono<PaymentResponse> confirmSimulatedPayment(Integer paymentId, String email) {
        return findOwnedPayment(paymentId, email)
                .flatMap(payment -> {
                    if ("PAID".equals(payment.getPaymentStatus())) {
                        return Mono.just(PaymentResponse.from(payment));
                    }
                    if (!"PENDING".equals(payment.getPaymentStatus())) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.CONFLICT, "El pago ya no está pendiente"));
                    }
                    LocalDateTime now = LocalDateTime.now();
                    if (payment.getExpiresAt() != null && !payment.getExpiresAt().isAfter(now)) {
                        return expirePayment(payment.getPaymentId())
                                .then(Mono.error(new ResponseStatusException(
                                        HttpStatus.GONE, "El plazo del pago venció; el pedido fue cancelado")));
                    }
                    return transactionalOperator.transactional(
                                    paymentRepository.markPaidIfPending(paymentId, now)
                                            .flatMap(updated -> updated == 1
                                                    ? markOrderPaid(payment.getOrderId())
                                                            .then(paymentRepository.findById(paymentId))
                                                    : paymentRepository.findById(paymentId))
                                            .switchIfEmpty(Mono.error(new ResponseStatusException(
                                                    HttpStatus.NOT_FOUND, "Pago no encontrado"))))
                            .flatMap(updatedPayment -> "PAID".equals(updatedPayment.getPaymentStatus())
                                    ? Mono.just(PaymentResponse.from(updatedPayment))
                                    : Mono.error(new ResponseStatusException(
                                            HttpStatus.CONFLICT, "El pago venció o ya no está pendiente")));
                });
    }

    @Override
    public Mono<Integer> expireDuePayments() {
        LocalDateTime now = LocalDateTime.now();
        return paymentRepository.findByPaymentStatusAndExpiresAtLessThanEqual("PENDING", now)
                .concatMap(payment -> expirePayment(payment.getPaymentId()))
                .reduce(0, Integer::sum);
    }

    private Mono<Payment> findOwnedPayment(Integer paymentId, String email) {
        return customerRepository.findByEmailIgnoreCase(email)
                .filter(customer -> Boolean.TRUE.equals(customer.getIsActive()))
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Customer no encontrado o inactivo")))
                .flatMap(customer -> paymentRepository.findById(paymentId)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Pago no encontrado")))
                        .flatMap(payment -> orderRepository.findById(payment.getOrderId())
                                .filter(order -> belongsToCustomer(order, customer))
                                .switchIfEmpty(Mono.error(new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Pago no encontrado")))
                                .thenReturn(payment)));
    }

    private boolean belongsToCustomer(Order order, Customer customer) {
        return order.getCustomerId().equals(String.valueOf(customer.getId()));
    }

    private Mono<Void> markOrderPaid(Integer orderId) {
        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Pedido relacionado al pago no encontrado")))
                .flatMap(order -> {
                    order.setStatus("PAID");
                    order.setUpdatedAt(LocalDateTime.now());
                    return orderRepository.save(order).then();
                });
    }

    private Mono<Integer> expirePayment(Integer paymentId) {
        LocalDateTime now = LocalDateTime.now();
        return transactionalOperator.transactional(
                paymentRepository.markExpiredIfDue(paymentId, now)
                        .flatMap(updated -> {
                            if (updated == 0) {
                                return Mono.just(0);
                            }
                            return paymentRepository.findById(paymentId)
                                    .flatMap(payment -> orderRepository.findById(payment.getOrderId())
                                            .switchIfEmpty(Mono.error(new ResponseStatusException(
                                                    HttpStatus.NOT_FOUND,
                                                    "Pedido relacionado al pago no encontrado")))
                                            .flatMap(order -> orderDetailRepository.findByOrderId(order.getOrderId())
                                                    .concatMap(detail -> inventoryService.increaseStock(
                                                            detail.getProductId(),
                                                            order.getStoreId(),
                                                            detail.getQuantity()))
                                                    .then(Mono.defer(() -> {
                                                        order.setStatus("CANCELLED");
                                                        order.setUpdatedAt(now);
                                                        return orderRepository.save(order).thenReturn(1);
                                                    }))));
                        }));
    }
}
