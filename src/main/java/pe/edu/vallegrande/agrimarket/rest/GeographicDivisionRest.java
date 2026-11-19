package pe.edu.vallegrande.agrimarket.rest;

import pe.edu.vallegrande.agrimarket.model.mongo.GeographicDivision;
import pe.edu.vallegrande.agrimarket.service.GeographicDivisionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ubigeos")
public class GeographicDivisionRest {

    private final GeographicDivisionService geographicDivisionService;

    public GeographicDivisionRest(GeographicDivisionService geographicDivisionService) {
        this.geographicDivisionService = geographicDivisionService;
    }

    @GetMapping
    public Flux<GeographicDivision> findAll() {
        return geographicDivisionService.findAll();
    }
}
