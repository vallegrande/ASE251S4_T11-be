package pe.edu.vallegrande.agrimarket.config;

import pe.edu.vallegrande.agrimarket.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String GENERIC_ERROR_MESSAGE = "Ocurrió un error interno. Inténtelo nuevamente.";

    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("message", ex.getMessage());
        body.put("timestamp", LocalDateTime.now());
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(body));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleValidationException(WebExchangeBindException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("timestamp", LocalDateTime.now());

        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::mapFieldError)
                .toList();
        body.put("errors", errors);

        if (!errors.isEmpty()) {
            body.put("message", "Error de validacion: " + errors.size() + " campo(s) con errores");
        }

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleGenericException(Exception ex) {
        LOGGER.error("Unhandled exception while processing request", ex);
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("message", GENERIC_ERROR_MESSAGE);
        body.put("timestamp", LocalDateTime.now());
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body));
    }

    private Map<String, String> mapFieldError(FieldError fieldError) {
        Map<String, String> error = new HashMap<>();
        error.put("field", fieldError.getField());
        error.put("message", fieldError.getDefaultMessage());
        return error;
    }
}
