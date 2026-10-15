package com.agrimarket.demo.security;

import com.agrimarket.demo.model.mongo.AuthSession;
import com.agrimarket.demo.repository.mongo.AuthSessionRepository;
import com.agrimarket.demo.repository.mongo.CustomerRepository;
import com.agrimarket.demo.repository.mongo.StaffUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BearerTokenService {

    private final AuthSessionRepository authSessionRepository;
    private final StaffUserRepository staffUserRepository;
    private final CustomerRepository customerRepository;

    @Value("${agrimarket.security.auth-token.expiration-ms:86400000}")
    private long expirationMs;

    public Mono<IssuedToken> issue(String userId, String email, String role) {
        if (expirationMs <= 0) {
            return Mono.error(new IllegalStateException(
                    "agrimarket.security.auth-token.expiration-ms debe ser mayor que cero"));
        }
        String token = BearerTokenCodec.generate();
        LocalDateTime now = LocalDateTime.now();
        AuthSession session = AuthSession.builder()
                .tokenHash(BearerTokenCodec.hash(token))
                .userId(userId)
                .email(email)
                .role(role)
                .createdAt(now)
                .expiresAt(now.plus(Duration.ofMillis(expirationMs)))
                .build();
        return authSessionRepository.save(session)
                .map(saved -> new IssuedToken(token, role, userId, email));
    }

    public Mono<Authentication> authenticate(String token) {
        if (token == null || token.length() != 43) {
            return Mono.empty();
        }
        return authSessionRepository
                .findByTokenHashAndExpiresAtAfter(BearerTokenCodec.hash(token), LocalDateTime.now())
                .flatMap(this::isAccountActive)
                .filter(ActiveSession::active)
                .map(active -> {
                    AuthSession session = active.session();
                    return new UsernamePasswordAuthenticationToken(
                            session.getEmail(),
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + session.getRole())));
                });
    }

    public Mono<Void> revoke(String token) {
        if (token == null || token.length() != 43) {
            return Mono.empty();
        }
        return authSessionRepository.deleteByTokenHash(BearerTokenCodec.hash(token));
    }

    private Mono<ActiveSession> isAccountActive(AuthSession session) {
        if ("CLIENTE".equals(session.getRole())) {
            long customerId;
            try {
                customerId = Long.parseLong(session.getUserId());
            } catch (NumberFormatException exception) {
                return Mono.just(new ActiveSession(session, false));
            }
            return customerRepository.findById(customerId)
                    .map(customer -> new ActiveSession(
                            session, Boolean.TRUE.equals(customer.getIsActive())
                            && customer.getEmail().equalsIgnoreCase(session.getEmail())))
                    .defaultIfEmpty(new ActiveSession(session, false));
        }
        if ("ADMIN".equals(session.getRole()) || "VENDEDOR".equals(session.getRole())) {
            return staffUserRepository.findById(session.getUserId())
                    .map(user -> new ActiveSession(
                            session,
                            Boolean.TRUE.equals(user.getIsActive())
                                    && user.getRole().equals(session.getRole())
                                    && user.getEmail().equalsIgnoreCase(session.getEmail())))
                    .defaultIfEmpty(new ActiveSession(session, false));
        }
        return Mono.error(new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "La sesión contiene un rol no válido"));
    }

    private record ActiveSession(AuthSession session, boolean active) {
    }
}
