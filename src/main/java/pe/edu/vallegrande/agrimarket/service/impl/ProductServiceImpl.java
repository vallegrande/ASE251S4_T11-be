package pe.edu.vallegrande.agrimarket.service.impl;

import pe.edu.vallegrande.agrimarket.service.*;
import pe.edu.vallegrande.agrimarket.dto.BulkImportResult;
import pe.edu.vallegrande.agrimarket.dto.StorefrontProductDTO;
import pe.edu.vallegrande.agrimarket.exception.ResourceNotFoundException;
import pe.edu.vallegrande.agrimarket.model.mongo.Product;
import pe.edu.vallegrande.agrimarket.repository.sql.InventoryRepository;
import pe.edu.vallegrande.agrimarket.repository.sql.StoreRepository;
import pe.edu.vallegrande.agrimarket.repository.mongo.ProductRepository;
import pe.edu.vallegrande.agrimarket.util.ExcelGenerator;
import pe.edu.vallegrande.agrimarket.util.PdfGenerator;
import pe.edu.vallegrande.agrimarket.util.ValueParser;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final StoreRepository storeRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final Validator validator;

    private static final String[] EXPORT_HEADERS = {
            "id", "name", "sku", "activeIngredient", "presentation", "basePrice",
            "categoryId", "brandId", "description", "imageUrl", "isActive", "isPublishedOnline"
    };

    @Override
    public Flux<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    public Flux<Product> findByStatus(Boolean isActive) {
        return productRepository.findByIsActive(isActive);
    }

    @Override
    public Flux<StorefrontProductDTO> findStorefrontProducts(Integer storeId) {
        return storeRepository.existsByStoreIdAndIsActiveTrue(storeId)
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "La tienda no existe o no está disponible")))
                .flatMapMany(ignored -> inventoryRepository.findAllByStoreId(storeId)
                        .filter(inventory -> inventory.getCurrentStock() != null
                                && inventory.getCurrentStock() > 0)
                        .flatMap(inventory -> productRepository.findById(
                                        Long.parseLong(inventory.getProductId()))
                                .filter(product -> Boolean.TRUE.equals(product.getIsActive())
                                        && Boolean.TRUE.equals(product.getIsPublishedOnline()))
                                .map(product -> toStorefrontProduct(product, storeId))));
    }

    private StorefrontProductDTO toStorefrontProduct(Product product, Integer storeId) {
        StorefrontProductDTO storefrontProduct = new StorefrontProductDTO();
        storefrontProduct.setId(product.getId());
        storefrontProduct.setName(product.getName());
        storefrontProduct.setActiveIngredient(product.getActiveIngredient());
        storefrontProduct.setPresentation(product.getPresentation());
        storefrontProduct.setBasePrice(product.getBasePrice());
        storefrontProduct.setImageUrl(product.getImageUrl());
        storefrontProduct.setDescription(product.getDescription());
        storefrontProduct.setSku(product.getSku());
        storefrontProduct.setCategoryId(product.getCategoryId());
        storefrontProduct.setBrandId(product.getBrandId());
        storefrontProduct.setStoreId(storeId);
        storefrontProduct.setAvailable(true);
        return storefrontProduct;
    }

    @Override
    public Mono<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    @Override
    public Mono<Product> create(Product product) {
        return sequenceGeneratorService.generateSequence("products_sequence")
                .flatMap(seq -> {
                    product.setId(seq);
                    LocalDateTime now = LocalDateTime.now();
                    product.setCreatedAt(now);
                    product.setUpdatedAt(now);
                    if (product.getIsActive() == null) {
                        product.setIsActive(true);
                    }
                    if (product.getIsPublishedOnline() == null) {
                        product.setIsPublishedOnline(false);
                    }
                    return productRepository.save(product);
                });
    }

    @Override
    public Mono<Product> update(Long id, Product product) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Product no encontrado con id: " + id)))
                .flatMap(existing -> {
                    existing.setName(product.getName());
                    existing.setActiveIngredient(product.getActiveIngredient());
                    existing.setPresentation(product.getPresentation());
                    existing.setBasePrice(product.getBasePrice());
                    existing.setImageUrl(product.getImageUrl());
                    existing.setDescription(product.getDescription());
                    existing.setSku(product.getSku());
                    existing.setCategoryId(product.getCategoryId());
                    existing.setBrandId(product.getBrandId());
                    existing.setMetadata(product.getMetadata());
                    if (product.getIsActive() != null) {
                        existing.setIsActive(product.getIsActive());
                    }
                    if (product.getIsPublishedOnline() != null) {
                        existing.setIsPublishedOnline(product.getIsPublishedOnline());
                    }
                    existing.setUpdatedAt(LocalDateTime.now());
                    return productRepository.save(existing);
                });
    }

    @Override
    public Mono<Product> updateOnlinePublication(Long id, Boolean isPublishedOnline) {
        if (isPublishedOnline == null) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "isPublishedOnline es obligatorio"));
        }
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Product no encontrado con id: " + id)))
                .flatMap(existing -> {
                    existing.setIsPublishedOnline(isPublishedOnline);
                    existing.setUpdatedAt(LocalDateTime.now());
                    return productRepository.save(existing);
                });
    }

    @Override
    public Mono<Product> logicalDelete(Long id) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Product no encontrado con id: " + id)))
                .flatMap(existing -> {
                    existing.setIsActive(false);
                    existing.setDeletedAt(LocalDateTime.now());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return productRepository.save(existing);
                });
    }

    @Override
    public Mono<Product> restore(Long id) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Product no encontrado con id: " + id)))
                .flatMap(existing -> {
                    existing.setIsActive(true);
                    existing.setRestoredAt(LocalDateTime.now());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return productRepository.save(existing);
                });
    }

    // ------------------------------------------------------------------
    // Carga masiva (importacion) y exportacion
    // ------------------------------------------------------------------

    @Override
    public Mono<BulkImportResult> importProducts(byte[] content, String filename) {
        return BulkImportSupport.importRows(
                content, filename, new BulkImportSupport.ImportPlan<>(this::mapRow, this::create));
    }

    private Product mapRow(Map<String, String> row) {
        String name = ValueParser.required(row, "name", "name", "nombre");
        String categoryId = ValueParser.required(row, "categoryId", "categoryid", "categoria");
        String brandId = ValueParser.required(row, "brandId", "brandid", "marca");
        BigDecimal basePrice = ValueParser.parseBigDecimal(
                ValueParser.value(row, "baseprice", "precio", "preciobase"));
        if (basePrice == null) {
            throw new IllegalArgumentException("Falta el campo obligatorio: basePrice");
        }

        Product product = Product.builder()
                .name(name)
                .activeIngredient(ValueParser.value(row, "activeingredient", "principioactivo"))
                .presentation(ValueParser.value(row, "presentation", "presentacion"))
                .basePrice(basePrice)
                .imageUrl(ValueParser.value(row, "imageurl", "imagen"))
                .description(ValueParser.value(row, "description", "descripcion"))
                .sku(ValueParser.value(row, "sku"))
                .categoryId(categoryId)
                .brandId(brandId)
                .isActive(ValueParser.parseBoolean(ValueParser.value(row, "isactive", "activo"), Boolean.TRUE))
                .isPublishedOnline(ValueParser.parseBoolean(
                        ValueParser.value(row, "ispublishedonline", "publicadoonline"), Boolean.FALSE))
                .build();

        validate(product);
        return product;
    }

    private void validate(Product product) {
        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(" | "));
            throw new IllegalArgumentException(message);
        }
    }

    @Override
    public Mono<byte[]> exportProductsExcel() {
        return productRepository.findAll()
                .map(this::toExportRow)
                .collectList()
                .flatMap(rows -> Mono.fromCallable(() -> ExcelGenerator.write("Productos", EXPORT_HEADERS, rows))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @Override
    public Mono<byte[]> exportProductsPdf() {
        return productRepository.findAll()
                .map(this::toExportRow)
                .collectList()
                .flatMap(rows -> Mono.fromCallable(() -> PdfGenerator.write("Reporte de Productos", EXPORT_HEADERS, rows))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    private String[] toExportRow(Product product) {
        return new String[] {
                text(product.getId()),
                text(product.getName()),
                text(product.getSku()),
                text(product.getActiveIngredient()),
                text(product.getPresentation()),
                text(product.getBasePrice()),
                text(product.getCategoryId()),
                text(product.getBrandId()),
                text(product.getDescription()),
                text(product.getImageUrl()),
                text(product.getIsActive()),
                text(product.getIsPublishedOnline())
        };
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }
}
