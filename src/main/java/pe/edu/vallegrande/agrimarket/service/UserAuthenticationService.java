package pe.edu.vallegrande.agrimarket.service;

import pe.edu.vallegrande.agrimarket.dto.AuthResponse;
import pe.edu.vallegrande.agrimarket.dto.LoginRequest;
import pe.edu.vallegrande.agrimarket.model.mongo.Customer;
import pe.edu.vallegrande.agrimarket.model.mongo.StaffUser;
import pe.edu.vallegrande.agrimarket.repository.mongo.CustomerRepository;
import pe.edu.vallegrande.agrimarket.repository.mongo.StaffUserRepository;
import pe.edu.vallegrande.agrimarket.security.BearerTokenService;
import pe.edu.vallegrande.agrimarket.security.IssuedToken;
import pe.edu.vallegrande.agrimarket.security.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserAuthenticationService {

    private static final String CUSTOMER_ROLE = "CLIENTE";
    private static final String BEARER_SCHEME = "Bearer ";
    private static final String INVALID_CREDENTIALS_MESSAGE = "Email o contraseña incorrectos";

    private final StaffUserRepository staffUserRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final BearerTokenService bearerTokenService;
    private final CartService cartService;

    public Mono<AuthResponse> login(LoginRequest request) {
        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length
                > PasswordPolicy.MAX_BCRYPT_PASSWORD_BYTES) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, PasswordPolicy.PASSWORD_TOO_LONG_ERROR));
        }
        String email = request.getEmail().trim().toLowerCase();
        return staffUserRepository.findByEmailIgnoreCase(email)
                .flatMap(user -> authenticateStaffUser(user, request.getPassword()))
                .switchIfEmpty(Mono.defer(() -> customerRepository.findByEmailIgnoreCase(email)
                        .flatMap(customer -> authenticateCustomer(customer, request.getPassword()))))
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE)))
                .flatMap(response -> {
                    if (!CUSTOMER_ROLE.equals(response.getRole()) || request.getSessionToken() == null
                            || request.getSessionToken().isBlank()) {
                        return Mono.just(response);
                    }
                    return cartService.mergeGuestCart(request.getSessionToken(), response.getUserId())
                            .thenReturn(response);
                });
    }

    public Mono<Void> logout(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_SCHEME)) {
            return Mono.empty();
        }
        return bearerTokenService.revoke(authorization.substring(BEARER_SCHEME.length()));
    }

    private Mono<AuthResponse> authenticateStaffUser(StaffUser user, String rawPassword) {
        if (user.getPassword() == null) {
            return invalidCredentials();
        }
        if (!Boolean.TRUE.equals(user.getIsActive())
                || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            return invalidCredentials();
        }
        return bearerTokenService.issue(user.getId(), user.getEmail(), user.getRole())
                .map(this::createResponse);
    }

    private Mono<AuthResponse> authenticateCustomer(Customer customer, String rawPassword) {
        if (!Boolean.TRUE.equals(customer.getIsActive())
                || !passwordMatches(rawPassword, customer.getPassword())) {
            return Mono.empty();
        }
        Mono<Customer> customerMono = Mono.just(customer);
        if (!isBcryptHash(customer.getPassword())) {
            customer.setPassword(passwordEncoder.encode(rawPassword));
            customer.setUpdatedAt(LocalDateTime.now());
            customerMono = customerRepository.save(customer);
        }
        return customerMono.flatMap(saved -> bearerTokenService.issue(
                        String.valueOf(saved.getId()), saved.getEmail(), CUSTOMER_ROLE))
                .map(this::createResponse);
    }

    private Mono<AuthResponse> invalidCredentials() {
        return Mono.error(new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE));
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
