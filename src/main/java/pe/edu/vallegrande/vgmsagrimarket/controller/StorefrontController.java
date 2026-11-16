package pe.edu.vallegrande.vgmsagrimarket.controller;

import pe.edu.vallegrande.vgmsagrimarket.dto.StorefrontProductDTO;
import pe.edu.vallegrande.vgmsagrimarket.service.ProductService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/storefront")
@Validated
@RequiredArgsConstructor
public class StorefrontController {

    private final ProductService productService;

    @GetMapping("/products")
    public Flux<StorefrontProductDTO> findProducts(
            @RequestParam @Positive Integer storeId) {
        return productService.findStorefrontProducts(storeId);
    }
}
