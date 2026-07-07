package com.alpaca.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Active session information returned by the API, including the last-seen timestamp, IP address,
 * user-agent, and client identifier for audit and session-management purposes.
 *
 * @param id the session's unique identifier
 * @param lastSeenAt the timestamp of the last request made within this session
 * @param ipAddress the IP address associated with the session
 * @param userAgent the HTTP {@code User-Agent} header from the session
 * @param clientId the OAuth2 client identifier associated with the session
 */
public record SessionResponseDTO(
        UUID id, Instant lastSeenAt, String ipAddress, String userAgent, String clientId) {}
