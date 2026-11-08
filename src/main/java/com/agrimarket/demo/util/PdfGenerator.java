package com.agrimarket.demo.util;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Genera reportes PDF desde una plantilla HTML en memoria.
 */
public final class PdfGenerator {

    private static final String TEMPLATE_PATH = "/templates/report.html";
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private PdfGenerator() {
    }

    public static byte[] write(String title, String[] headers, List<String[]> rows) {
        Objects.requireNonNull(title, "title no puede ser null");
        Objects.requireNonNull(headers, "headers no puede ser null");
        Objects.requireNonNull(rows, "rows no puede ser null");
        if (headers.length == 0) {
            throw new IllegalArgumentException("El reporte debe tener al menos una columna");
        }

        String html = loadTemplate()
                .replace("{{title}}", escapeHtml(title))
                .replace("{{generatedAt}}", LocalDateTime.now().format(TIMESTAMP))
                .replace("{{recordCount}}", Integer.toString(rows.size()))
                .replace("{{recordLabel}}", rows.size() == 1 ? "registro" : "registros")
                .replace("{{tableHeaders}}", renderHeaders(headers))
                .replace("{{tableRows}}", renderRows(headers.length, rows));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            new PdfRendererBuilder()
                    .useFastMode()
                    .withHtmlContent(html, null)
                    .toStream(out)
                    .run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF: " + e.getMessage(), e);
        }
    }

    private static String loadTemplate() {
        try (InputStream input = PdfGenerator.class.getResourceAsStream(TEMPLATE_PATH)) {
            if (input == null) {
                throw new IllegalStateException("No se encontro la plantilla PDF: " + TEMPLATE_PATH);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la plantilla PDF: " + e.getMessage(), e);
        }
    }

    private static String renderHeaders(String[] headers) {
        StringBuilder html = new StringBuilder();
        for (String header : headers) {
            html.append("<th>").append(escapeHtml(header)).append("</th>");
        }
        return html.toString();
    }

    private static String renderRows(int columnCount, List<String[]> rows) {
        StringBuilder html = new StringBuilder();
        for (String[] row : rows) {
            html.append("<tr>");
            for (int i = 0; i < columnCount; i++) {
                String value = i < row.length && row[i] != null ? row[i] : "";
                html.append("<td>").append(escapeHtml(value)).append("</td>");
            }
            html.append("</tr>");
        }
        return html.toString();
    }

    private static String escapeHtml(String value) {
        return value == null ? "" : value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
