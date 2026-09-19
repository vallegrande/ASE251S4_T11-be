package com.agrimarket.demo.model.mongo;

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

    private String customerCode;
    private String docType;
    private String docNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;

    private Ubigeo ubigeo;

    private String password;

    @Builder.Default
    private Integer totalVisits = 0;

    private LocalDate lastPurchaseDate;
    private LocalDate customerSince;
    private String notes;

    @Builder.Default
    private Boolean isActive = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime restoredAt;
}
