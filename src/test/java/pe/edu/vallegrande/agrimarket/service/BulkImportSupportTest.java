package pe.edu.vallegrande.agrimarket.service;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BulkImportSupportTest {

    @Test
    void reportsInvalidRowsAndKeepsSourceRowNumbers() {
        byte[] content = "name\ninvalid\nsave-error\nvalid\n".getBytes(StandardCharsets.UTF_8);

        StepVerifier.create(BulkImportSupport.importRows(
                        content,
                        "items.csv",
                new BulkImportSupport.ImportPlan<>(
                        this::mapName,
                        name -> "save-error".equals(name)
                                ? Mono.error(new IllegalStateException("Save failed"))
                                : Mono.just(name))))
                .assertNext(result -> {
                    assertEquals(1, result.getImported());
                    assertEquals(2, result.getFailed());
                    assertEquals(3, result.getTotal());
                    assertEquals(2, result.getErrors().get(0).getRow());
                    assertEquals(3, result.getErrors().get(1).getRow());
                })
                .verifyComplete();
    }

    private String mapName(Map<String, String> row) {
        String name = row.get("name");
        if ("invalid".equals(name)) {
            throw new IllegalArgumentException("Invalid row");
        }
        return name;
    }
}
