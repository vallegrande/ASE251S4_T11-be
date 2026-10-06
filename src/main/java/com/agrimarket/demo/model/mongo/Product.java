package com.agrimarket.demo.model.mongo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    private Long id;

    @NotBlank(message = "name es obligatorio")
    @Size(max = 200, message = "name no puede superar los 200 caracteres")
    private String name;

    @Size(max = 200, message = "activeIngredient no puede superar los 200 caracteres")
    private String activeIngredient;

    @Size(max = 200, message = "presentation no puede superar los 200 caracteres")
    private String presentation;

    @NotNull(message = "basePrice es obligatorio")
    @DecimalMin(value = "0.00", inclusive = false, message = "basePrice debe ser mayor a 0")
    private BigDecimal basePrice;

    @Size(max = 500, message = "imageUrl no puede superar los 500 caracteres")
    private String imageUrl;

    @Size(max = 2000, message = "description no puede superar los 2000 caracteres")
    private String description;

    @Size(max = 50, message = "sku no puede superar los 50 caracteres")
    private String sku;

    @NotBlank(message = "categoryId es obligatorio")
    private String categoryId;

    @NotBlank(message = "brandId es obligatorio")
    private String brandId;

    private Map<String, Object> metadata;

    @Builder.Default
    private Boolean isActive = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime restoredAt;
}
