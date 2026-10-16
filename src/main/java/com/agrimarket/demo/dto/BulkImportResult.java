package com.agrimarket.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Resumen del resultado de una carga masiva (importacion).
 * No se detiene todo el proceso si una fila falla: se reporta por fila.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkImportResult {

    private int total;
    private int imported;
    private int failed;

    @Builder.Default
    private List<RowError> errors = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowError {
        private int row;
        private String message;
    }
}
