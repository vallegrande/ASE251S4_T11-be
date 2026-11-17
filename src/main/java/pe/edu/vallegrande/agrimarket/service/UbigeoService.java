package pe.edu.vallegrande.agrimarket.service;

import pe.edu.vallegrande.agrimarket.model.mongo.Ubigeo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UbigeoService {

    Flux<Ubigeo> findAll();

    Mono<Ubigeo> findById(Long id);

    Flux<Ubigeo> findByDepartment(String department);
}
