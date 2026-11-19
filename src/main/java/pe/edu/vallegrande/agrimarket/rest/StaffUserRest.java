package pe.edu.vallegrande.agrimarket.rest;

import pe.edu.vallegrande.agrimarket.dto.StaffUserRequest;
import pe.edu.vallegrande.agrimarket.model.mongo.StaffUser;
import pe.edu.vallegrande.agrimarket.service.StaffUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/staff-users")
public class StaffUserRest {

    private final StaffUserService staffUserService;

    public StaffUserRest(StaffUserService staffUserService) {
        this.staffUserService = staffUserService;
    }

    // ADMIN: lista las cuentas internas de administradores y vendedores.
    @GetMapping
    public Flux<StaffUser> findAll() {
        return staffUserService.findAll();
    }

    // El primer ADMIN puede crearse sin token solo cuando todavía no hay usuarios internos.
    @PostMapping
    public Mono<StaffUser> create(@Valid @RequestBody StaffUserRequest request) {
        return staffUserService.create(request);
    }

    @PutMapping("/{id}")
    public Mono<StaffUser> update(@PathVariable String id, @Valid @RequestBody StaffUserRequest request) {
        return staffUserService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public Mono<StaffUser> deactivate(@PathVariable String id) {
        return staffUserService.deactivate(id);
    }
}
