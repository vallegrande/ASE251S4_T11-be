package pe.edu.vallegrande.agrimarket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType;
    private String role;
    private String userId;
    private String email;
}
