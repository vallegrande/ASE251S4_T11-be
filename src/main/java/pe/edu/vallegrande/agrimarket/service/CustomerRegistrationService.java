package pe.edu.vallegrande.agrimarket.service;

import pe.edu.vallegrande.agrimarket.dto.CustomerRegistrationRequest;
import pe.edu.vallegrande.agrimarket.dto.CustomerRegistrationResponse;
import pe.edu.vallegrande.agrimarket.model.mongo.Customer;
import pe.edu.vallegrande.agrimarket.model.mongo.GeographicDivision;
import pe.edu.vallegrande.agrimarket.repository.mongo.CustomerRepository;
import pe.edu.vallegrande.agrimarket.repository.mongo.GeographicDivisionRepository;
import pe.edu.vallegrande.agrimarket.repository.mongo.StaffUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CustomerRegistrationService {

    private final StaffUserRepository staffUserRepository;
    private final CustomerRepository customerRepository;
    private final GeographicDivisionRepository geographicDivisionRepository;
    private final CustomerService customerService;

    public Mono<CustomerRegistrationResponse> register(CustomerRegistrationRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        return Mono.zip(
                        staffUserRepository.existsByEmailIgnoreCase(email),
                        customerRepository.existsByEmailIgnoreCase(email),
                        customerRepository.existsByDocTypeIgnoreCaseAndDocNumber(
                                request.getDocType().trim().toUpperCase(), request.getDocNumber().trim()))
                .flatMap(exists -> registerIfAvailable(request, email, exists));
    }

    private Mono<CustomerRegistrationResponse> registerIfAvailable(
            CustomerRegistrationRequest request, String email, Tuple3<Boolean, Boolean, Boolean> exists) {
        if (exists.getT1() || exists.getT2() || exists.getT3()) {
            return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una cuenta con ese email o documento"));
        }
        return geographicDivisionRepository.findById(request.getUbigeoId())
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "El ubigeo indicado no existe")))
                .flatMap(division -> customerService.create(buildCustomer(request, email, division)))
                .map(saved -> new CustomerRegistrationResponse(
                        saved.getId(), saved.getEmail(), saved.getFirstName(), saved.getLastName()));
    }

    private Customer buildCustomer(
            CustomerRegistrationRequest request, String email, GeographicDivision division) {
        LocalDateTime now = LocalDateTime.now();
        return Customer.builder()
                .docType(request.getDocType().trim().toUpperCase())
                .docNumber(request.getDocNumber().trim())
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(email)
                .phone(request.getPhone().trim())
                .address(request.getAddress())
                .ubigeo(division)
                .password(request.getPassword())
                .totalVisits(0)
                .customerSince(LocalDate.now())
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
