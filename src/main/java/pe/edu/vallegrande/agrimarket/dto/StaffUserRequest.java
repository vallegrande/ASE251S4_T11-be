package pe.edu.vallegrande.agrimarket.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import pe.edu.vallegrande.agrimarket.security.PasswordPolicy;
import pe.edu.vallegrande.agrimarket.validation.ValidationLimits;

@Data
public class StaffUserRequest {

    @NotBlank
    @Email
    private String email;

    @Size(min = PasswordPolicy.MIN_PASSWORD_LENGTH, max = PasswordPolicy.MAX_BCRYPT_PASSWORD_BYTES)
    private String password;

    @NotBlank
    @Size(max = ValidationLimits.MAX_NAME_LENGTH)
    private String firstName;

    @NotBlank
    @Size(max = ValidationLimits.MAX_NAME_LENGTH)
    private String lastName;

    @NotBlank
    @Pattern(regexp = "ADMIN|VENDEDOR", message = "role debe ser ADMIN o VENDEDOR")
    private String role;

    private Boolean isActive;
}
