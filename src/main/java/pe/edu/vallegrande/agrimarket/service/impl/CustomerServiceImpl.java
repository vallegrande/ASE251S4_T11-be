package pe.edu.vallegrande.agrimarket.service.impl;

import pe.edu.vallegrande.agrimarket.service.*;
import pe.edu.vallegrande.agrimarket.dto.BulkImportResult;
import pe.edu.vallegrande.agrimarket.exception.ResourceNotFoundException;
import pe.edu.vallegrande.agrimarket.model.mongo.Customer;
import pe.edu.vallegrande.agrimarket.model.mongo.AdministrativeDivision;
import pe.edu.vallegrande.agrimarket.repository.mongo.CustomerRepository;
import pe.edu.vallegrande.agrimarket.repository.mongo.StaffUserRepository;
import pe.edu.vallegrande.agrimarket.security.PasswordPolicy;
import pe.edu.vallegrande.agrimarket.util.ExcelGenerator;
import pe.edu.vallegrande.agrimarket.util.PdfGenerator;
import pe.edu.vallegrande.agrimarket.util.ValueParser;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final StaffUserRepository staffUserRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;

    private static final String DEFAULT_IMPORT_PASSWORD = "Importado2025.";
    private static final String[] EXPORT_HEADERS = {
            "id", "docType", "docNumber", "firstName", "lastName", "email",
            "phone", "address", "department", "province", "district", "customerSince", "isActive"
    };

    @Override
    public Flux<Customer> findAll() {
        return customerRepository.findAll();
    }

    @Override
    public Flux<Customer> findByStatus(Boolean isActive) {
        return customerRepository.findByIsActive(isActive);
    }

    @Override
    public Mono<Customer> findById(Long id) {
        return customerRepository.findById(id);
    }

    @Override
    public Mono<Customer> create(Customer customer) {
        String documentError = documentValidationError(customer.getDocType(), customer.getDocNumber());
        if (documentError != null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, documentError));
        }
        if (!PasswordPolicy.isValid(customer.getPassword())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    PasswordPolicy.PASSWORD_REQUIREMENTS_ERROR));
        }
        customer.setEmail(customer.getEmail().trim().toLowerCase());
        customer.setDocType(customer.getDocType().trim().toUpperCase());
        customer.setDocNumber(customer.getDocNumber().trim());
        return saveIfUnique(customer);
    }

    private Mono<Customer> saveIfUnique(Customer customer) {
        return Mono.zip(emailIsInUse(customer.getEmail(), null),
                        customerRepository.existsByDocTypeIgnoreCaseAndDocNumber(
                                customer.getDocType().trim(), customer.getDocNumber().trim()))
                .flatMap(inUse -> {
                    if (inUse.getT1() || inUse.getT2()) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.CONFLICT, "Ya existe una cuenta con ese email o documento"));
                    }
                    return saveNewCustomer(customer);
                });
    }

    private Mono<Customer> saveNewCustomer(Customer customer) {
        return sequenceGeneratorService.generateSequence("customers_sequence")
                .flatMap(sequence -> {
                    customer.setId(sequence);
                    LocalDateTime now = LocalDateTime.now();
                    customer.setCreatedAt(now);
                    customer.setUpdatedAt(now);
                    if (customer.getIsActive() == null) {
                        customer.setIsActive(true);
                    }
                    if (customer.getTotalVisits() == null) {
                        customer.setTotalVisits(0);
                    }
                    customer.setPassword(passwordEncoder.encode(customer.getPassword()));
                    return customerRepository.save(customer);
                });
    }

    @Override
    public Mono<Customer> update(Long id, Customer customer) {
        return customerRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Customer no encontrado con id: " + id)))
                .flatMap(existing -> validateAndUpdate(existing, customer));
    }

    private Mono<Customer> validateAndUpdate(Customer existing, Customer customer) {
        if (customer.getDocType() == null || customer.getDocNumber() == null || customer.getEmail() == null) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "docType, docNumber y email son obligatorios"));
        }
        String normalizedDocType = customer.getDocType().trim().toUpperCase();
        String normalizedDocNumber = customer.getDocNumber().trim();
        String documentError = documentValidationError(normalizedDocType, normalizedDocNumber);
        if (documentError != null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, documentError));
        }
        return checkUpdateConflicts(existing, customer, normalizedDocType, normalizedDocNumber);
    }

    private Mono<Customer> checkUpdateConflicts(
            Customer existing, Customer customer, String docType, String docNumber) {
        String email = customer.getEmail().trim().toLowerCase();
        Mono<Boolean> emailUsed = email.equalsIgnoreCase(existing.getEmail())
                ? Mono.just(false)
                : emailIsInUse(email, existing.getId());
        Mono<Boolean> documentUsed = customerRepository.findByDocTypeIgnoreCaseAndDocNumber(docType, docNumber)
                .filter(found -> !Objects.equals(found.getId(), existing.getId()))
                .hasElement();
        return Mono.zip(emailUsed, documentUsed)
                .flatMap(used -> used.getT1() || used.getT2()
                        ? Mono.error(new ResponseStatusException(
                                HttpStatus.CONFLICT, "Ya existe una cuenta con ese email o documento"))
                        : updateExistingCustomer(existing, customer, email, docType, docNumber));
    }

    private Mono<Customer> updateExistingCustomer(
            Customer existing,
            Customer customer,
            String email,
            String docType,
            String docNumber) {
        existing.setDocType(docType);
        existing.setDocNumber(docNumber);
        existing.setFirstName(customer.getFirstName());
        existing.setLastName(customer.getLastName());
        existing.setEmail(email);
        existing.setPhone(customer.getPhone());
        existing.setAddress(customer.getAddress());
        existing.setUbigeo(customer.getUbigeo());
        if (customer.getPassword() != null && !customer.getPassword().isBlank()) {
            if (!PasswordPolicy.isValid(customer.getPassword())) {
                return Mono.error(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        PasswordPolicy.PASSWORD_REQUIREMENTS_ERROR));
            }
            existing.setPassword(passwordEncoder.encode(customer.getPassword()));
        }
        existing.setTotalVisits(customer.getTotalVisits());
        existing.setLastPurchaseDate(customer.getLastPurchaseDate());
        existing.setCustomerSince(customer.getCustomerSince());
        existing.setNotes(customer.getNotes());
        existing.setIsActive(customer.getIsActive());
        existing.setUpdatedAt(LocalDateTime.now());
        return validateCustomer(existing).then(customerRepository.save(existing));
    }

    private Mono<Void> validateCustomer(Customer customer) {
        Set<ConstraintViolation<Customer>> violations = validator.validate(customer);
        if (violations.isEmpty()) {
            return Mono.empty();
        }
        String message = violations.stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(" | "));
        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, message));
    }

    private String documentValidationError(String docType, String docNumber) {
        if (docType == null || docNumber == null) {
            return "docType y docNumber son obligatorios";
        }
        String normalizedType = docType.trim().toUpperCase();
        String normalizedNumber = docNumber.trim();
        boolean valid = switch (normalizedType) {
            case "DNI" -> normalizedNumber.matches("\\d{8}");
            case "RUC" -> normalizedNumber.matches("\\d{11}");
            case "CE" -> normalizedNumber.matches("\\d{9}");
            default -> false;
        };
        if (valid) return null;
        return switch (normalizedType) {
            case "DNI" -> "DNI debe tener exactamente 8 dígitos";
            case "RUC" -> "RUC debe tener exactamente 11 dígitos";
            case "CE" -> "CE debe tener exactamente 9 dígitos";
            default -> "docType debe ser DNI, RUC o CE";
        };
    }

    private Mono<Boolean> emailIsInUse(String email, Long excludingCustomerId) {
        return Mono.zip(
                        customerRepository.findByEmailIgnoreCase(email)
                                .filter(customer -> !Objects.equals(customer.getId(), excludingCustomerId))
                                .hasElement(),
                        staffUserRepository.existsByEmailIgnoreCase(email))
                .map(result -> result.getT1() || result.getT2());
    }

    @Override
    public Mono<Customer> logicalDelete(Long id) {
        return customerRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Customer no encontrado con id: " + id)))
                .flatMap(existing -> {
                    existing.setIsActive(false);
                    existing.setDeletedAt(LocalDateTime.now());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return customerRepository.save(existing);
                });
    }

    @Override
    public Mono<Customer> restore(Long id) {
        return customerRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Customer no encontrado con id: " + id)))
                .flatMap(existing -> {
                    existing.setIsActive(true);
                    existing.setRestoredAt(LocalDateTime.now());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return customerRepository.save(existing);
                });
    }

    // ------------------------------------------------------------------
    // Carga masiva (importacion) y exportacion
    // ------------------------------------------------------------------

    @Override
    public Mono<BulkImportResult> importCustomers(byte[] content, String filename) {
        return BulkImportSupport.importRows(content, filename, this::mapRow, this::create);
    }

    private Customer mapRow(Map<String, String> row) {
        String docType = ValueParser.required(row, "docType", "doctype", "tipodocumento");
        String docNumber = ValueParser.required(row, "docNumber", "docnumber", "numerodocumento");
        String firstName = ValueParser.required(row, "firstName", "firstname", "nombres");
        String lastName = ValueParser.required(row, "lastName", "lastname", "apellidos");
        String email = ValueParser.required(row, "email", "email", "correo");

        Customer.CustomerBuilder builder = Customer.builder()
                .docType(docType)
                .docNumber(docNumber)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phone(ValueParser.value(row, "phone", "telefono"))
                .address(ValueParser.value(row, "address", "direccion"))
                .ubigeo(mapAdministrativeDivision(row))
                .password(password(row))
                .totalVisits(ValueParser.parseInteger(
                        ValueParser.value(row, "totalvisits", "totalvisitas"), 0))
                .lastPurchaseDate(ValueParser.parseDate(
                        ValueParser.value(row, "lastpurchasedate", "ultimacompra")))
                .customerSince(defaultCustomerSince(row))
                .notes(ValueParser.value(row, "notes", "notas", "observaciones"))
                .isActive(ValueParser.parseBoolean(ValueParser.value(row, "isactive", "activo"), Boolean.TRUE));

        Customer customer = builder.build();
        validate(customer);
        return customer;
    }

    private AdministrativeDivision mapAdministrativeDivision(Map<String, String> row) {
        String idValue = ValueParser.required(row, "ubigeoId", "ubigeoid", "idubigeo");
        Long id;
        try {
            id = Long.valueOf(idValue);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("ubigeoId debe ser un número entero");
        }
        String department = ValueParser.value(row, "department", "departamento");
        String province = ValueParser.value(row, "province", "provincia");
        String district = ValueParser.value(row, "district", "distrito");
        if (department == null || province == null || district == null) {
            throw new IllegalArgumentException(
                    "Si registra ubigeoId debe completar department, province y district");
        }
        return AdministrativeDivision.builder()
                .id(id)
                .department(department)
                .province(province)
                .district(district)
                .build();
    }

    private String password(Map<String, String> row) {
        String value = ValueParser.value(row, "password", "clave", "contrasena", "contraseña");
        return value == null ? DEFAULT_IMPORT_PASSWORD : value;
    }

    private LocalDate defaultCustomerSince(Map<String, String> row) {
        LocalDate parsed = ValueParser.parseDate(ValueParser.value(row, "customersince", "clientedesde"));
        return parsed != null ? parsed : LocalDate.now();
    }

    private void validate(Customer customer) {
        Set<ConstraintViolation<Customer>> violations = validator.validate(customer);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(" | "));
            throw new IllegalArgumentException(message);
        }
    }

    @Override
    public Mono<byte[]> exportCustomersExcel() {
        return customerRepository.findAll()
                .map(this::toExportRow)
                .collectList()
                .flatMap(rows -> Mono.fromCallable(() -> ExcelGenerator.write("Clientes", EXPORT_HEADERS, rows))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @Override
    public Mono<byte[]> exportCustomersPdf() {
        return customerRepository.findAll()
                .map(this::toExportRow)
                .collectList()
                .flatMap(rows -> Mono.fromCallable(() -> PdfGenerator.write("Reporte de Clientes", EXPORT_HEADERS, rows))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    private String[] toExportRow(Customer customer) {
        AdministrativeDivision division = customer.getUbigeo();
        return new String[] {
                text(customer.getId()),
                text(customer.getDocType()),
                text(customer.getDocNumber()),
                text(customer.getFirstName()),
                text(customer.getLastName()),
                text(customer.getEmail()),
                text(customer.getPhone()),
                text(customer.getAddress()),
                division != null ? text(division.getDepartment()) : "",
                division != null ? text(division.getProvince()) : "",
                division != null ? text(division.getDistrict()) : "",
                text(customer.getCustomerSince()),
                text(customer.getIsActive())
        };
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }
}
