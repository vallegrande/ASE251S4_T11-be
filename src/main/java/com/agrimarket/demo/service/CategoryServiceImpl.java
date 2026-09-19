package com.agrimarket.demo.service;

import com.agrimarket.demo.exception.ResourceNotFoundException;
import com.agrimarket.demo.model.mongo.Category;
import com.agrimarket.demo.repository.mongo.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    @Override
    public Flux<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Override
    public Flux<Category> findByStatus(Boolean isActive) {
        return categoryRepository.findByIsActive(isActive);
    }

    @Override
    public Mono<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }

    @Override
    public Mono<Category> create(Category category) {
        return sequenceGeneratorService.generateSequence("categories_sequence")
                .flatMap(seq -> {
                    category.setId(seq);
                    LocalDateTime now = LocalDateTime.now();
                    category.setCreatedAt(now);
                    category.setUpdatedAt(now);
                    if (category.getIsActive() == null) {
                        category.setIsActive(true);
                    }
                    return categoryRepository.save(category);
                });
    }

    @Override
    public Mono<Category> update(Long id, Category category) {
        return categoryRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Category no encontrado con id: " + id)))
                .flatMap(existing -> {
                    existing.setCategoryName(category.getCategoryName());
                    existing.setParentCategoryId(category.getParentCategoryId());
                    existing.setDescription(category.getDescription());
                    existing.setLevel(category.getLevel());
                    existing.setCode(category.getCode());
                    existing.setOrderDisplay(category.getOrderDisplay());
                    existing.setIsActive(category.getIsActive());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return categoryRepository.save(existing);
                });
    }

    @Override
    public Mono<Category> logicalDelete(Long id) {
        return categoryRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Category no encontrado con id: " + id)))
                .flatMap(existing -> {
                    existing.setIsActive(false);
                    existing.setDeletedAt(LocalDateTime.now());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return categoryRepository.save(existing);
                });
    }

    @Override
    public Mono<Category> restore(Long id) {
        return categoryRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Category no encontrado con id: " + id)))
                .flatMap(existing -> {
                    existing.setIsActive(true);
                    existing.setRestoredAt(LocalDateTime.now());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return categoryRepository.save(existing);
                });
    }
}
