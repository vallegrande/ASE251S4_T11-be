package com.agrimarket.demo.rest;

import com.agrimarket.demo.dto.BulkImportResult;
import com.agrimarket.demo.model.mongo.Customer;
import com.agrimarket.demo.service.CustomerService;
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
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final CustomerService customerService;

    @GetMapping
    public Flux<Customer> findAll() {
        return customerService.findAll();
    }

    @GetMapping("/status/{isActive}")
    public Flux<Customer> findByStatus(@PathVariable Boolean isActive) {
        return customerService.findByStatus(isActive);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Customer>> findById(@PathVariable Long id) {
        return customerService.findById(id)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PostMapping
    public Mono<Customer> create(@Valid @RequestBody Customer customer) {
        return customerService.create(customer);
    }

    @PutMapping("/{id}")
    public Mono<Customer> update(@PathVariable Long id, @Valid @RequestBody Customer customer) {
        return customerService.update(id, customer);
    }

    @PatchMapping("/{id}/delete")
    public Mono<ResponseEntity<Customer>> logicalDelete(@PathVariable Long id) {
        return customerService.logicalDelete(id)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PatchMapping("/{id}/restore")
    public Mono<ResponseEntity<Customer>> restore(@PathVariable Long id) {
        return customerService.restore(id)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    /** Carga masiva de clientes desde un archivo Excel (.xlsx/.xls) o CSV. */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<BulkImportResult> importCustomers(@RequestPart("file") FilePart file) {
        return readBytes(file)
                .flatMap(content -> customerService.importCustomers(content, file.filename()));
    }

    /** Exporta todos los clientes a Excel (.xlsx). */
    @GetMapping("/export/excel")
    public Mono<ResponseEntity<Resource>> exportExcel() {
        return customerService.exportCustomersExcel()
                .map(bytes -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"clientes.xlsx\"")
                        .contentType(XLSX)
                        .body(new ByteArrayResource(bytes)));
    }

    /** Exporta todos los clientes a PDF. */
    @GetMapping("/export/pdf")
    public Mono<ResponseEntity<Resource>> exportPdf() {
        return customerService.exportCustomersPdf()
                .map(bytes -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"clientes.pdf\"")
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
