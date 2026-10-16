package com.agrimarket.demo.service;

import com.agrimarket.demo.dto.BulkImportResult;
import com.agrimarket.demo.model.mongo.Customer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CustomerService {

    Flux<Customer> findAll();

    Flux<Customer> findByStatus(Boolean isActive);

    Mono<Customer> findById(Long id);

    Mono<Customer> create(Customer customer);

    Mono<Customer> update(Long id, Customer customer);

    Mono<Customer> logicalDelete(Long id);

    Mono<Customer> restore(Long id);

    /** Carga masiva desde un archivo Excel (.xlsx/.xls) o CSV. */
    Mono<BulkImportResult> importCustomers(byte[] content, String filename);

    /** Exporta todos los clientes a un archivo Excel (.xlsx). */
    Mono<byte[]> exportCustomersExcel();

    /** Exporta todos los clientes a un archivo PDF. */
    Mono<byte[]> exportCustomersPdf();
}
