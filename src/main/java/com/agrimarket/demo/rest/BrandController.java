package com.agrimarket.demo.rest;

import com.agrimarket.demo.model.mongo.Brand;
import com.agrimarket.demo.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @GetMapping
    public Flux<Brand> findAll() {
        return brandService.findAll();
    }

    @GetMapping("/status/{isActive}")
    public Flux<Brand> findByStatus(@PathVariable Boolean isActive) {
        return brandService.findByStatus(isActive);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Brand>> findById(@PathVariable Long id) {
        return brandService.findById(id)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PostMapping
    public Mono<Brand> create(@Valid @RequestBody Brand brand) {
        return brandService.create(brand);
    }

    @PutMapping("/{id}")
    public Mono<Brand> update(@PathVariable Long id, @Valid @RequestBody Brand brand) {
        return brandService.update(id, brand);
    }

    @PatchMapping("/{id}/delete")
    public Mono<ResponseEntity<Brand>> logicalDelete(@PathVariable Long id) {
        return brandService.logicalDelete(id)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PatchMapping("/{id}/restore")
    public Mono<ResponseEntity<Brand>> restore(@PathVariable Long id) {
        return brandService.restore(id)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }
}
