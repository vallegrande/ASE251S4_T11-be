package pe.edu.vallegrande.agrimarket.util;

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
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TabularFileReader {

    private static final int UTF8_BOM_LENGTH = 3;
    private static final int UTF8_BOM_BYTE_1 = 0xEF;
    private static final int UTF8_BOM_BYTE_2 = 0xBB;
    private static final int UTF8_BOM_BYTE_3 = 0xBF;
    private static final char COMMA_DELIMITER = ',';
    private static final char SEMICOLON_DELIMITER = ';';
    private static final int FIRST_WORKSHEET_INDEX = 0;
    private static final int HEADER_ROW_OFFSET = 1;

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
        String text = decodeCsvContent(content);
        if (text.isBlank()) {
            return List.of();
        }
        return parseCsvRows(text, detectDelimiter(text));
    }

    private static String decodeCsvContent(byte[] content) {
        int offset = hasUtf8Bom(content) ? UTF8_BOM_LENGTH : 0;
        return new String(content, offset, content.length - offset, StandardCharsets.UTF_8);
    }

    private static boolean hasUtf8Bom(byte[] content) {
        return content.length >= UTF8_BOM_LENGTH
                && (content[0] & 0xFF) == UTF8_BOM_BYTE_1
                && (content[1] & 0xFF) == UTF8_BOM_BYTE_2
                && (content[2] & 0xFF) == UTF8_BOM_BYTE_3;
    }

    private static char detectDelimiter(String text) {
        char delimiter = COMMA_DELIMITER;
        String firstLine = text.split("\n")[0];
        long commas = firstLine.chars().filter(ch -> ch == COMMA_DELIMITER).count();
        long semicolons = firstLine.chars().filter(ch -> ch == SEMICOLON_DELIMITER).count();
        if (semicolons > commas) {
            delimiter = SEMICOLON_DELIMITER;
        }
        return delimiter;
    }

    private static List<Map<String, String>> parseCsvRows(String text, char delimiter) throws IOException {
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
            Sheet sheet = workbook.getSheetAt(FIRST_WORKSHEET_INDEX);
            DataFormatter formatter = new DataFormatter();
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                return List.of();
            }
            List<String> headers = readExcelHeaders(headerRow, formatter);
            return readExcelRows(sheet, headers, formatter);
        }
    }

    private static List<String> readExcelHeaders(Row headerRow, DataFormatter formatter) {
        List<String> headers = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < headerRow.getLastCellNum(); columnIndex++) {
            Cell cell = headerRow.getCell(columnIndex);
            headers.add(cell == null ? "" : formatter.formatCellValue(cell));
        }
        return headers;
    }

    private static List<Map<String, String>> readExcelRows(
            Sheet sheet, List<String> headers, DataFormatter formatter) {
        List<Map<String, String>> rows = new ArrayList<>();
        for (int rowIndex = sheet.getFirstRowNum() + HEADER_ROW_OFFSET;
                rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            Map<String, String> raw = new LinkedHashMap<>();
            for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
                Cell cell = row.getCell(columnIndex);
                raw.put(headers.get(columnIndex), cell == null ? "" : formatter.formatCellValue(cell));
            }
            Map<String, String> normalized = normalize(raw);
            if (!isEmptyRow(normalized)) {
                rows.add(normalized);
            }
        }
        return rows;
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
