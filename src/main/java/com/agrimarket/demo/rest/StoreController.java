package com.agrimarket.demo.rest;

import com.agrimarket.demo.dto.StoreAvailabilityRequest;
import com.agrimarket.demo.dto.StoreRequest;
import com.agrimarket.demo.model.sql.Store;
import com.agrimarket.demo.service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    // Público: muestra solo las tiendas disponibles para recojo.
    @GetMapping
    public Flux<Store> findAll() {
        return storeService.findAll();
    }

    // Público: una tienda inactiva no se muestra por ID.
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Store>> findById(@PathVariable Integer id) {
        return storeService.findById(id)
                .map(ResponseEntity::ok)
                .onErrorResume(com.agrimarket.demo.exception.ResourceNotFoundException.class,
                        ignored -> Mono.just(ResponseEntity.notFound().build()));
    }

    // ADMIN: incluye tiendas inactivas para revisarlas o reactivarlas.
    @GetMapping("/manage")
    public Flux<Store> findAllForManagement() {
        return storeService.findAllForManagement();
    }

    // ADMIN: consulta una tienda aunque esté inactiva.
    @GetMapping("/manage/{id}")
    public Mono<ResponseEntity<Store>> findByIdForManagement(@PathVariable Integer id) {
        return storeService.findByIdForManagement(id)
                .map(ResponseEntity::ok)
                .onErrorResume(com.agrimarket.demo.exception.ResourceNotFoundException.class,
                        ignored -> Mono.just(ResponseEntity.notFound().build()));
    }

    @PostMapping
    public Mono<ResponseEntity<Store>> create(@Valid @RequestBody StoreRequest request) {
        return storeService.create(request)
                .map(store -> ResponseEntity.status(HttpStatus.CREATED).body(store));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Store>> update(
            @PathVariable Integer id,
            @Valid @RequestBody StoreRequest request) {
        return storeService.update(id, request)
                .map(ResponseEntity::ok)
                .onErrorResume(com.agrimarket.demo.exception.ResourceNotFoundException.class,
                        ignored -> Mono.just(ResponseEntity.notFound().build()));
    }

    @PatchMapping("/{id}/availability")
    public Mono<ResponseEntity<Store>> updateAvailability(
            @PathVariable Integer id,
            @Valid @RequestBody StoreAvailabilityRequest request) {
        return storeService.updateAvailability(id, request.getIsActive())
                .map(ResponseEntity::ok)
                .onErrorResume(com.agrimarket.demo.exception.ResourceNotFoundException.class,
                        ignored -> Mono.just(ResponseEntity.notFound().build()));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Store>> deactivate(@PathVariable Integer id) {
        return storeService.deactivate(id)
                .map(ResponseEntity::ok)
                .onErrorResume(com.agrimarket.demo.exception.ResourceNotFoundException.class,
                        ignored -> Mono.just(ResponseEntity.notFound().build()));
    }
}
