package pe.edu.vallegrande.agrimarket.service.impl;

import pe.edu.vallegrande.agrimarket.service.*;
import pe.edu.vallegrande.agrimarket.model.mongo.GeographicDivision;
import pe.edu.vallegrande.agrimarket.repository.mongo.GeographicDivisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GeographicDivisionServiceImpl implements GeographicDivisionService {

    private final GeographicDivisionRepository geographicDivisionRepository;

    @Override
    public Flux<GeographicDivision> findAll() {
        return geographicDivisionRepository.findAll();
    }

    @Override
    public Mono<GeographicDivision> findById(Long id) {
        return geographicDivisionRepository.findById(id);
    }

    @Override
    public Flux<GeographicDivision> findByDepartment(String department) {
        return geographicDivisionRepository.findByDepartment(department);
    }
}
