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

    public SpecificException(Integer status, String reason) {
        super(HttpStatusCode.valueOf(status), reason);
    }
}
