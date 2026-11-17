package pe.edu.vallegrande.agrimarket.controller;

import pe.edu.vallegrande.agrimarket.dto.StorefrontProductDTO;
import pe.edu.vallegrande.agrimarket.service.ProductService;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/storefront")
@Validated
public class StorefrontRest {

    private final ProductService productService;

    public StorefrontRest(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public Flux<StorefrontProductDTO> findProducts(
            @RequestParam @Positive Integer storeId) {
        return productService.findStorefrontProducts(storeId);
    }
}
