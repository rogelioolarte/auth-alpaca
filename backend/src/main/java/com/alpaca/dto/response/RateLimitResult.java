package com.alpaca.dto.response;

/**
 * Outcome of a rate-limit check.
 *
 * <p>When {@code allowed} is {@code false}, {@code retryAfterSeconds} indicates how long the caller
 * should wait before retrying the request.
 *
 * @param allowed {@code true} if the request is within the rate limit, {@code false} if throttled
 * @param retryAfterSeconds the number of seconds the caller should wait before retrying; meaningful
 *     only when {@code allowed} is {@code false}
 */
public record RateLimitResult(boolean allowed, long retryAfterSeconds) {}
