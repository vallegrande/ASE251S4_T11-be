package pe.edu.vallegrande.vgmsagrimarket.security;

public record IssuedToken(String value, String role, String userId, String email) {
}
