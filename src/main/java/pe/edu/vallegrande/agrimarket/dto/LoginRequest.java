package pe.edu.vallegrande.agrimarket.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import pe.edu.vallegrande.agrimarket.security.PasswordPolicy;
import pe.edu.vallegrande.agrimarket.validation.ValidationLimits;

@Data
public class LoginRequest {
    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(max = PasswordPolicy.MAX_BCRYPT_PASSWORD_BYTES)
    private String password;

    @Size(max = ValidationLimits.MAX_SESSION_TOKEN_LENGTH)
    private String sessionToken;
}
