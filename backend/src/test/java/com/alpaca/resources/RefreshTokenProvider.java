package com.alpaca.resources;

import com.alpaca.entity.RefreshToken;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RefreshTokenProvider {

    public static RefreshToken singleEntity() {
        RefreshToken token = new RefreshToken();
        token.setId(UUID.fromString("019e0f51-038c-7f79-96b5-be2e0b329111"));
        token.setUser(UserProvider.singleEntity());
        token.setTokenHash("hashed_refresh_token_value");
        token.setTokenJti(UUID.fromString("2632eb79-63a4-4213-b905-0ad176f0004b"));
        token.setFamilyId(UUID.fromString("3632eb79-63a4-4213-b905-0ad176f0004c"));
        token.setRevoked(false);
        token.setRevokedAt(null);
        token.setExpiresAt(Instant.parse("2027-01-02T10:00:00Z"));
        token.setLastUsedAt(Instant.parse("2024-01-01T10:30:00Z"));
        token.setClientId("web-client");
        token.setIpAddress("127.0.0.1");
        token.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        return token;
    }

    public static RefreshToken alternativeEntity() {
        RefreshToken token = new RefreshToken();
        token.setId(UUID.fromString("019e0f52-a9da-7560-a196-359bbcf6571c"));
        token.setUser(UserProvider.alternativeEntity());
        token.setTokenHash("alternative_hashed_refresh_token");
        token.setTokenJti(UUID.fromString("a82a1001-b033-48f6-b2e6-6b327f0a61ec"));
        token.setFamilyId(UUID.fromString("b82a1001-b033-48f6-b2e6-6b327f0a61ed"));
        token.setRevoked(false);
        token.setRevokedAt(null);
        token.setExpiresAt(Instant.parse("2024-01-03T11:00:00Z"));
        token.setLastUsedAt(Instant.parse("2024-01-02T11:15:00Z"));
        token.setClientId("mobile-client");
        token.setIpAddress("192.168.1.1");
        token.setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36");
        return token;
    }

    public static RefreshToken singleTemplate() {
        return RefreshToken.builder()
                .tokenHash("hashed_refresh_token_value")
                .tokenJti(UUID.fromString("2632eb79-63a4-4213-b905-0ad176f0004b"))
                .familyId(UUID.fromString("3632eb79-63a4-4213-b905-0ad176f0004c"))
                .clientId("web-client-0")
                .ipAddress("127.0.0.1")
                .userAgent("Mozilla/5.0")
                .expiresAt(Instant.parse("2027-01-02T10:00:00Z"))
                .lastUsedAt(Instant.parse("2024-01-01T10:30:00Z"))
                .revoked(false)
                .revokedAt(null)
                .replacedBy(null)
                .build();
    }

    public static RefreshToken alternativeTemplate() {
        return RefreshToken.builder()
                .tokenHash("hashed_refresh_token_value_alt")
                .tokenJti(UUID.fromString("019e0fea-8342-7663-be6e-9535953577e5"))
                .familyId(UUID.fromString("019e0feb-31df-778c-a3ab-144dd876e1ed"))
                .clientId("web-client-1")
                .ipAddress("127.0.0.1")
                .userAgent("Mozilla/5.0")
                .expiresAt(Instant.parse("2027-01-02T10:00:00Z"))
                .lastUsedAt(Instant.parse("2024-01-01T10:30:00Z"))
                .revoked(false)
                .revokedAt(null)
                .replacedBy(null)
                .build();
    }

    public static List<RefreshToken> listEntities() {
        return new ArrayList<>(List.of(singleEntity(), alternativeEntity()));
    }
}
