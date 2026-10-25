package com.agrimarket.demo.dto;

import java.math.BigDecimal;

public record StorefrontProductDTO(
        Long id,
        String name,
        String activeIngredient,
        String presentation,
        BigDecimal basePrice,
        String imageUrl,
        String description,
        String sku,
        String categoryId,
        String brandId,
        Integer storeId,
        boolean available) {
}
