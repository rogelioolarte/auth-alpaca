package com.alpaca.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Thrown when a client has exceeded its allowed request rate and should back off.
 *
 * <p>Translates to HTTP 429 Too Many Requests. The {@link #retryAfterSeconds} value tells the
 * client how long to wait before issuing a new request, and is also set as the {@code Retry-After}
 * response header by {@link GlobalExceptionHandler}.
 */
@Getter
public class RateLimitExceededException extends ResponseStatusException {

    private final long retryAfterSeconds;

    /**
     * Constructs a new RateLimitExceededException with the suggested retry delay.
     *
     * @param retryAfterSeconds the number of seconds the client should wait before retrying
     */
    public RateLimitExceededException(long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
