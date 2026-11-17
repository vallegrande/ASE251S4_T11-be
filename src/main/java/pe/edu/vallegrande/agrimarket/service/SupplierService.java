package pe.edu.vallegrande.agrimarket.service;

import pe.edu.vallegrande.agrimarket.model.mongo.Supplier;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SupplierService {

    Flux<Supplier> findAll();

    Mono<Supplier> findById(Long id);

    Mono<Supplier> create(Supplier supplier);
}
