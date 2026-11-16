package pe.edu.vallegrande.vgmsagrimarket.service;

import pe.edu.vallegrande.vgmsagrimarket.model.mongo.Brand;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BrandService {

    Flux<Brand> findAll();

    Flux<Brand> findByStatus(Boolean isActive);

    Mono<Brand> findById(Long id);

    Mono<Brand> create(Brand brand);

    Mono<Brand> update(Long id, Brand brand);

    Mono<Brand> logicalDelete(Long id);

    Mono<Brand> restore(Long id);
}
