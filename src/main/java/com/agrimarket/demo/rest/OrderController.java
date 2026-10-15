package com.agrimarket.demo.rest;

import com.agrimarket.demo.dto.CreateOrderRequest;
import com.agrimarket.demo.dto.OrderWithDetailsDTO;
import com.agrimarket.demo.dto.StatusUpdateRequest;
import com.agrimarket.demo.dto.UpdateOrderRequest;
import com.agrimarket.demo.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

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
