package com.agrimarket.demo.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lee archivos tabulares (.xlsx, .xls, .csv) y devuelve las filas como mapas
 * "encabezado -> valor". Los encabezados se normalizan (minusculas, sin espacios
 * ni guiones bajos) para tolerar variaciones en la plantilla.
 */
public final class TabularFileReader {

    private TabularFileReader() {
    }

    public static List<Map<String, String>> read(byte[] content, String filename) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("El archivo esta vacio");
        }
        String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        try {
            if (name.endsWith(".csv")) {
                return readCsv(content);
            }
            if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
                return readExcel(content);
            }
            throw new IllegalArgumentException("Formato no soportado. Use un archivo .xlsx, .xls o .csv");
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer el archivo: " + e.getMessage(), e);
        }
    }

    private static List<Map<String, String>> readCsv(byte[] content) throws IOException {
        // Detectar BOM y saltarlo si existe
        int offset = 0;
        if (content.length >= 3 && (content[0] & 0xFF) == 0xEF && (content[1] & 0xFF) == 0xBB && (content[2] & 0xFF) == 0xBF) {
            offset = 3;
        }

        String text = new String(content, offset, content.length - offset, StandardCharsets.UTF_8);
        if (text.isBlank()) {
            return List.of();
        }

        // Detectar delimitador (, o ;)
        char delimiter = ',';
        String firstLine = text.split("\n")[0];
        long commas = firstLine.chars().filter(ch -> ch == ',').count();
        long semicolons = firstLine.chars().filter(ch -> ch == ';').count();
        if (semicolons > commas) {
            delimiter = ';';
        }

        try (Reader reader = new StringReader(text);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setDelimiter(delimiter)
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreSurroundingSpaces(true)
                     .setIgnoreEmptyLines(true)
                     .build()
                     .parse(reader)) {
            List<Map<String, String>> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                // Si el record no tiene el mismo numero de columnas que el header, toMap() puede fallar o ser incompleto
                Map<String, String> raw;
                try {
                    raw = record.toMap();
                } catch (IllegalStateException e) {
                    continue; // Fila malformada
                }
                Map<String, String> normalized = normalize(raw);
                if (!isEmptyRow(normalized)) {
                    rows.add(normalized);
                }
            }
            return rows;
        }
    }

    private static List<Map<String, String>> readExcel(byte[] content) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                return List.of();
            }
            List<String> headers = new ArrayList<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                headers.add(cell == null ? "" : formatter.formatCellValue(cell));
            }
            List<Map<String, String>> rows = new ArrayList<>();
            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                Map<String, String> raw = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = row.getCell(i);
                    raw.put(headers.get(i), cell == null ? "" : formatter.formatCellValue(cell));
                }
                Map<String, String> normalized = normalize(raw);
                if (!isEmptyRow(normalized)) {
                    rows.add(normalized);
                }
            }
            return rows;
        }
    }

    private static Map<String, String> normalize(Map<String, String> raw) {
        Map<String, String> normalized = new LinkedHashMap<>();
        raw.forEach((key, value) -> normalized.put(normalizeKey(key), value == null ? "" : value.trim()));
        return normalized;
    }

    private static String normalizeKey(String key) {
        if (key == null) {
            return "";
        }
        return key.trim().toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "");
    }

    private static boolean isEmptyRow(Map<String, String> row) {
        return row.values().stream().allMatch(String::isEmpty);
    }
}
