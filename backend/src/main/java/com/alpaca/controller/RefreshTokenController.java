package com.alpaca.controller;

import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.dto.response.RateLimitResult;
import com.alpaca.exception.RateLimitExceededException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.security.ratelimit.IPRateLimit;
import com.alpaca.service.IRefreshTokenService;
import com.alpaca.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for refresh token rotation at {@code /api/auth}.
 *
 * <p>Provides the {@code POST /rotate} endpoint for exchanging an expiring refresh token for a new
 * access and refresh token pair (token rotation). Requests are IP-rate-limited via {@link
 * IPRateLimit} to prevent abuse. Requires authentication — unauthenticated requests return HTTP
 * 401.
 *
 * @see IRefreshTokenService
 * @see IPRateLimit
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RefreshTokenController {

    private final IRefreshTokenService service;
    private final IPRateLimit rateLimit;

    /**
     * Rotates a refresh token, issuing a new access token and refresh token pair.
     *
     * <p>The previous refresh token is revoked and cannot be reused. This endpoint is
     * IP-rate-limited — exceeding the limit produces HTTP 429 (see {@link
     * RateLimitExceededException}).
     *
     * @param refreshToken the current refresh token, provided via {@code X-Refresh-Token} header
     * @param clientId the client identifier, provided via {@code X-Client-Id} header
     * @param userAgent the user agent string, provided via {@code User-Agent} header
     * @param user the currently authenticated user; if {@code null} the request is rejected
     * @param request the HTTP servlet request (used for client IP extraction and rate limiting)
     * @return {@link ResponseEntity} containing a new {@link AuthResponseDTO} with status {@link
     *     HttpStatus#OK}, or {@link HttpStatus#UNAUTHORIZED} if not authenticated
     * @throws RateLimitExceededException when the client IP exceeds the rate limit
     */
    @PostMapping("/rotate")
    public ResponseEntity<AuthResponseDTO> rotateRefreshToken(
            @RequestHeader("X-Refresh-Token") String refreshToken,
            @RequestHeader("X-Client-Id") String clientId,
            @RequestHeader("User-Agent") String userAgent,
            @AuthenticationPrincipal UserPrincipal user,
            HttpServletRequest request) {

        String clientIp = Utils.extractClientIP(request);
        RateLimitResult result = rateLimit.check(clientIp);
        if (!result.allowed()) {
            throw new RateLimitExceededException(result.retryAfterSeconds());
        }
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        return ResponseEntity.ok(
                service.rotateRefreshToken(refreshToken, clientId, userAgent, clientIp));
    }
}
