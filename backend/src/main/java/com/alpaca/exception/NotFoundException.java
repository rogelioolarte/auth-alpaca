package com.alpaca.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Thrown when a requested resource does not exist.
 *
 * <p>Translates to HTTP 404 Not Found. Use this exception when a lookup by identifier or query
 * yields no result — for entities, profiles, sessions, or any domain resource.
 */
public class NotFoundException extends ResponseStatusException {

    /**
     * Constructs a new NotFoundException with the given detail message.
     *
     * @param message the detail message describing the resource that was not found
     */
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
