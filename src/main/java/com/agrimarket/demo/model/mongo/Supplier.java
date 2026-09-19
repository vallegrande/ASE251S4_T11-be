package com.agrimarket.demo.model.mongo;

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

    private String supplierCode;
    private String businessName;
    private String docType;
    private String docNumber;
    private String phone;
    private String email;
    private String address;

    private Ubigeo ubigeo;

    private String contactPerson;

    @Builder.Default
    private Boolean isActive = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime restoredAt;
}
