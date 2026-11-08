package com.agrimarket.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // Permitir cualquier origen para evitar bloqueos en la nube
        corsConfig.setAllowedOrigins(List.of("*"));

        corsConfig.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH",
                "DELETE", "OPTIONS"
        ));

        corsConfig.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "Accept", "Origin"
        ));

        corsConfig.setAllowCredentials(false); // Debe ser false si usas "*"
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", corsConfig);

        return source;
    }
}