package com.agrimarket.demo.repository.sql;

import com.agrimarket.demo.model.sql.Payment;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface PaymentRepository extends ReactiveCrudRepository<Payment, Integer> {

    Mono<Payment> findByOrderId(Integer orderId);

    Mono<Boolean> existsByOrderIdAndPaymentStatus(Integer orderId, String paymentStatus);

    Flux<Payment> findByPaymentStatusAndExpiresAtLessThanEqual(String paymentStatus, LocalDateTime now);

    @Modifying
    @Query("UPDATE payment SET payment_status = 'PAID', paid_at = :now, updated_at = :now " +
            "WHERE payment_id = :paymentId AND payment_status = 'PENDING' " +
            "AND (expires_at IS NULL OR expires_at > :now)")
    Mono<Integer> markPaidIfPending(
            @Param("paymentId") Integer paymentId,
            @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE payment SET payment_status = 'EXPIRED', updated_at = :now " +
            "WHERE payment_id = :paymentId AND payment_status = 'PENDING' AND expires_at <= :now")
    Mono<Integer> markExpiredIfDue(
            @Param("paymentId") Integer paymentId,
            @Param("now") LocalDateTime now);
}
