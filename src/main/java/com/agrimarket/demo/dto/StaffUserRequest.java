package com.agrimarket.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StaffUserRequest {

    @NotBlank
    @Email
    private String email;

    @Size(min = 12, max = 72)
    private String password;

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @NotBlank
    @Pattern(regexp = "ADMIN|VENDEDOR", message = "role debe ser ADMIN o VENDEDOR")
    private String role;

    private Boolean isActive;
}
