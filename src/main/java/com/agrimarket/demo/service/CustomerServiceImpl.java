package com.agrimarket.demo.service;

import com.agrimarket.demo.exception.ResourceNotFoundException;
import com.agrimarket.demo.model.mongo.Customer;
import com.agrimarket.demo.repository.mongo.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    @Override
    public Flux<Customer> findAll() {
        return customerRepository.findAll();
    }

    @Override
    public Flux<Customer> findByStatus(Boolean isActive) {
        return customerRepository.findByIsActive(isActive);
    }

    @Override
    public Mono<Customer> findById(Long id) {
        return customerRepository.findById(id);
    }

    @Override
    public Mono<Customer> create(Customer customer) {
        return sequenceGeneratorService.generateSequence("customers_sequence")
                .flatMap(seq -> {
                    customer.setId(seq);
                    LocalDateTime now = LocalDateTime.now();
                    customer.setCreatedAt(now);
                    customer.setUpdatedAt(now);
                    if (customer.getIsActive() == null) {
                        customer.setIsActive(true);
                    }
                    if (customer.getTotalVisits() == null) {
                        customer.setTotalVisits(0);
                    }
                    return customerRepository.save(customer);
                });
    }

    @Override
    public Mono<Customer> update(Long id, Customer customer) {
        return customerRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Customer no encontrado con id: " + id)))
                .flatMap(existing -> {
                    existing.setCustomerCode(customer.getCustomerCode());
                    existing.setDocType(customer.getDocType());
                    existing.setDocNumber(customer.getDocNumber());
                    existing.setFirstName(customer.getFirstName());
                    existing.setLastName(customer.getLastName());
                    existing.setEmail(customer.getEmail());
                    existing.setPhone(customer.getPhone());
                    existing.setAddress(customer.getAddress());
                    existing.setUbigeo(customer.getUbigeo());
                    existing.setPassword(customer.getPassword());
                    existing.setTotalVisits(customer.getTotalVisits());
                    existing.setLastPurchaseDate(customer.getLastPurchaseDate());
                    existing.setCustomerSince(customer.getCustomerSince());
                    existing.setNotes(customer.getNotes());
                    existing.setIsActive(customer.getIsActive());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return customerRepository.save(existing);
                });
    }

    @Override
    public Mono<Customer> logicalDelete(Long id) {
        return customerRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Customer no encontrado con id: " + id)))
                .flatMap(existing -> {
                    existing.setIsActive(false);
                    existing.setDeletedAt(LocalDateTime.now());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return customerRepository.save(existing);
                });
    }

    @Override
    public Mono<Customer> restore(Long id) {
        return customerRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Customer no encontrado con id: " + id)))
                .flatMap(existing -> {
                    existing.setIsActive(true);
                    existing.setRestoredAt(LocalDateTime.now());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return customerRepository.save(existing);
                });
    }
}
