package pe.edu.vallegrande.agrimarket.service;

import pe.edu.vallegrande.agrimarket.model.mongo.AdministrativeDivision;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AdministrativeDivisionService {

    Flux<AdministrativeDivision> findAll();

    Mono<AdministrativeDivision> findById(Long id);

    Flux<AdministrativeDivision> findByDepartment(String department);
}
