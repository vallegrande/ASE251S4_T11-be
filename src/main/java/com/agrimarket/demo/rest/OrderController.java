package com.agrimarket.demo.rest;

import com.agrimarket.demo.dto.CreateOrderRequest;
import com.agrimarket.demo.dto.OrderWithDetailsDTO;
import com.agrimarket.demo.dto.UpdateOrderRequest;
import com.agrimarket.demo.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public Flux<OrderWithDetailsDTO> findAll() {
        return orderService.findAllWithDetails();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<OrderWithDetailsDTO>> findById(@PathVariable Integer id) {
        return orderService.findByIdWithDetails(id)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PostMapping
    public Mono<OrderWithDetailsDTO> create(@RequestBody CreateOrderRequest req) {
        return orderService.create(req);
    }

    @PutMapping("/{id}")
    public Mono<OrderWithDetailsDTO> update(@PathVariable Integer id, @RequestBody UpdateOrderRequest req) {
        return orderService.update(id, req);
    }
}
