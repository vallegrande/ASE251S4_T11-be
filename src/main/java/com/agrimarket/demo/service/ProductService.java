package com.agrimarket.demo.service;

import com.agrimarket.demo.dto.BulkImportResult;
import com.agrimarket.demo.dto.StorefrontProductDTO;
import com.agrimarket.demo.model.mongo.Product;
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

    /** Carga masiva desde un archivo Excel (.xlsx/.xls) o CSV. */
    Mono<BulkImportResult> importProducts(byte[] content, String filename);

    /** Exporta todos los productos a un archivo Excel (.xlsx). */
    Mono<byte[]> exportProductsExcel();

    /** Exporta todos los productos a un archivo PDF. */
    Mono<byte[]> exportProductsPdf();
}
