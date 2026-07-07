package com.alpaca.exception;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

/**
 * Thrown when a specific HTTP status code and reason must be returned dynamically.
 *
 * <p>Unlike the typed exception subclasses (e.g., {@link BadRequestException}, {@link
 * NotFoundException}), this exception accepts a numeric status code at construction time. Use it
 * for error scenarios where the status cannot be determined statically or does not map to a
 * dedicated exception class.
 */
public class SpecificException extends ResponseStatusException {

    /**
     * Constructs a new SpecificException with a dynamic HTTP status and reason.
     *
     * @param status the HTTP status code to return (e.g., 400, 409, 422)
     * @param reason the detail message explaining the error
     */
    public SpecificException(Integer status, String reason) {
        super(HttpStatusCode.valueOf(status), reason);
    }
}
