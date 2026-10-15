package com.agrimarket.demo.rest;

import com.agrimarket.demo.dto.StaffUserRequest;
import com.agrimarket.demo.model.mongo.StaffUser;
import com.agrimarket.demo.service.StaffUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/staff-users")
@RequiredArgsConstructor
public class StaffUserController {

    private final StaffUserService staffUserService;

    // ADMIN: lista las cuentas internas de administradores y vendedores.
    @GetMapping
    public Flux<StaffUser> findAll() {
        return staffUserService.findAll();
    }

    // El primer ADMIN puede crearse sin token solo cuando todavía no hay usuarios internos.
    @PostMapping
    public Mono<StaffUser> create(@Valid @RequestBody StaffUserRequest request) {
        return staffUserService.create(request);
    }

    @PutMapping("/{id}")
    public Mono<StaffUser> update(@PathVariable String id, @Valid @RequestBody StaffUserRequest request) {
        return staffUserService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public Mono<StaffUser> deactivate(@PathVariable String id) {
        return staffUserService.deactivate(id);
    }
}
