package com.agrimarket.demo.service;

import com.agrimarket.demo.dto.BulkImportResult;
import com.agrimarket.demo.exception.ResourceNotFoundException;
import com.agrimarket.demo.model.mongo.Product;
import com.agrimarket.demo.repository.mongo.ProductRepository;
import com.agrimarket.demo.util.ExcelGenerator;
import com.agrimarket.demo.util.PdfGenerator;
import com.agrimarket.demo.util.TabularFileReader;
import com.agrimarket.demo.util.ValueParser;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final Validator validator;

    private static final String[] EXPORT_HEADERS = {
            "id", "name", "sku", "activeIngredient", "presentation", "basePrice",
            "categoryId", "brandId", "description", "imageUrl", "isActive"
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
                    existing.setIsActive(product.getIsActive());
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
        return Mono.fromCallable(() -> TabularFileReader.read(content, filename))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .index()
                .concatMap(tuple -> processRow((int) (tuple.getT1() + 2L), tuple.getT2()))
                .collectList()
                .map(this::buildResult);
    }

    private Mono<Optional<BulkImportResult.RowError>> processRow(int rowNumber, Map<String, String> row) {
        Product product;
        try {
            product = mapRow(row);
        } catch (IllegalArgumentException ex) {
            return Mono.just(Optional.of(rowError(rowNumber, ex.getMessage())));
        }
        return create(product)
                .map(saved -> Optional.<BulkImportResult.RowError>empty())
                .onErrorResume(ex -> Mono.just(Optional.of(rowError(rowNumber, reason(ex)))));
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

    private BulkImportResult buildResult(List<Optional<BulkImportResult.RowError>> results) {
        List<BulkImportResult.RowError> errors = results.stream()
                .flatMap(Optional::stream)
                .toList();
        return BulkImportResult.builder()
                .total(results.size())
                .imported(results.size() - errors.size())
                .failed(errors.size())
                .errors(errors)
                .build();
    }

    private BulkImportResult.RowError rowError(int row, String message) {
        return BulkImportResult.RowError.builder().row(row).message(message).build();
    }

    private String reason(Throwable ex) {
        if (ex instanceof ResponseStatusException rse && rse.getReason() != null) {
            return rse.getReason();
        }
        return ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
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
                text(product.getIsActive())
        };
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }
}
