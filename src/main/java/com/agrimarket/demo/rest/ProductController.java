package com.agrimarket.demo.rest;

import com.agrimarket.demo.dto.BulkImportResult;
import com.agrimarket.demo.model.mongo.Product;
import com.agrimarket.demo.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ProductService productService;

    @GetMapping
    public Flux<Product> findAll() {
        return productService.findAll();
    }

    @GetMapping("/status/{isActive}")
    public Flux<Product> findByStatus(@PathVariable Boolean isActive) {
        return productService.findByStatus(isActive);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Product>> findById(@PathVariable Long id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PostMapping
    public Mono<Product> create(@Valid @RequestBody Product product) {
        return productService.create(product);
    }

    @PutMapping("/{id}")
    public Mono<Product> update(@PathVariable Long id, @Valid @RequestBody Product product) {
        return productService.update(id, product);
    }

    @PatchMapping("/{id}/delete")
    public Mono<ResponseEntity<Product>> logicalDelete(@PathVariable Long id) {
        return productService.logicalDelete(id)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PatchMapping("/{id}/restore")
    public Mono<ResponseEntity<Product>> restore(@PathVariable Long id) {
        return productService.restore(id)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    /** Carga masiva de productos desde un archivo Excel (.xlsx/.xls) o CSV. */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<BulkImportResult> importProducts(@RequestPart("file") FilePart file) {
        return readBytes(file)
                .flatMap(content -> productService.importProducts(content, file.filename()));
    }

    /** Exporta todos los productos a Excel (.xlsx). */
    @GetMapping("/export/excel")
    public Mono<ResponseEntity<Resource>> exportExcel() {
        return productService.exportProductsExcel()
                .map(bytes -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"productos.xlsx\"")
                        .contentType(XLSX)
                        .body(new ByteArrayResource(bytes)));
    }

    /** Exporta todos los productos a PDF. */
    @GetMapping("/export/pdf")
    public Mono<ResponseEntity<Resource>> exportPdf() {
        return productService.exportProductsPdf()
                .map(bytes -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"productos.pdf\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(new ByteArrayResource(bytes)));
    }

    private Mono<byte[]> readBytes(FilePart file) {
        return DataBufferUtils.join(file.content())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .defaultIfEmpty(new byte[0]);
    }
}
