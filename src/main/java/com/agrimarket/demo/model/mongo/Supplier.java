package com.agrimarket.demo.model.mongo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "suppliers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Supplier {

    public static final String SEQUENCE_NAME = "suppliers_sequence";

    @Id
    private Long id;

    @NotBlank(message = "businessName es obligatorio")
    @Size(max = 150, message = "businessName no puede superar 150 caracteres")
    private String businessName;

    @Size(max = 20, message = "docType no puede superar 20 caracteres")
    private String docType;

    @Size(max = 20, message = "docNumber no puede superar 20 caracteres")
    private String docNumber;

    @Size(max = 50, message = "phone no puede superar 50 caracteres")
    private String phone;

    @Email(message = "email debe ser válido")
    @Size(max = 255, message = "email no puede superar 255 caracteres")
    private String email;

    @Size(max = 255, message = "address no puede superar 255 caracteres")
    private String address;

    private Ubigeo ubigeo;

    @Size(max = 100, message = "contactPerson no puede superar 100 caracteres")
    private String contactPerson;

    @Builder.Default
    private Boolean isActive = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime restoredAt;
}
