package com.alpaca.model;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthCode {
    String code;
    String codeChallenge;
    String codeVerifier;
    String redirectUri;
    String clientId;
    String userAgent;
    String clientIp;
    UUID userId;
    Instant expiresAt;

    public AuthCode(
            String code,
            String codeChallenge,
            String clientId,
            String userAgent,
            String clientIp,
            UUID userId,
            String redirectUri) {
        this.code = code;
        this.codeChallenge = codeChallenge;
        this.clientId = clientId;
        this.userAgent = userAgent;
        this.clientIp = clientIp;
        this.userId = userId;
        this.redirectUri = redirectUri;
        this.expiresAt = Instant.now().plusSeconds(60);
    }

    public AuthCode(
            String code,
            String codeVerifier,
            String redirectUri,
            String clientId,
            String userAgent,
            String clientIp) {
        this.code = code;
        this.codeVerifier = codeVerifier;
        this.redirectUri = redirectUri;
        this.clientId = clientId;
        this.userAgent = userAgent;
        this.clientIp = clientIp;
    }
}
