package pe.edu.vallegrande.agrimarket.service;

import pe.edu.vallegrande.agrimarket.model.mongo.GeographicDivision;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GeographicDivisionService {

    Flux<GeographicDivision> findAll();

    Mono<GeographicDivision> findById(Long id);

    Flux<GeographicDivision> findByDepartment(String department);
}
