package com.agrimarket.demo.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class BearerTokenAuthenticationFilter implements WebFilter {

    private final BearerTokenService bearerTokenService;

    public BearerTokenAuthenticationFilter(BearerTokenService bearerTokenService) {
        this.bearerTokenService = bearerTokenService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String token = authorization.substring(7);
        return bearerTokenService.authenticate(token)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(result -> result
                        .map(authentication -> authenticatedRequest(exchange, chain, authentication))
                        .orElseGet(() -> unauthorized(exchange)));
    }

    private Mono<Void> authenticatedRequest(
            ServerWebExchange exchange, WebFilterChain chain, Authentication authentication) {
        return chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
