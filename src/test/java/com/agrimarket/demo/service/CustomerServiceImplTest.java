package com.agrimarket.demo.service;

import com.agrimarket.demo.model.mongo.Customer;
import com.agrimarket.demo.repository.mongo.CustomerRepository;
import com.agrimarket.demo.repository.mongo.StaffUserRepository;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;

class CustomerServiceImplTest {

    private final CustomerServiceImpl customerService = new CustomerServiceImpl(
            mock(CustomerRepository.class),
            mock(StaffUserRepository.class),
            mock(SequenceGeneratorService.class),
            mock(PasswordEncoder.class),
            mock(Validator.class));

    @Test
    void rejectsUnsupportedDocumentType() {
        Customer customer = customer("PASAPORTE", "123456789");

        StepVerifier.create(customerService.create(customer))
                .expectErrorMatches(error -> error instanceof ResponseStatusException exception
                        && exception.getStatusCode() == HttpStatus.BAD_REQUEST)
                .verify();
    }

    @Test
    void rejectsDocumentNumberWithWrongLengthForType() {
        Customer customer = customer("DNI", "123456789");

        StepVerifier.create(customerService.create(customer))
                .expectErrorMatches(error -> error instanceof ResponseStatusException exception
                        && exception.getStatusCode() == HttpStatus.BAD_REQUEST)
                .verify();
    }

    private Customer customer(String docType, String docNumber) {
        return Customer.builder()
                .docType(docType)
                .docNumber(docNumber)
                .build();
    }
}
