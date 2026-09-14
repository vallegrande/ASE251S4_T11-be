package com.agrimarket.demo.rest;

import com.agrimarket.demo.model.Customer;
import com.agrimarket.demo.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public Flux<Customer> findAll() {
        return customerService.findAll();
    }

    @GetMapping("/status/{isActive}")
    public Flux<Customer> findByStatus(@PathVariable Boolean isActive) {
        return customerService.findByStatus(isActive);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Customer>> findById(@PathVariable Long id) {
        return customerService.findById(id)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PostMapping
    public Mono<Customer> create(@Valid @RequestBody Customer customer) {
        return customerService.create(customer);
    }

    @PutMapping("/{id}")
    public Mono<Customer> update(@PathVariable Long id, @Valid @RequestBody Customer customer) {
        return customerService.update(id, customer);
    }

    @PatchMapping("/{id}/delete")
    public Mono<ResponseEntity<Customer>> logicalDelete(@PathVariable Long id) {
        return customerService.logicalDelete(id)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PatchMapping("/{id}/restore")
    public Mono<ResponseEntity<Customer>> restore(@PathVariable Long id) {
        return customerService.restore(id)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }
}
