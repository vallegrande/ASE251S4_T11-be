package com.agrimarket.demo.rest;

import com.agrimarket.demo.model.mongo.Ubigeo;
import com.agrimarket.demo.service.UbigeoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ubigeos")
@RequiredArgsConstructor
public class UbigeoController {

    private final UbigeoService ubigeoService;

    @GetMapping
    public Flux<Ubigeo> findAll() {
        return ubigeoService.findAll();
    }
}
