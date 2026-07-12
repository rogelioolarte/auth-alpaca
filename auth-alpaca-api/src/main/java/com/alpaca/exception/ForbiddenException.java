package com.alpaca.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Thrown when the authenticated user lacks permission to access a resource or perform an action.
 *
 * <p>Translates to HTTP 403 Forbidden. Unlike {@link UnauthorizedException}, the caller has been
 * identified but does not hold the required role or authority.
 */
public class ForbiddenException extends ResponseStatusException {
    /**
     * Constructs a new ForbiddenException with the given detail message.
     *
     * @param message the detail message explaining why access was denied
     */
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
