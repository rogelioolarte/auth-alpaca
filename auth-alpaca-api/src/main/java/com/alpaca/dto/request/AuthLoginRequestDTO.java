package com.alpaca.dto.request;

/**
 * Captures a full login attempt including HTTP context — client ID, user-agent, and originating IP
 * — for session tracking and audit logging beyond the bare credentials carried by {@link
 * AuthRequestDTO}.
 *
 * @param email the user's email address; must not be blank
 * @param password the user's password; must not be blank
 * @param clientId the OAuth2 client identifier for this login
 * @param userAgent the HTTP {@code User-Agent} header value from the login request
 * @param clientIp the originating IP address of the login request
 */
public record AuthLoginRequestDTO(
        String email, String password, String clientId, String userAgent, String clientIp) {}
