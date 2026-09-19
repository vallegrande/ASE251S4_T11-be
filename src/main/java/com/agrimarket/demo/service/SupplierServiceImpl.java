package com.agrimarket.demo.service;

import com.agrimarket.demo.exception.ResourceNotFoundException;
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
    public Flux<Supplier> findByStatus(Boolean isActive) {
        return supplierRepository.findByIsActive(isActive);
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

    @Override
    public Mono<Supplier> update(Long id, Supplier supplier) {
        return supplierRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Supplier no encontrado con id: " + id)))
                .flatMap(existing -> {
                    existing.setSupplierCode(supplier.getSupplierCode());
                    existing.setBusinessName(supplier.getBusinessName());
                    existing.setDocType(supplier.getDocType());
                    existing.setDocNumber(supplier.getDocNumber());
                    existing.setPhone(supplier.getPhone());
                    existing.setEmail(supplier.getEmail());
                    existing.setAddress(supplier.getAddress());
                    existing.setUbigeo(supplier.getUbigeo());
                    existing.setContactPerson(supplier.getContactPerson());
                    existing.setIsActive(supplier.getIsActive() != null ? supplier.getIsActive() : existing.getIsActive());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return supplierRepository.save(existing);
                });
    }

    @Override
    public Mono<Supplier> logicalDelete(Long id) {
        return supplierRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Supplier no encontrado con id: " + id)))
                .flatMap(existing -> {
                    existing.setIsActive(false);
                    existing.setDeletedAt(LocalDateTime.now());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return supplierRepository.save(existing);
                });
    }

    @Override
    public Mono<Supplier> restore(Long id) {
        return supplierRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Supplier no encontrado con id: " + id)))
                .flatMap(existing -> {
                    existing.setIsActive(true);
                    existing.setRestoredAt(LocalDateTime.now());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return supplierRepository.save(existing);
                });
    }
}
