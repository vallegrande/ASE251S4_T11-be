package pe.edu.vallegrande.agrimarket.service.impl;

import pe.edu.vallegrande.agrimarket.service.*;
import pe.edu.vallegrande.agrimarket.model.mongo.AdministrativeDivision;
import pe.edu.vallegrande.agrimarket.repository.mongo.AdministrativeDivisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AdministrativeDivisionServiceImpl implements AdministrativeDivisionService {

    private final AdministrativeDivisionRepository administrativeDivisionRepository;

    @Override
    public Flux<AdministrativeDivision> findAll() {
        return administrativeDivisionRepository.findAll();
    }

    @Override
    public Mono<AdministrativeDivision> findById(Long id) {
        return administrativeDivisionRepository.findById(id);
    }

    @Override
    public Flux<AdministrativeDivision> findByDepartment(String department) {
        return administrativeDivisionRepository.findByDepartment(department);
    }
}
