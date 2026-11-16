package pe.edu.vallegrande.vgmsagrimarket.controller;

import pe.edu.vallegrande.vgmsagrimarket.model.mongo.Ubigeo;
import pe.edu.vallegrande.vgmsagrimarket.service.UbigeoService;
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
