package com.agrimarket.demo.model.mongo;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "customers")
@CompoundIndex(name = "ux_customer_document", def = "{'docType': 1, 'docNumber': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    private Long id;

    @NotBlank(message = "docType es obligatorio")
    @Size(max = 20, message = "docType no puede superar los 20 caracteres")
    @Pattern(regexp = "(?i)DNI|RUC|CE", message = "docType debe ser DNI, RUC o CE")
    private String docType;

    @NotBlank(message = "docNumber es obligatorio")
    @Size(max = 20, message = "docNumber no puede superar los 20 caracteres")
    private String docNumber;

    @NotBlank(message = "firstName es obligatorio")
    @Size(max = 100, message = "firstName no puede superar los 100 caracteres")
    private String firstName;

    @NotBlank(message = "lastName es obligatorio")
    @Size(max = 100, message = "lastName no puede superar los 100 caracteres")
    private String lastName;

    @NotBlank(message = "email es obligatorio")
    @Email(message = "email debe ser válido")
    @Size(max = 255, message = "email no puede superar los 255 caracteres")
    @Indexed(unique = true)
    private String email;

    @NotBlank(message = "phone es obligatorio")
    @Pattern(regexp = "\\+?[0-9()\\-\\s]{7,20}", message = "phone debe ser un número válido")
    @Size(max = 50, message = "phone no puede superar los 50 caracteres")
    private String phone;

    @NotBlank(message = "address es obligatorio")
    @Size(max = 255, message = "address no puede superar los 255 caracteres")
    private String address;

    @NotNull(message = "ubigeo es obligatorio")
    @Valid
    private Ubigeo ubigeo;

    @NotBlank(message = "password es obligatorio al crear el cliente")
    @Size(max = 255, message = "password no puede superar los 255 caracteres")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @PositiveOrZero(message = "totalVisits debe ser >= 0")
    @Builder.Default
    private Integer totalVisits = 0;

    private LocalDate lastPurchaseDate;

    @NotNull(message = "customerSince es obligatorio")
    private LocalDate customerSince;

    @Size(max = 1000, message = "notes no puede superar los 1000 caracteres")
    private String notes;

    @Builder.Default
    private Boolean isActive = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime restoredAt;
}
