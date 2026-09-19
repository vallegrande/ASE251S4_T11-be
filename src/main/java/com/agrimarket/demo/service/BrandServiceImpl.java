package com.agrimarket.demo.service;

import com.agrimarket.demo.exception.ResourceNotFoundException;
import com.agrimarket.demo.model.mongo.Brand;
import com.agrimarket.demo.repository.mongo.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    @Override
    public Flux<Brand> findAll() {
        return brandRepository.findAll();
    }

    @Override
    public Flux<Brand> findByStatus(Boolean isActive) {
        return brandRepository.findByIsActive(isActive);
    }

    @Override
    public Mono<Brand> findById(Long id) {
        return brandRepository.findById(id);
    }

    @Override
    public Mono<Brand> create(Brand brand) {
        return sequenceGeneratorService.generateSequence("brands_sequence")
                .flatMap(seq -> {
                    brand.setId(seq);
                    LocalDateTime now = LocalDateTime.now();
                    brand.setCreatedAt(now);
                    brand.setUpdatedAt(now);
                    if (brand.getIsActive() == null) {
                        brand.setIsActive(true);
                    }
                    return brandRepository.save(brand);
                });
    }

    @Override
    public Mono<Brand> update(Long id, Brand brand) {
        return brandRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Brand no encontrado con id: " + id)))
                .flatMap(existing -> {
                    existing.setBrandName(brand.getBrandName());
                    existing.setIsActive(brand.getIsActive());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return brandRepository.save(existing);
                });
    }

    @Override
    public Mono<Brand> logicalDelete(Long id) {
        return brandRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Brand no encontrado con id: " + id)))
                .flatMap(existing -> {
                    existing.setIsActive(false);
                    existing.setDeletedAt(LocalDateTime.now());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return brandRepository.save(existing);
                });
    }

    @Override
    public Mono<Brand> restore(Long id) {
        return brandRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Brand no encontrado con id: " + id)))
                .flatMap(existing -> {
                    existing.setIsActive(true);
                    existing.setRestoredAt(LocalDateTime.now());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return brandRepository.save(existing);
                });
    }
}
