package com.alpaca.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Thrown when an unexpected server-side failure prevents request processing.
 *
 * <p>Translates to HTTP 500 Internal Server Error. Use this exception for unrecoverable runtime
 * errors that are not attributable to client input — infrastructure failures, null dereferences, or
 * unanticipated states.
 */
public class InternalErrorException extends ResponseStatusException {

    /**
     * Constructs a new InternalErrorException with the given detail message.
     *
     * @param message the detail message describing the internal error
     */
    public InternalErrorException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
