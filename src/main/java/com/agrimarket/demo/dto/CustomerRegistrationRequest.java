package com.agrimarket.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerRegistrationRequest {

    @NotBlank
    @Size(max = 20)
    @Pattern(regexp = "(?i)DNI|RUC|CE", message = "docType debe ser DNI, RUC o CE")
    private String docType;

    @NotBlank
    @Pattern(regexp = "(?:\\d{8}|\\d{11}|\\d{9})",
            message = "docNumber debe tener 8 dígitos para DNI, 11 para RUC o 9 para CE")
    private String docNumber;

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Pattern(regexp = "\\+?[0-9()\\-\\s]{7,20}", message = "phone debe ser un número válido")
    @Size(max = 50)
    private String phone;

    @NotBlank
    @Size(max = 255)
    private String address;

    @NotNull
    @Positive
    private Long ubigeoId;

    @NotBlank
    @Size(min = 12, max = 72)
    private String password;
}
