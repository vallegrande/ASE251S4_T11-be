package com.agrimarket.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CustomerRegistrationResponse {
    private Long customerId;
    private String email;
    private String firstName;
    private String lastName;
}
