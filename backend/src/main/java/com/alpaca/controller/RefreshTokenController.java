package com.alpaca.controller;

import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.exception.RateLimitExceededException;
import com.alpaca.dto.response.RateLimitResult;
import com.alpaca.security.ratelimit.IPRateLimit;
import com.alpaca.service.IRefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class RefreshTokenController {

    private final IRefreshTokenService service;
    private final IPRateLimit rateLimit;

    @PostMapping("/rotate")
    public ResponseEntity<AuthResponseDTO> rotateRefreshToken(
            @RequestHeader("X-Refresh-Token") String refreshToken,
            @RequestHeader("X-Client-Id") String clientId,
            @RequestHeader(value = "User-Agent") String userAgent,
            HttpServletRequest request) {

        String clientIp = extractClientIP(request);
        RateLimitResult result = rateLimit.check(clientIp);
        if (!result.allowed()) {
            throw new RateLimitExceededException(result.retryAfterSeconds());
        }

        return ResponseEntity.ok(
                service.rotateRefreshToken(refreshToken, clientId, userAgent, clientIp));
    }

    private String extractClientIP(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
