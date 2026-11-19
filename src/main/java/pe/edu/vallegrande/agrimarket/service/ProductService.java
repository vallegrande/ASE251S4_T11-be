package pe.edu.vallegrande.agrimarket.service;

import pe.edu.vallegrande.agrimarket.dto.BulkImportResult;
import pe.edu.vallegrande.agrimarket.dto.StorefrontProductDTO;
import pe.edu.vallegrande.agrimarket.model.mongo.Product;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductService {

    Flux<Product> findAll();

    Flux<Product> findByStatus(Boolean isActive);

    Flux<StorefrontProductDTO> findStorefrontProducts(Integer storeId);

    Mono<Product> findById(Long id);

    Mono<Product> create(Product product);

    Mono<Product> update(Long id, Product product);

    Mono<Product> logicalDelete(Long id);

    Mono<Product> restore(Long id);

    Mono<Product> updateOnlinePublication(Long id, Boolean isPublishedOnline);

    /** Imports XLSX, XLS, or CSV rows and returns per-row errors while continuing valid rows. */
    Mono<BulkImportResult> importProducts(byte[] content, String filename);

    /** Exports the current product catalog as an XLSX workbook. */
    Mono<byte[]> exportProductsExcel();

    /** Exports the current product catalog as a PDF report. */
    Mono<byte[]> exportProductsPdf();
}
