package com.agrimarket.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class CheckoutResponse {
    OrderWithDetailsDTO order;
    PaymentResponse payment;
}
