package pe.edu.vallegrande.agrimarket.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TabularFileReaderTest {

    @Test
    void rejectsCsvRowsWithDifferentColumnCount() {
        byte[] content = "name,email\nAna,ana@example.com,extra".getBytes(StandardCharsets.UTF_8);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> TabularFileReader.read(content, "customers.csv"));

        assertTrue(exception.getMessage().contains("cantidad de columnas"));
    }
}
