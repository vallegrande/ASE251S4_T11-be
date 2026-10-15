package com.agrimarket.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StoreRequest {

    @NotBlank
    @Size(max = 150)
    private String storeName;

    @Size(max = 255)
    private String address;

    @Size(max = 50)
    private String phone;

    @Positive
    private Integer ubigeoId;
}
