package com.agrimarket.demo.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * Utilidades para leer y convertir valores de una fila importada (mapa encabezado -> valor).
 */
public final class ValueParser {

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"));

    private ValueParser() {
    }

    /** Devuelve el primer valor no vacio de las claves indicadas, o null. */
    public static String value(Map<String, String> row, String... keys) {
        for (String key : keys) {
            String value = row.get(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    /** Igual que {@link #value} pero lanza excepcion si esta vacio. */
    public static String required(Map<String, String> row, String field, String... keys) {
        String value = value(row, keys);
        if (value == null) {
            throw new IllegalArgumentException("Falta el campo obligatorio: " + field);
        }
        return value;
    }

    public static Boolean parseBoolean(String value, Boolean defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "true", "1", "si", "sí", "yes", "y", "activo", "activa" -> Boolean.TRUE;
            case "false", "0", "no", "n", "inactivo", "inactiva" -> Boolean.FALSE;
            default -> throw new IllegalArgumentException("Valor booleano invalido: " + value);
        };
    }

    public static Integer parseInteger(String value, Integer defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Numero entero invalido: " + value);
        }
    }

    public static BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Numero decimal invalido: " + value);
        }
    }

    public static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(trimmed, format);
            } catch (DateTimeParseException ignored) {
                // se prueba con el siguiente formato
            }
        }
        throw new IllegalArgumentException("Fecha invalida (use yyyy-MM-dd): " + value);
    }
}
