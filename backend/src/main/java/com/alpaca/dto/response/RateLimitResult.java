package com.alpaca.dto.response;

public record RateLimitResult(boolean allowed, long retryAfterSeconds) {}
