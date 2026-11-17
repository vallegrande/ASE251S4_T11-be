package pe.edu.vallegrande.agrimarket.security;

public record IssuedToken(String value, String role, String userId, String email) {
}
