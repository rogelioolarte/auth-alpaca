package com.alpaca.model;

public record RateLimitResult(boolean allowed, long retryAfterSeconds) {}
