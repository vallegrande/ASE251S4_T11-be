package pe.edu.vallegrande.agrimarket.controller;

import pe.edu.vallegrande.agrimarket.model.mongo.Ubigeo;
import pe.edu.vallegrande.agrimarket.service.UbigeoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ubigeos")
public class UbigeoRest {

    private final UbigeoService ubigeoService;

    public UbigeoRest(UbigeoService ubigeoService) {
        this.ubigeoService = ubigeoService;
    }

    @GetMapping
    public Flux<Ubigeo> findAll() {
        return ubigeoService.findAll();
    }
}
