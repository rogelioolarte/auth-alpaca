package com.alpaca.dto.response;

import java.time.LocalDateTime;

/**
 * Standard error envelope returned by the API on failures.
 *
 * <p>Carries the failing endpoint path, a human-readable message, and the server timestamp so
 * callers can correlate responses with server-side logs.
 */
public record ErrorResponseDTO(String apiPath, String message, LocalDateTime errorTime) {}
