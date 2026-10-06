package com.agrimarket.demo.model.mongo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "customers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    private Long id;

    @NotBlank(message = "docType es obligatorio")
    @Size(max = 20, message = "docType no puede superar los 20 caracteres")
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
    private String email;

    @Size(max = 50, message = "phone no puede superar los 50 caracteres")
    private String phone;

    @Size(max = 255, message = "address no puede superar los 255 caracteres")
    private String address;

    private Ubigeo ubigeo;

    @Size(max = 255, message = "password no puede superar los 255 caracteres")
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
