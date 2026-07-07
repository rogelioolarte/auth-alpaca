package com.alpaca.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Thrown when authentication is required but missing or invalid.
 *
 * <p>Translates to HTTP 401 Unauthorized. Use this exception when a request lacks valid
 * credentials, has an expired or malformed token, or the identity cannot be established.
 */
public class UnauthorizedException extends ResponseStatusException {

    /**
     * Constructs a new UnauthorizedException with the given detail message.
     *
     * @param message the detail message explaining why authentication failed
     */
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
