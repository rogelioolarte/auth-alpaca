package com.alpaca.exception;

import com.alpaca.dto.response.ErrorResponseDTO;
import java.time.LocalDateTime;
import java.util.HashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

/**
 * Centralized exception handler for REST controllers using {@link RestControllerAdvice}.
 *
 * <p>This class intercepts exceptions thrown by controller methods and converts them into
 * meaningful HTTP responses with structured payloads.
 *
 * <p>Handled exception types include:
 *
 * <ul>
 *   <li>{@link SpecificException}: returns a status code and reason from the exception.
 *   <li>{@link MethodArgumentNotValidException}: captures validation errors and returns a map of
 *       field names to error messages.
 *   <li>Generic {@link Exception}: wraps unexpected errors in an {@link ErrorResponseDTO} with
 *       timestamp and request description, returning HTTP 500.
 *   <li>{@link ResponseStatusException}: translates the exception into an error response using the
 *       exception’s status and reason.
 * </ul>
 *
 * <p>This approach centralizes error logic, improves API consistency, and cleanly separates concern
 * across controllers. It reflects recommended practices for production-grade REST APIs using Spring
 * Boot. {@link RestControllerAdvice} automatically applies to all controllers globally.
 *
 * @see RestControllerAdvice
 * @see ExceptionHandler
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles custom SpecificException and returns the configured status and reason.
     *
     * @param specificException the thrown specific exception
     * @return a ResponseEntity containing the reason and matching status code
     */
    @ExceptionHandler(value = {SpecificException.class})
    public ResponseEntity<String> handleSpecificException(SpecificException specificException) {
        return ResponseEntity.status(specificException.getStatusCode())
                .body(specificException.getReason());
    }

    /**
     * Captures validation errors thrown due to method argument validation failures. Constructs a
     * field-to-error message map for easy client-side display.
     *
     * @param exception the validation exception containing field errors
     * @return a BAD_REQUEST response containing a map of field-specific error messages
     */
    @ExceptionHandler(value = {MethodArgumentNotValidException.class})
    public ResponseEntity<HashMap<String, String>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception) {
        HashMap<String, String> errors = new HashMap<>();
        for (FieldError fieldError : exception.getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Catches all unhandled exceptions and returns a structured error response with details
     * including the request path, error message, and timestamp.
     *
     * @param exception the uncaught exception
     * @param webRequest the current web request to extract context
     * @return an INTERNAL_SERVER_ERROR response with an {@link ErrorResponseDTO}
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGlobalException(
            Exception exception, WebRequest webRequest) {
        return new ResponseEntity<>(
                new ErrorResponseDTO(
                        webRequest.getDescription(false),
                        exception.getMessage(),
                        LocalDateTime.now()),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handles ResponseStatusException by passing through its status and reason into a structured
     * error payload.
     *
     * @param exception the exception containing a predefined HTTP status
     * @param webRequest the current web request for context
     * @return response with the appropriate HTTP status and an {@link ErrorResponseDTO}
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDTO> handleResponseStatusException(
            ResponseStatusException exception, WebRequest webRequest) {
        return new ResponseEntity<>(
                new ErrorResponseDTO(
                        webRequest.getDescription(false),
                        exception.getReason(),
                        LocalDateTime.now()),
                HttpStatus.valueOf(exception.getStatusCode().value()));
    }
}
