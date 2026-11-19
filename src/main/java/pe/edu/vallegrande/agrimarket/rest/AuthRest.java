package pe.edu.vallegrande.agrimarket.rest;

import pe.edu.vallegrande.agrimarket.dto.AuthResponse;
import pe.edu.vallegrande.agrimarket.dto.CustomerRegistrationRequest;
import pe.edu.vallegrande.agrimarket.dto.CustomerRegistrationResponse;
import pe.edu.vallegrande.agrimarket.dto.LoginRequest;
import pe.edu.vallegrande.agrimarket.service.CustomerRegistrationService;
import pe.edu.vallegrande.agrimarket.service.UserAuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
public class AuthRest {

    private final UserAuthenticationService userAuthenticationService;
    private final CustomerRegistrationService customerRegistrationService;

    public AuthRest(
            UserAuthenticationService userAuthenticationService,
            CustomerRegistrationService customerRegistrationService) {
        this.userAuthenticationService = userAuthenticationService;
        this.customerRegistrationService = customerRegistrationService;
    }

    @PostMapping("/login")
    public Mono<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return userAuthenticationService.login(request);
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        return userAuthenticationService.logout(authorization)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CustomerRegistrationResponse> register(
            @Valid @RequestBody CustomerRegistrationRequest request) {
        return customerRegistrationService.register(request);
    }
}
