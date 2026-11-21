package pe.edu.vallegrande.agrimarket.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void hidesInternalExceptionDetailsFromResponse() {
        StepVerifier.create(handler.handleGenericException(new IllegalStateException("database password")))
                .assertNext(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                    assertEquals(
                            "Ocurrió un error interno. Inténtelo nuevamente.",
                            response.getBody().get("message"));
                })
                .verifyComplete();
    }
}
