package com.agrimarket.demo.service;

import com.agrimarket.demo.model.mongo.Supplier;
import com.agrimarket.demo.repository.mongo.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    @Override
    public Flux<Supplier> findAll() {
        return supplierRepository.findAll();
    }

    @Override
    public Mono<Supplier> findById(Long id) {
        return supplierRepository.findById(id);
    }

    @Override
    public Mono<Supplier> create(Supplier supplier) {
        return sequenceGeneratorService.generateSequence(Supplier.SEQUENCE_NAME)
                .map(id -> {
                    LocalDateTime now = LocalDateTime.now();
                    supplier.setId(id);
                    supplier.setCreatedAt(now);
                    supplier.setUpdatedAt(now);
                    if (supplier.getIsActive() == null) {
                        supplier.setIsActive(true);
                    }
                    return supplier;
                })
                .flatMap(supplierRepository::save);
    }
}
