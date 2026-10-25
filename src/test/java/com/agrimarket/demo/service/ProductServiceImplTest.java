package com.agrimarket.demo.service;

import com.agrimarket.demo.dto.StorefrontProductDTO;
import com.agrimarket.demo.model.mongo.Product;
import com.agrimarket.demo.model.sql.Inventory;
import com.agrimarket.demo.repository.mongo.ProductRepository;
import com.agrimarket.demo.repository.sql.InventoryRepository;
import com.agrimarket.demo.repository.sql.StoreRepository;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceImplTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final InventoryRepository inventoryRepository = mock(InventoryRepository.class);
    private final StoreRepository storeRepository = mock(StoreRepository.class);
    private final SequenceGeneratorService sequenceGeneratorService = mock(SequenceGeneratorService.class);
    private final Validator validator = mock(Validator.class);

    private final ProductServiceImpl productService = new ProductServiceImpl(
            productRepository,
            inventoryRepository,
            storeRepository,
            sequenceGeneratorService,
            validator);

    @Test
    void storefrontReturnsOnlyPublishedActiveProductsWithPositiveStock() {
        when(storeRepository.existsByStoreIdAndIsActiveTrue(7)).thenReturn(Mono.just(true));
        when(inventoryRepository.findAllByStoreId(7)).thenReturn(Flux.just(
                inventory("11", 7, 5),
                inventory("12", 7, 0),
                inventory("13", 7, 3),
                inventory("14", 7, 2)));
        when(productRepository.findById(11L)).thenReturn(Mono.just(product(11L, true, true)));
        when(productRepository.findById(13L)).thenReturn(Mono.just(product(13L, true, false)));
        when(productRepository.findById(14L)).thenReturn(Mono.just(product(14L, false, true)));

        StepVerifier.create(productService.findStorefrontProducts(7))
                .expectNext(new StorefrontProductDTO(
                        11L,
                        "Producto 11",
                        null,
                        "Presentación",
                        BigDecimal.TEN,
                        null,
                        null,
                        "SKU-11",
                        "1",
                        "2",
                        7,
                        true))
                .verifyComplete();

        verify(productRepository, never()).findById(12L);
    }

    @Test
    void storefrontRejectsInactiveStore() {
        when(storeRepository.existsByStoreIdAndIsActiveTrue(7)).thenReturn(Mono.just(false));

        StepVerifier.create(productService.findStorefrontProducts(7))
                .expectError(ResponseStatusException.class)
                .verify();

        verify(inventoryRepository, never()).findAllByStoreId(7);
    }

    private Inventory inventory(String productId, Integer storeId, Integer stock) {
        return Inventory.builder()
                .productId(productId)
                .storeId(storeId)
                .currentStock(stock)
                .build();
    }

    private Product product(Long id, boolean active, boolean publishedOnline) {
        return Product.builder()
                .id(id)
                .name("Producto " + id)
                .presentation("Presentación")
                .basePrice(BigDecimal.TEN)
                .sku("SKU-" + id)
                .categoryId("1")
                .brandId("2")
                .isActive(active)
                .isPublishedOnline(publishedOnline)
                .build();
    }
}
