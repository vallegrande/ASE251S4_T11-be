package pe.edu.vallegrande.vgmsagrimarket.dto;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class CheckoutResponse {
    OrderWithDetailsDTO order;
    PaymentResponse payment;
}
