package com.alpaca.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
