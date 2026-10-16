package com.agrimarket.demo.util;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Genera un PDF (A4 horizontal) en memoria a partir de un titulo, encabezados y filas.
 */
public final class PdfGenerator {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private PdfGenerator() {
    }

    public static byte[] write(String title, String[] headers, List<String[]> rows) {
        Document document = new Document(PageSize.A4.rotate(), 25, 25, 30, 30);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean open = false;
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            open = true;

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15);
            Paragraph heading = new Paragraph(title, titleFont);
            heading.setAlignment(Element.ALIGN_CENTER);
            document.add(heading);

            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Paragraph subtitle = new Paragraph(
                    "Generado: " + LocalDateTime.now().format(TIMESTAMP) + "     Registros: " + rows.size(),
                    subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setGrayFill(0.85f);
                cell.setPadding(4);
                table.addCell(cell);
            }

            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 7);
            for (String[] row : rows) {
                for (int i = 0; i < headers.length; i++) {
                    String value = i < row.length && row[i] != null ? row[i] : "";
                    PdfPCell cell = new PdfPCell(new Phrase(value, bodyFont));
                    cell.setPadding(3);
                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    table.addCell(cell);
                }
            }

            document.add(table);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF: " + e.getMessage(), e);
        } finally {
            if (open) {
                document.close();
            }
        }
        return out.toByteArray();
    }
}
