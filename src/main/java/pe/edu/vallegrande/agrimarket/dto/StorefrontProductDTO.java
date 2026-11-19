package pe.edu.vallegrande.agrimarket.dto;

import java.math.BigDecimal;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StorefrontProductDTO {

    private Long id;
    private String name;
    private String activeIngredient;
    private String presentation;
    private BigDecimal basePrice;
    private String imageUrl;
    private String description;
    private String sku;
    private String categoryId;
    private String brandId;
    private Integer storeId;
    private boolean available;
}
