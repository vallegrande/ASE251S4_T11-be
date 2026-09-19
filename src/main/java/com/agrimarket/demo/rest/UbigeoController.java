package com.agrimarket.demo.rest;

import com.agrimarket.demo.model.mongo.Ubigeo;
import com.agrimarket.demo.service.UbigeoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/ubigeos")
@RequiredArgsConstructor
public class UbigeoController {

    private final UbigeoService ubigeoService;

    @GetMapping
    public Flux<Ubigeo> findAll() {
        return ubigeoService.findAll();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Ubigeo>> findById(@PathVariable Long id) {
        return ubigeoService.findById(id)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @GetMapping("/department/{department}")
    public Flux<Ubigeo> findByDepartment(@PathVariable String department) {
        return ubigeoService.findByDepartment(department);
    }
}
