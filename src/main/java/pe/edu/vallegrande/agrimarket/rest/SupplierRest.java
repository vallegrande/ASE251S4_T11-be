package pe.edu.vallegrande.agrimarket.rest;

import pe.edu.vallegrande.agrimarket.model.mongo.Supplier;
import pe.edu.vallegrande.agrimarket.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierRest {

    private final SupplierService supplierService;

    public SupplierRest(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public Flux<Supplier> findAll() {
        return supplierService.findAll();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Supplier>> findById(@PathVariable Long id) {
        return supplierService.findById(id)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PostMapping
    public Mono<Supplier> create(@Valid @RequestBody Supplier supplier) {
        return supplierService.create(supplier);
    }
}
