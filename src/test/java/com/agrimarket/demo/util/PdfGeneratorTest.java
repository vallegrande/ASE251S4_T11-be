package com.agrimarket.demo.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfGeneratorTest {

    @Test
    void rendersHtmlTemplateAndEscapesReportContent() throws IOException {
        byte[] pdf = PdfGenerator.write(
                "Reporte <Clientes>",
                new String[] {"Nombre & apellido"},
                Collections.singletonList(new String[] {"Ana <admin@example.com>"}));

        try (var document = PDDocument.load(pdf)) {
            String text = new PDFTextStripper().getText(document);

            assertTrue(text.contains("Reporte <Clientes>"));
            assertTrue(text.contains("Nombre & apellido"));
            assertTrue(text.contains("Ana <admin@example.com>"));
            assertTrue(text.contains("1 registro"));
        }
    }
}
