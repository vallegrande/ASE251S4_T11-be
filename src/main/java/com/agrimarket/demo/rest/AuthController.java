package com.agrimarket.demo.rest;

import com.agrimarket.demo.dto.AuthResponse;
import com.agrimarket.demo.dto.CustomerRegistrationRequest;
import com.agrimarket.demo.dto.CustomerRegistrationResponse;
import com.agrimarket.demo.dto.LoginRequest;
import com.agrimarket.demo.model.mongo.Customer;
import com.agrimarket.demo.model.mongo.StaffUser;
import com.agrimarket.demo.repository.mongo.StaffUserRepository;
import com.agrimarket.demo.repository.mongo.CustomerRepository;
import com.agrimarket.demo.repository.mongo.UbigeoRepository;
import com.agrimarket.demo.security.BearerTokenService;
import com.agrimarket.demo.security.IssuedToken;
import com.agrimarket.demo.service.CartService;
import com.agrimarket.demo.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final StaffUserRepository staffUserRepository;
    private final CustomerRepository customerRepository;
    private final UbigeoRepository ubigeoRepository;
    private final CustomerService customerService;
    private final PasswordEncoder passwordEncoder;
    private final BearerTokenService bearerTokenService;
    private final CartService cartService;

    // Busca primero una cuenta interna y luego un cliente; entrega un token de sesión.
    @PostMapping("/login")
    public Mono<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "password no puede superar 72 bytes en UTF-8"));
        }
        String email = request.getEmail().trim().toLowerCase();
        return staffUserRepository.findByEmailIgnoreCase(email)
                .flatMap(user -> authenticateStaffUser(user, request.getPassword()))
                .switchIfEmpty(Mono.defer(() -> customerRepository.findByEmailIgnoreCase(email)
                        .flatMap(customer -> authenticateCustomer(customer, request.getPassword()))))
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos")))
                .flatMap(response -> {
                    if (!"CLIENTE".equals(response.getRole()) || request.getSessionToken() == null
                            || request.getSessionToken().isBlank()) {
                        return Mono.just(response);
                    }
                    return cartService.mergeGuestCart(request.getSessionToken(), response.getUserId())
                            .thenReturn(response);
                });
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.noContent().build());
        }
        return bearerTokenService.revoke(authorization.substring(7))
                .thenReturn(ResponseEntity.noContent().build());
    }

    // Registro público exclusivo para clientes; los usuarios internos se gestionan aparte.
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CustomerRegistrationResponse> register(
            @Valid @RequestBody CustomerRegistrationRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        return Mono.zip(
                        staffUserRepository.existsByEmailIgnoreCase(email),
                        customerRepository.existsByEmailIgnoreCase(email),
                        customerRepository.existsByDocTypeIgnoreCaseAndDocNumber(
                                request.getDocType().trim().toUpperCase(), request.getDocNumber().trim()))
                .flatMap(exists -> {
                    if (exists.getT1() || exists.getT2() || exists.getT3()) {
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT,
                                "Ya existe una cuenta con ese email o documento"));
                    }
                    return ubigeoRepository.findById(request.getUbigeoId())
                            .switchIfEmpty(Mono.error(new ResponseStatusException(
                                    HttpStatus.BAD_REQUEST, "El ubigeo indicado no existe")))
                            .flatMap(ubigeo -> {
                                Customer customer = Customer.builder()
                                        .docType(request.getDocType().trim().toUpperCase())
                                        .docNumber(request.getDocNumber().trim())
                                        .firstName(request.getFirstName().trim())
                                        .lastName(request.getLastName().trim())
                                        .email(email)
                                        .phone(request.getPhone().trim())
                                        .address(request.getAddress())
                                        .ubigeo(ubigeo)
                                        .password(request.getPassword())
                                        .totalVisits(0)
                                        .customerSince(LocalDate.now())
                                        .isActive(true)
                                        .createdAt(LocalDateTime.now())
                                        .updatedAt(LocalDateTime.now())
                                        .build();
                                return customerService.create(customer)
                                        .map(saved -> new CustomerRegistrationResponse(
                                                saved.getId(), saved.getEmail(),
                                                saved.getFirstName(), saved.getLastName()));
                            });
                });
    }

    private Mono<AuthResponse> authenticateStaffUser(StaffUser user, String rawPassword) {
        if (user.getPassword() == null) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos"));
        }
        if (!Boolean.TRUE.equals(user.getIsActive()) || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos"));
        }
        return bearerTokenService.issue(user.getId(), user.getEmail(), user.getRole())
                .map(this::createResponse);
    }

    private Mono<AuthResponse> authenticateCustomer(Customer customer, String rawPassword) {
        if (!Boolean.TRUE.equals(customer.getIsActive()) || !passwordMatches(rawPassword, customer.getPassword())) {
            return Mono.empty();
        }
        Mono<Customer> customerMono = Mono.just(customer);
        if (!isBcryptHash(customer.getPassword())) {
            customer.setPassword(passwordEncoder.encode(rawPassword));
            customer.setUpdatedAt(LocalDateTime.now());
            customerMono = customerRepository.save(customer);
        }
        return customerMono.flatMap(saved -> bearerTokenService.issue(
                        String.valueOf(saved.getId()), saved.getEmail(), "CLIENTE"))
                .map(this::createResponse);
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }
        return isBcryptHash(storedPassword)
                ? passwordEncoder.matches(rawPassword, storedPassword)
                : rawPassword.equals(storedPassword);
    }

    private boolean isBcryptHash(String password) {
        return password != null && password.startsWith("$2");
    }

    private AuthResponse createResponse(IssuedToken issuedToken) {
        return new AuthResponse(
                issuedToken.value(), "Bearer", issuedToken.role(), issuedToken.userId(), issuedToken.email());
    }
}
