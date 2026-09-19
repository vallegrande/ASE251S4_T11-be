package com.agrimarket.demo.rest;

import com.agrimarket.demo.model.mongo.Category;
import com.agrimarket.demo.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public Flux<Category> findAll() {
        return categoryService.findAll();
    }

    @GetMapping("/status/{isActive}")
    public Flux<Category> findByStatus(@PathVariable Boolean isActive) {
        return categoryService.findByStatus(isActive);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Category>> findById(@PathVariable Long id) {
        return categoryService.findById(id)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PostMapping
    public Mono<Category> create(@Valid @RequestBody Category category) {
        return categoryService.create(category);
    }

    @PutMapping("/{id}")
    public Mono<Category> update(@PathVariable Long id, @Valid @RequestBody Category category) {
        return categoryService.update(id, category);
    }

    @PatchMapping("/{id}/delete")
    public Mono<ResponseEntity<Category>> logicalDelete(@PathVariable Long id) {
        return categoryService.logicalDelete(id)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PatchMapping("/{id}/restore")
    public Mono<ResponseEntity<Category>> restore(@PathVariable Long id) {
        return categoryService.restore(id)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }
}
