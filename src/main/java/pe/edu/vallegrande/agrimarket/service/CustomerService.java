package pe.edu.vallegrande.agrimarket.service;

import pe.edu.vallegrande.agrimarket.dto.BulkImportResult;
import pe.edu.vallegrande.agrimarket.model.mongo.Customer;
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

    Mono<BulkImportResult> importCustomers(byte[] content, String filename);

    Mono<byte[]> exportCustomersExcel();

    Mono<byte[]> exportCustomersExcel(Boolean isActive);

    Mono<byte[]> exportCustomersPdf();

    Mono<byte[]> exportCustomersPdf(Boolean isActive);
}
