package pe.edu.vallegrande.agrimarket.service.impl;

import pe.edu.vallegrande.agrimarket.service.*;
import pe.edu.vallegrande.agrimarket.model.mongo.Ubigeo;
import pe.edu.vallegrande.agrimarket.repository.mongo.UbigeoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UbigeoServiceImpl implements UbigeoService {

    private final UbigeoRepository ubigeoRepository;

    @Override
    public Flux<Ubigeo> findAll() {
        return ubigeoRepository.findAll();
    }

    @Override
    public Mono<Ubigeo> findById(Long id) {
        return ubigeoRepository.findById(id);
    }

    @Override
    public Flux<Ubigeo> findByDepartment(String department) {
        return ubigeoRepository.findByDepartment(department);
    }
}
