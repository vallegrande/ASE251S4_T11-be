package pe.edu.vallegrande.agrimarket.security;

import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {

    public static final int MIN_PASSWORD_LENGTH = 12;
    public static final int MAX_BCRYPT_PASSWORD_BYTES = 72;
    public static final String PASSWORD_REQUIREMENTS_ERROR =
            "password debe tener al menos 12 caracteres y no superar 72 bytes en UTF-8";
    public static final String PASSWORD_TOO_LONG_ERROR =
            "password no puede superar 72 bytes en UTF-8";

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        return password != null
                && password.length() >= MIN_PASSWORD_LENGTH
                && password.getBytes(StandardCharsets.UTF_8).length <= MAX_BCRYPT_PASSWORD_BYTES;
    }
}
