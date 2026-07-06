package com.alpaca.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Thrown when a user attempts to start a new session but has already reached the maximum number of
 * concurrent sessions allowed.
 *
 * <p>Translates to HTTP 403 Forbidden. The {@link #getMaxOfSessions()} value allows the caller or
 * error handler to inform the user of the limit.
 */
@Getter
public class ExceededSessionsException extends ResponseStatusException {

    private final long maxOfSessions;

    public ExceededSessionsException(long maxOfSessions) {
        super(
                HttpStatus.FORBIDDEN,
                String.format("Maximum number of active sessions %s exceeded", maxOfSessions));
        this.maxOfSessions = maxOfSessions;
    }
}
