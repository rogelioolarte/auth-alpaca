package com.alpaca.dto.response;

/**
 * Contains the JWT pair returned after successful authentication.
 *
 * <p>The {@code accessToken} is short-lived for API authorization; the {@code refreshToken} is
 * long-lived and used to obtain a new access token without requiring the user to re-authenticate.
 */
public record AuthResponseDTO(String accessToken, String refreshToken) {}
