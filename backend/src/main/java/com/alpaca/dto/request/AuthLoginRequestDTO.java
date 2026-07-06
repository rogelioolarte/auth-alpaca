package com.alpaca.dto.request;

/**
 * Captures a full login attempt including HTTP context — client ID, user-agent, and originating IP
 * — for session tracking and audit logging beyond the bare credentials carried by {@link
 * AuthRequestDTO}.
 */
public record AuthLoginRequestDTO(
        String email, String password, String clientId, String userAgent, String clientIp) {}
