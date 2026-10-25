
package com.agrimarket.demo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            BearerTokenService bearerTokenService,
            CorsConfigurationSource corsConfigurationSource) {

        return http
                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .securityContextRepository(
                        NoOpServerSecurityContextRepository.getInstance())
                .addFilterAt(
                        new BearerTokenAuthenticationFilter(bearerTokenService),
                        SecurityWebFiltersOrder.AUTHENTICATION)
                .authorizeExchange(exchanges -> exchanges

                        // Las peticiones previas de CORS no requieren token.
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .pathMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**")
                        .permitAll()

                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/auth/login",
                                "/api/auth/register")
                        .permitAll()

                        .pathMatchers(HttpMethod.GET, "/api/storefront/products")
                        .permitAll()

                        .pathMatchers(HttpMethod.POST, "/api/auth/logout")
                        .authenticated()

                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/stores/manage",
                                "/api/stores/manage/**")
                        .hasRole("ADMIN")

                        .pathMatchers(
                                "/api/products/import",
                                "/api/products/export/**")
                        .hasAnyRole("ADMIN", "VENDEDOR")

                        .pathMatchers(HttpMethod.POST, "/api/products")
                        .hasRole("ADMIN")

                        .pathMatchers(HttpMethod.PUT, "/api/products/**")
                        .hasRole("ADMIN")

                        .pathMatchers(HttpMethod.PATCH, "/api/products/**")
                        .hasRole("ADMIN")

                        .pathMatchers("/api/products", "/api/products/**")
                        .hasAnyRole("ADMIN", "VENDEDOR")

                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/categories/**",
                                "/api/brands/**",
                                "/api/ubigeos",
                                "/api/stores/**")
                        .permitAll()

                        .pathMatchers(HttpMethod.GET, "/api/cart").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/cart/items").permitAll()
                        .pathMatchers(HttpMethod.PUT, "/api/cart/items/**").permitAll()
                        .pathMatchers(HttpMethod.DELETE, "/api/cart/items/**").permitAll()

                        .pathMatchers(HttpMethod.POST, "/api/staff-users").permitAll()
                        .pathMatchers("/api/staff-users/**").hasRole("ADMIN")

                        .pathMatchers("/api/inventory/**")
                        .hasAnyRole("ADMIN", "VENDEDOR")

                        .pathMatchers("/api/purchase-orders/**")
                        .hasAnyRole("ADMIN", "VENDEDOR")

                        .pathMatchers("/api/customers/**")
                        .hasAnyRole("ADMIN", "VENDEDOR")

                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/orders/manage",
                                "/api/orders/manage/**")
                        .hasAnyRole("ADMIN", "VENDEDOR")

                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/orders",
                                "/api/orders/**")
                        .hasRole("CLIENTE")

                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/orders",
                                "/api/orders/**")
                        .hasAnyRole("ADMIN", "VENDEDOR")

                        .pathMatchers(
                                HttpMethod.PUT,
                                "/api/orders",
                                "/api/orders/**")
                        .hasAnyRole("ADMIN", "VENDEDOR")

                        .pathMatchers(HttpMethod.PATCH, "/api/orders/**")
                        .hasAnyRole("ADMIN", "VENDEDOR")

                        .pathMatchers(HttpMethod.POST, "/api/stores")
                        .hasRole("ADMIN")

                        .pathMatchers(HttpMethod.PUT, "/api/stores/**")
                        .hasRole("ADMIN")

                        .pathMatchers(HttpMethod.PATCH, "/api/stores/**")
                        .hasRole("ADMIN")

                        .pathMatchers(HttpMethod.DELETE, "/api/stores/**")
                        .hasRole("ADMIN")

                        .pathMatchers("/api/cart/checkout")
                        .hasRole("CLIENTE")

                        .pathMatchers("/api/cart/**")
                        .hasRole("CLIENTE")

                        .pathMatchers("/api/payments/**")
                        .hasRole("CLIENTE")

                        .anyExchange().authenticated())
                .build();
    }
}
