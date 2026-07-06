package com.alpaca.dto.response;

/**
 * Outcome of a rate-limit check.
 *
 * <p>When {@code allowed} is {@code false}, {@code retryAfterSeconds} indicates how long the caller
 * should wait before retrying the request.
 */
public record RateLimitResult(boolean allowed, long retryAfterSeconds) {}
