package pe.edu.vallegrande.agrimarket.service;

import pe.edu.vallegrande.agrimarket.model.mongo.Customer;
import pe.edu.vallegrande.agrimarket.model.mongo.GeographicDivision;
import pe.edu.vallegrande.agrimarket.util.ValueParser;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CustomerImportMapper {

    private static final String DEFAULT_IMPORT_PASSWORD = "Importado2025.";
    private static final int DEFAULT_TOTAL_VISITS = 0;

    private final Validator validator;

    public Customer mapRow(Map<String, String> row) {
        Customer customer = buildCustomer(row);
        validate(customer);
        return customer;
    }

    private Customer buildCustomer(Map<String, String> row) {
        return Customer.builder()
                .docType(ValueParser.required(row, "docType", "doctype", "tipodocumento"))
                .docNumber(ValueParser.required(row, "docNumber", "docnumber", "numerodocumento"))
                .firstName(ValueParser.required(row, "firstName", "firstname", "nombres"))
                .lastName(ValueParser.required(row, "lastName", "lastname", "apellidos"))
                .email(ValueParser.required(row, "email", "email", "correo"))
                .phone(ValueParser.value(row, "phone", "telefono"))
                .address(ValueParser.value(row, "address", "direccion"))
                .ubigeo(mapGeographicDivision(row))
                .password(password(row))
                .totalVisits(ValueParser.parseInteger(
                        ValueParser.value(row, "totalvisits", "totalvisitas"), DEFAULT_TOTAL_VISITS))
                .lastPurchaseDate(ValueParser.parseDate(
                        ValueParser.value(row, "lastpurchasedate", "ultimacompra")))
                .customerSince(defaultCustomerSince(row))
                .notes(ValueParser.value(row, "notes", "notas", "observaciones"))
                .isActive(ValueParser.parseBoolean(ValueParser.value(row, "isactive", "activo"), Boolean.TRUE))
                .build();
    }

    private GeographicDivision mapGeographicDivision(Map<String, String> row) {
        String idValue = ValueParser.required(row, "ubigeoId", "ubigeoid", "idubigeo");
        long id = parseGeographicDivisionId(idValue);
        String department = ValueParser.value(row, "department", "departamento");
        String province = ValueParser.value(row, "province", "provincia");
        String district = ValueParser.value(row, "district", "distrito");
        if (department == null || province == null || district == null) {
            throw new IllegalArgumentException(
                    "Si registra ubigeoId debe completar department, province y district");
        }
        return GeographicDivision.builder()
                .id(id)
                .department(department)
                .province(province)
                .district(district)
                .build();
    }

    private long parseGeographicDivisionId(String idValue) {
        try {
            return Long.parseLong(idValue);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("ubigeoId debe ser un número entero", exception);
        }
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
                    .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                    .collect(Collectors.joining(" | "));
            throw new IllegalArgumentException(message);
        }
    }
}
