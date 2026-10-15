package com.agrimarket.demo.config;

import com.agrimarket.demo.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class PaymentExpiryJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentExpiryJob.class);

    private final PaymentService paymentService;

    @Scheduled(fixedDelayString = "${agrimarket.payment.expiry-check-ms:60000}")
    public void expirePendingPayments() {
        paymentService.expireDuePayments()
                .subscribe(
                        expiredCount -> {
                            if (expiredCount > 0) {
                                LOGGER.info("Expired {} simulated payments and released their stock", expiredCount);
                            }
                        },
                        error -> LOGGER.error("Failed to expire pending simulated payments", error));
    }
}
