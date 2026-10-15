package com.agrimarket.demo.service;

import com.agrimarket.demo.exception.ResourceNotFoundException;
import com.agrimarket.demo.model.mongo.Customer;
import com.agrimarket.demo.repository.mongo.CustomerRepository;
import com.agrimarket.demo.repository.mongo.StaffUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final StaffUserRepository staffUserRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final PasswordEncoder passwordEncoder;

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
        if (!validPassword(customer.getPassword())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "password debe tener al menos 12 caracteres y no superar 72 bytes en UTF-8"));
        }
        customer.setEmail(customer.getEmail().trim().toLowerCase());
        customer.setDocType(customer.getDocType().trim().toUpperCase());
        customer.setDocNumber(customer.getDocNumber().trim());
        return Mono.zip(emailIsInUse(customer.getEmail(), null),
                        customerRepository.existsByDocTypeIgnoreCaseAndDocNumber(
                                customer.getDocType().trim(), customer.getDocNumber().trim()))
                .flatMap(inUse -> {
                    if (inUse.getT1() || inUse.getT2()) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.CONFLICT, "Ya existe una cuenta con ese email o documento"));
                    }
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
                                customer.setPassword(passwordEncoder.encode(customer.getPassword()));
                                return customerRepository.save(customer);
                            });
                });
    }

    @Override
    public Mono<Customer> update(Long id, Customer customer) {
        return customerRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Customer no encontrado con id: " + id)))
                .flatMap(existing -> {
                    String email = customer.getEmail().trim().toLowerCase();
                    String docType = customer.getDocType().trim();
                    String docNumber = customer.getDocNumber().trim();
                    Mono<Boolean> emailUsed = email.equalsIgnoreCase(existing.getEmail())
                            ? Mono.just(false)
                            : emailIsInUse(email, id);
                    Mono<Boolean> documentUsed = customerRepository
                            .findByDocTypeIgnoreCaseAndDocNumber(docType, docNumber)
                            .filter(found -> !Objects.equals(found.getId(), id))
                            .hasElement();
                    return Mono.zip(emailUsed, documentUsed)
                            .flatMap(used -> used.getT1() || used.getT2()
                                    ? Mono.error(new ResponseStatusException(
                                            HttpStatus.CONFLICT, "Ya existe una cuenta con ese email o documento"))
                                    : updateExistingCustomer(existing, customer, email));
                });
    }

    private Mono<Customer> updateExistingCustomer(Customer existing, Customer customer, String email) {
        existing.setDocType(customer.getDocType().trim().toUpperCase());
        existing.setDocNumber(customer.getDocNumber().trim());
        existing.setFirstName(customer.getFirstName());
        existing.setLastName(customer.getLastName());
        existing.setEmail(email);
        existing.setPhone(customer.getPhone());
        existing.setAddress(customer.getAddress());
        existing.setUbigeo(customer.getUbigeo());
        if (customer.getPassword() != null && !customer.getPassword().isBlank()) {
            if (!validPassword(customer.getPassword())) {
                return Mono.error(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "password debe tener al menos 12 caracteres y no superar 72 bytes en UTF-8"));
            }
            existing.setPassword(passwordEncoder.encode(customer.getPassword()));
        }
        existing.setTotalVisits(customer.getTotalVisits());
        existing.setLastPurchaseDate(customer.getLastPurchaseDate());
        existing.setCustomerSince(customer.getCustomerSince());
        existing.setNotes(customer.getNotes());
        existing.setIsActive(customer.getIsActive());
        existing.setUpdatedAt(LocalDateTime.now());
        return customerRepository.save(existing);
    }

    private Mono<Boolean> emailIsInUse(String email, Long excludingCustomerId) {
        return Mono.zip(
                        customerRepository.findByEmailIgnoreCase(email)
                                .filter(customer -> !Objects.equals(customer.getId(), excludingCustomerId))
                                .hasElement(),
                        staffUserRepository.existsByEmailIgnoreCase(email))
                .map(result -> result.getT1() || result.getT2());
    }

    private boolean validPassword(String password) {
        return password != null
                && password.length() >= 12
                && password.getBytes(StandardCharsets.UTF_8).length <= 72;
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
