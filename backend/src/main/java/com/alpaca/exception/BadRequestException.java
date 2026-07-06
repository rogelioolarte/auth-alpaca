package com.alpaca.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Thrown when the client sends a malformed or semantically invalid request.
 *
 * <p>Translates to HTTP 400 Bad Request. Use this exception to reject input that fails validation,
 * parsing, or business-rule checks at the API boundary.
 */
public class BadRequestException extends ResponseStatusException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
