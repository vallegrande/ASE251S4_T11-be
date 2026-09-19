package com.agrimarket.demo.service;

import com.agrimarket.demo.exception.ResourceNotFoundException;
import com.agrimarket.demo.model.mongo.Product;
import com.agrimarket.demo.repository.mongo.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    @Override
    public Flux<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    public Flux<Product> findByStatus(Boolean isActive) {
        return productRepository.findByIsActive(isActive);
    }

    @Override
    public Mono<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    @Override
    public Mono<Product> create(Product product) {
        return sequenceGeneratorService.generateSequence("products_sequence")
                .flatMap(seq -> {
                    product.setId(seq);
                    LocalDateTime now = LocalDateTime.now();
                    product.setCreatedAt(now);
                    product.setUpdatedAt(now);
                    if (product.getIsActive() == null) {
                        product.setIsActive(true);
                    }
                    return productRepository.save(product);
                });
    }

    @Override
    public Mono<Product> update(Long id, Product product) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Product no encontrado con id: " + id)))
                .flatMap(existing -> {
                    existing.setName(product.getName());
                    existing.setActiveIngredient(product.getActiveIngredient());
                    existing.setPresentation(product.getPresentation());
                    existing.setBasePrice(product.getBasePrice());
                    existing.setImageUrl(product.getImageUrl());
                    existing.setDescription(product.getDescription());
                    existing.setSku(product.getSku());
                    existing.setCategoryId(product.getCategoryId());
                    existing.setBrandId(product.getBrandId());
                    existing.setMetadata(product.getMetadata());
                    existing.setIsActive(product.getIsActive());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return productRepository.save(existing);
                });
    }

    @Override
    public Mono<Product> logicalDelete(Long id) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Product no encontrado con id: " + id)))
                .flatMap(existing -> {
                    existing.setIsActive(false);
                    existing.setDeletedAt(LocalDateTime.now());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return productRepository.save(existing);
                });
    }

    @Override
    public Mono<Product> restore(Long id) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Product no encontrado con id: " + id)))
                .flatMap(existing -> {
                    existing.setIsActive(true);
                    existing.setRestoredAt(LocalDateTime.now());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return productRepository.save(existing);
                });
    }
}
