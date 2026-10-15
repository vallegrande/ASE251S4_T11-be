package com.agrimarket.demo.service;

import com.agrimarket.demo.dto.StaffUserRequest;
import com.agrimarket.demo.exception.ResourceNotFoundException;
import com.agrimarket.demo.model.mongo.StaffUser;
import com.agrimarket.demo.repository.mongo.CustomerRepository;
import com.agrimarket.demo.repository.mongo.StaffUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class StaffUserServiceImpl implements StaffUserService {

    private final StaffUserRepository staffUserRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Flux<StaffUser> findAll() {
        return staffUserRepository.findAll();
    }

    @Override
    public Mono<StaffUser> create(StaffUserRequest request) {
        return authorizeCreation(request)
                .then(Mono.defer(() -> createAuthorized(request)));
    }

    private Mono<Void> authorizeCreation(StaffUserRequest request) {
        return staffUserRepository.count()
                .flatMap(existingUsers -> ReactiveSecurityContextHolder.getContext()
                        .map(context -> context.getAuthentication())
                        .filter(this::isAdmin)
                        .hasElement()
                        .flatMap(isAdmin -> {
                            if (isAdmin || (existingUsers == 0 && "ADMIN".equals(request.getRole()))) {
                                return Mono.empty();
                            }
                            return Mono.error(new ResponseStatusException(
                                    HttpStatus.FORBIDDEN,
                                    existingUsers == 0
                                            ? "El primer usuario interno debe tener rol ADMIN"
                                            : "Solo un ADMIN autenticado puede crear usuarios internos"));
                        }));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.isAuthenticated()
                && authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private Mono<StaffUser> createAuthorized(StaffUserRequest request) {
        if (!validPassword(request.getPassword())) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "password debe tener al menos 12 caracteres y no superar 72 bytes en UTF-8"));
        }
        String email = request.getEmail().trim().toLowerCase();
        return Mono.zip(staffUserRepository.existsByEmailIgnoreCase(email),
                        customerRepository.existsByEmailIgnoreCase(email))
                .flatMap(exists -> {
                    if (exists.getT1() || exists.getT2()) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.CONFLICT, "Ya existe un usuario con ese email"));
                    }
                    LocalDateTime now = LocalDateTime.now();
                    StaffUser user = StaffUser.builder()
                            .email(email)
                            .password(passwordEncoder.encode(request.getPassword()))
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .role(request.getRole())
                            .isActive(request.getIsActive() == null || request.getIsActive())
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    return staffUserRepository.save(user);
                });
    }

    @Override
    public Mono<StaffUser> update(String id, StaffUserRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        return staffUserRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("StaffUser no encontrado con id: " + id)))
                .flatMap(existing -> Mono.zip(
                                staffUserRepository.findByEmailIgnoreCase(email)
                                        .filter(other -> !other.getId().equals(id))
                                        .hasElement(),
                                customerRepository.existsByEmailIgnoreCase(email))
                        .flatMap(emailExists -> {
                            if (emailExists.getT1() || emailExists.getT2()) {
                                return Mono.error(new ResponseStatusException(
                                        HttpStatus.CONFLICT, "Ya existe un usuario con ese email"));
                            }
                            existing.setEmail(email);
                            existing.setFirstName(request.getFirstName());
                            existing.setLastName(request.getLastName());
                            existing.setRole(request.getRole());
                            if (request.getPassword() != null && !request.getPassword().isBlank()) {
                                if (!validPassword(request.getPassword())) {
                                    return Mono.<StaffUser>error(new ResponseStatusException(
                                            HttpStatus.BAD_REQUEST,
                                            "password debe tener al menos 12 caracteres y no superar 72 bytes en UTF-8"));
                                }
                                existing.setPassword(passwordEncoder.encode(request.getPassword()));
                            }
                            if (request.getIsActive() != null) {
                                existing.setIsActive(request.getIsActive());
                            }
                            existing.setUpdatedAt(LocalDateTime.now());
                            return staffUserRepository.save(existing);
                        }));
    }

    @Override
    public Mono<StaffUser> deactivate(String id) {
        return staffUserRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("StaffUser no encontrado con id: " + id)))
                .flatMap(user -> {
                    user.setIsActive(false);
                    user.setUpdatedAt(LocalDateTime.now());
                    return staffUserRepository.save(user);
                });
    }

    private boolean validPassword(String password) {
        return password != null
                && password.length() >= 12
                && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
