package com.agrimarket.demo.security;

public record IssuedToken(String value, String role, String userId, String email) {
}
