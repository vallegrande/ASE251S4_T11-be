package pe.edu.vallegrande.vgmsagrimarket.controller;

import pe.edu.vallegrande.vgmsagrimarket.dto.PaymentResponse;
import pe.edu.vallegrande.vgmsagrimarket.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // CLIENTE: consulta únicamente el pago asociado a uno de sus pedidos.
    @GetMapping("/{paymentId}")
    public Mono<PaymentResponse> findById(
            @PathVariable Integer paymentId,
            @AuthenticationPrincipal String email) {
        return paymentService.findForCustomer(paymentId, email);
    }

    // Solo simula la confirmación; no realiza un cobro real ni llama a una pasarela.
    @PostMapping("/{paymentId}/simulate-confirmation")
    public Mono<PaymentResponse> simulateConfirmation(
            @PathVariable Integer paymentId,
            @AuthenticationPrincipal String email) {
        return paymentService.confirmSimulatedPayment(paymentId, email);
    }
}
