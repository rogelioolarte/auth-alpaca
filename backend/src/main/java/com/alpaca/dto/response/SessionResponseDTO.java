package com.alpaca.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Active session information returned by the API, including the last-seen timestamp, IP address,
 * user-agent, and client identifier for audit and session-management purposes.
 */
public record SessionResponseDTO(
        UUID id, Instant lastSeenAt, String ipAddress, String userAgent, String clientId) {}
