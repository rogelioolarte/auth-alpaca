package com.alpaca.exception;

import com.alpaca.dto.response.ErrorResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;

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
@Slf4j
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
     * Handles data integrity violations (e.g., unique constraint or duplicate key) by returning
     * HTTP 409 Conflict.
     *
     * @param ex the data integrity exception
     * @param req the current web request for context
     * @return a 409 response indicating a resource conflict
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, WebRequest req) {
        return buildResponse(
                HttpStatus.CONFLICT, "A resource with the same identifier already exists.", req);
    }

    /**
     * Handles cases where a query or update expected a result but found none (e.g., delete by
     * non-existent ID), returning HTTP 404 Not Found.
     *
     * @param ex the empty-result exception
     * @param req the current web request for context
     * @return a 404 response with the exception's message
     */
    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<ErrorResponseDTO> handleEmptyResultException(
            EmptyResultDataAccessException ex, WebRequest req) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    /**
     * Handles any uncaught exception not handled by more specific exception handlers.
     *
     * <p>Logs the full error at ERROR level and returns a generic HTTP 500 response, avoiding
     * leakage of internal implementation details to the client.
     *
     * @param ex the unexpected exception
     * @param req the current web request for context
     * @return a 500 response with a generic error message wrapped in an {@link ErrorResponseDTO}
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGlobalException(Exception ex, WebRequest req) {
        log.error("Unexpected error occurred: ", ex);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", req);
    }

    /**
     * Handles {@link ResponseStatusException} by forwarding its status and reason into a structured
     * error payload.
     *
     * <p>This is the catch-all handler for all typed exception subclasses ({@link
     * BadRequestException}, {@link NotFoundException}, etc.) since they all extend {@link
     * ResponseStatusException}.
     *
     * @param exception the exception containing a predefined HTTP status and reason
     * @param webRequest the current web request for context
     * @return a response with the appropriate HTTP status and an {@link ErrorResponseDTO}
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

    /**
     * Handles rate-limit violations by returning HTTP 429 with a {@code Retry-After} header so the
     * client knows when to retry.
     *
     * @param ex the rate-limit exception carrying the retry delay
     * @return a 429 response with the {@code Retry-After} header
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Object> handleRateLimit(RateLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body("Too many requests, try again later");
    }

    private ResponseEntity<ErrorResponseDTO> buildResponse(
            HttpStatus status, String message, WebRequest req) {
        ErrorResponseDTO error =
                new ErrorResponseDTO(req.getDescription(false), message, LocalDateTime.now());
        return new ResponseEntity<>(error, status);
    }
}
