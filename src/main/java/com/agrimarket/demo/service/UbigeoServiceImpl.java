package com.agrimarket.demo.service;

import com.agrimarket.demo.model.mongo.Ubigeo;
import com.agrimarket.demo.repository.mongo.UbigeoRepository;
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
