package pe.edu.vallegrande.agrimarket.service;

import pe.edu.vallegrande.agrimarket.exception.ResourceNotFoundException;
import pe.edu.vallegrande.agrimarket.model.mongo.Product;
import pe.edu.vallegrande.agrimarket.repository.mongo.ProductRepository;
import pe.edu.vallegrande.agrimarket.repository.sql.StoreRepository;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderReferenceValidatorTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final StoreRepository storeRepository = mock(StoreRepository.class);
    private final OrderReferenceValidator validator =
            new OrderReferenceValidator(productRepository, storeRepository);

    @Test
    void rejectsEmptyProductList() {
        StepVerifier.create(validator.validateProductsExist(List.of(), "Debe registrar items"))
                .expectErrorMatches(error -> error instanceof ResourceNotFoundException
                        && "Debe registrar items".equals(error.getMessage()))
                .verify();
    }

    @Test
    void rejectsNonNumericProductId() {
        StepVerifier.create(validator.validateProductsExist(List.of("invalid"), "Debe registrar items"))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void acceptsExistingDistinctProductIds() {
        when(productRepository.findAllById(List.of(17L)))
                .thenReturn(Flux.just(Product.builder().id(17L).build()));

        StepVerifier.create(validator.validateProductsExist(List.of("17", "17"), "Debe registrar items"))
                .verifyComplete();
    }
}
