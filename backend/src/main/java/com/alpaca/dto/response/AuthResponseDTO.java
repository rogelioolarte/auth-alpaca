package com.alpaca.dto.response;

/**
 * Contains the JWT pair returned after successful authentication.
 *
 * <p>The {@code accessToken} is short-lived for API authorization; the {@code refreshToken} is
 * long-lived and used to obtain a new access token without requiring the user to re-authenticate.
 *
 * @param accessToken the short-lived JWT access token for API authorization
 * @param refreshToken the long-lived JWT refresh token for obtaining new access tokens
 */
public record AuthResponseDTO(String accessToken, String refreshToken) {}
