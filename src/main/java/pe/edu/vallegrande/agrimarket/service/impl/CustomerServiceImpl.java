package pe.edu.vallegrande.agrimarket.service.impl;

import pe.edu.vallegrande.agrimarket.dto.BulkImportResult;
import pe.edu.vallegrande.agrimarket.exception.ResourceNotFoundException;
import pe.edu.vallegrande.agrimarket.model.mongo.Customer;
import pe.edu.vallegrande.agrimarket.repository.mongo.CustomerRepository;
import pe.edu.vallegrande.agrimarket.repository.mongo.StaffUserRepository;
import pe.edu.vallegrande.agrimarket.security.PasswordPolicy;
import pe.edu.vallegrande.agrimarket.service.BulkImportSupport;
import pe.edu.vallegrande.agrimarket.service.CustomerExportService;
import pe.edu.vallegrande.agrimarket.service.CustomerImportMapper;
import pe.edu.vallegrande.agrimarket.service.CustomerService;
import pe.edu.vallegrande.agrimarket.service.SequenceGeneratorService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private static final int DEFAULT_TOTAL_VISITS = 0;
    private static final int DNI_DOCUMENT_NUMBER_LENGTH = 8;
    private static final int RUC_DOCUMENT_NUMBER_LENGTH = 11;
    private static final int CE_DOCUMENT_NUMBER_LENGTH = 9;
    private static final String DOCUMENT_NUMBER_PATTERN_TEMPLATE = "\\d{%d}";
    private static final String REQUIRED_DOCUMENT_ERROR = "docType y docNumber son obligatorios";
    private static final String UNSUPPORTED_DOCUMENT_TYPE_ERROR = "docType debe ser DNI, RUC o CE";

    private final CustomerRepository customerRepository;
    private final StaffUserRepository staffUserRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;
    private final CustomerImportMapper customerImportMapper;
    private final CustomerExportService customerExportService;

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
                        customer.setTotalVisits(DEFAULT_TOTAL_VISITS);
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
            return REQUIRED_DOCUMENT_ERROR;
        }
        String normalizedType = docType.trim().toUpperCase();
        String normalizedNumber = docNumber.trim();
        boolean valid = switch (normalizedType) {
            case "DNI" -> hasExpectedDocumentLength(normalizedNumber, DNI_DOCUMENT_NUMBER_LENGTH);
            case "RUC" -> hasExpectedDocumentLength(normalizedNumber, RUC_DOCUMENT_NUMBER_LENGTH);
            case "CE" -> hasExpectedDocumentLength(normalizedNumber, CE_DOCUMENT_NUMBER_LENGTH);
            default -> false;
        };
        if (valid) {
            return null;
        }
        return switch (normalizedType) {
            case "DNI" -> documentLengthError("DNI", DNI_DOCUMENT_NUMBER_LENGTH);
            case "RUC" -> documentLengthError("RUC", RUC_DOCUMENT_NUMBER_LENGTH);
            case "CE" -> documentLengthError("CE", CE_DOCUMENT_NUMBER_LENGTH);
            default -> UNSUPPORTED_DOCUMENT_TYPE_ERROR;
        };
    }

    private String documentLengthError(String documentType, int expectedLength) {
        return documentType + " debe tener exactamente " + expectedLength + " dígitos";
    }

    private boolean hasExpectedDocumentLength(String documentNumber, int expectedLength) {
        String pattern = String.format(Locale.ROOT, DOCUMENT_NUMBER_PATTERN_TEMPLATE, expectedLength);
        return documentNumber.matches(pattern);
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

    @Override
    public Mono<BulkImportResult> importCustomers(byte[] content, String filename) {
        return BulkImportSupport.importRows(
                content, filename, new BulkImportSupport.ImportPlan<>(customerImportMapper::mapRow, this::create));
    }

    @Override
    public Mono<byte[]> exportCustomersExcel() {
        return exportCustomersExcel(null);
    }

    @Override
    public Mono<byte[]> exportCustomersExcel(Boolean isActive) {
        return customerExportService.exportExcel(isActive);
    }

    @Override
    public Mono<byte[]> exportCustomersPdf() {
        return exportCustomersPdf(null);
    }

    @Override
    public Mono<byte[]> exportCustomersPdf(Boolean isActive) {
        return customerExportService.exportPdf(isActive);
    }
}
