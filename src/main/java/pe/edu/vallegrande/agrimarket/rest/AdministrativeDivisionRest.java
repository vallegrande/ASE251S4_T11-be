package pe.edu.vallegrande.agrimarket.rest;

import pe.edu.vallegrande.agrimarket.model.mongo.AdministrativeDivision;
import pe.edu.vallegrande.agrimarket.service.AdministrativeDivisionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ubigeos")
public class AdministrativeDivisionRest {

    private final AdministrativeDivisionService administrativeDivisionService;

    public AdministrativeDivisionRest(AdministrativeDivisionService administrativeDivisionService) {
        this.administrativeDivisionService = administrativeDivisionService;
    }

    @GetMapping
    public Flux<AdministrativeDivision> findAll() {
        return administrativeDivisionService.findAll();
    }
}
