package pe.edu.vallegrande.agrimarket.controller;

import pe.edu.vallegrande.agrimarket.dto.CreateOrderRequest;
import pe.edu.vallegrande.agrimarket.dto.OrderWithDetailsDTO;
import pe.edu.vallegrande.agrimarket.dto.StatusUpdateRequest;
import pe.edu.vallegrande.agrimarket.dto.UpdateOrderRequest;
import pe.edu.vallegrande.agrimarket.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import pe.edu.vallegrande.agrimarket.validation.ValidationLimits;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderRest {

    private final OrderService orderService;

    public OrderRest(OrderService orderService) {
        this.orderService = orderService;
    }

    // ADMIN/VENDEDOR: lista todos los pedidos para gestionarlos desde la tienda.
    @GetMapping("/manage")
    public Flux<OrderWithDetailsDTO> findAllForManagement() {
        return orderService.findAllWithDetails();
    }

    // ADMIN/VENDEDOR: consulta cualquier pedido, aunque pertenezca a otro cliente.
    @GetMapping("/manage/{id}")
    public Mono<ResponseEntity<OrderWithDetailsDTO>> findByIdForManagement(@PathVariable Integer id) {
        return orderService.findByIdWithDetails(id)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    // CLIENTE: solo lista los pedidos del cliente autenticado.
    @GetMapping
    public Flux<OrderWithDetailsDTO> findAll(@AuthenticationPrincipal String email) {
        return orderService.findByCustomerEmail(email);
    }

    // CLIENTE: solo consulta el pedido si pertenece al cliente autenticado.
    @GetMapping("/{id}")
    public Mono<ResponseEntity<OrderWithDetailsDTO>> findById(
            @PathVariable Integer id,
            @AuthenticationPrincipal String email) {
        return orderService.findByIdWithDetailsForCustomer(id, email)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PostMapping
    public Mono<OrderWithDetailsDTO> create(@Valid @RequestBody CreateOrderRequest req) {
        return orderService.create(req);
    }

    // ADMIN/VENDEDOR: registra un lote completo; si una orden falla, se revierte todo el lote.
    @PostMapping("/batch")
    public Mono<List<OrderWithDetailsDTO>> createBatch(
            @RequestBody
            @NotEmpty(message = "El lote debe incluir al menos una orden")
            @Size(max = ValidationLimits.MAX_BATCH_SIZE,
                    message = "El lote no puede superar " + ValidationLimits.MAX_BATCH_SIZE + " órdenes")
            List<@Valid CreateOrderRequest> requests) {
        return orderService.createBatch(requests);
    }

    @PutMapping("/{id}")
    public Mono<OrderWithDetailsDTO> update(@PathVariable Integer id, @Valid @RequestBody UpdateOrderRequest req) {
        return orderService.update(id, req);
    }

    // ADMIN/VENDEDOR: avanza el estado respetando el flujo permitido del pedido.
    @PatchMapping("/{id}/status")
    public Mono<OrderWithDetailsDTO> updateStatus(
            @PathVariable Integer id,
            @Valid @RequestBody StatusUpdateRequest req) {
        return orderService.updateStatus(id, req);
    }
}
