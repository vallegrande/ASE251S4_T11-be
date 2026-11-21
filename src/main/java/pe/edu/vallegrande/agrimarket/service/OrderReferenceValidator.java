package pe.edu.vallegrande.agrimarket.service;

import pe.edu.vallegrande.agrimarket.exception.ResourceNotFoundException;
import pe.edu.vallegrande.agrimarket.repository.mongo.ProductRepository;
import pe.edu.vallegrande.agrimarket.repository.sql.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderReferenceValidator {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

    public Mono<Void> validateStoreExists(Integer storeId) {
        if (storeId == null) {
            return Mono.empty();
        }
        return storeRepository.existsById(storeId)
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Store no existe con id: " + storeId)))
                .then();
    }

    public Mono<Void> validateStoreAndProducts(
            Integer storeId, Collection<String> productIds, String emptyItemsMessage) {
        return validateStoreExists(storeId)
                .then(validateProductsExist(productIds, emptyItemsMessage));
    }

    public Mono<Void> validateProductsExist(Collection<String> productIds, String emptyItemsMessage) {
        if (productIds == null || productIds.isEmpty()) {
            return Mono.error(new ResourceNotFoundException(emptyItemsMessage));
        }
        List<Long> distinctIds;
        try {
            distinctIds = productIds.stream()
                    .map(Long::valueOf)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (NumberFormatException exception) {
            return Mono.error(new ResourceNotFoundException(
                    "Existe un productId con formato inválido en los items"));
        }
        return productRepository.findAllById(distinctIds).collectList()
                .flatMap(found -> {
                    if (found.size() != distinctIds.size()) {
                        return Mono.error(new ResourceNotFoundException(
                                "Algunos productId no existen en el catálogo (encontrados: "
                                        + found.size() + "/" + distinctIds.size() + ")"));
                    }
                    return Mono.empty();
                });
    }
}
