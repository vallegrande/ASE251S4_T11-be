package com.agrimarket.demo.rest;

import com.agrimarket.demo.model.mongo.Supplier;
import com.agrimarket.demo.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    public Flux<Supplier> findAll() {
        return supplierService.findAll();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Supplier>> findById(@PathVariable Long id) {
        return supplierService.findById(id)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PostMapping
    public Mono<Supplier> create(@Valid @RequestBody Supplier supplier) {
        return supplierService.create(supplier);
    }
}
