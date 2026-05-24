package com.alpaca.dto.response;

import java.time.Instant;
import java.util.UUID;

public record SessionResponseDTO(
        UUID id, Instant lastSeenAt, String ipAddress, String userAgent, String clientId) {}
