package com.alpaca.resources;

import com.alpaca.entity.RefreshToken;
import com.alpaca.entity.Session;
import java.time.Instant;
import java.util.*;

public class RefreshTokenProvider {

    public static RefreshToken singleEntity() {
        RefreshToken token = new RefreshToken();
        token.setId(UUID.fromString("1632eb79-63a4-4213-b905-0ad176f0004a"));
        token.setUser(UserProvider.singleEntity());
        token.setTokenHash("hashed_refresh_token_value");
        token.setTokenJti(UUID.fromString("2632eb79-63a4-4213-b905-0ad176f0004b"));
        token.setFamilyId(UUID.fromString("3632eb79-63a4-4213-b905-0ad176f0004c"));
        token.setRevoked(false);
        token.setExpiresAt(Instant.parse("2027-01-02T10:00:00Z"));
        token.setLastUsedAt(Instant.parse("2024-01-01T10:30:00Z"));
        token.setClientId("web-client");
        token.setIpAddress("127.0.0.1");
        token.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        return token;
    }

    public static RefreshToken alternativeEntity() {
        RefreshToken token = new RefreshToken();
        token.setId(UUID.fromString("982a1001-b033-48f6-b2e6-6b327f0a61eb"));
        token.setUser(UserProvider.alternativeEntity());
        token.setTokenHash("alternative_hashed_refresh_token");
        token.setTokenJti(UUID.fromString("a82a1001-b033-48f6-b2e6-6b327f0a61ec"));
        token.setFamilyId(UUID.fromString("b82a1001-b033-48f6-b2e6-6b327f0a61ed"));
        token.setRevoked(false);
        token.setExpiresAt(Instant.parse("2024-01-03T11:00:00Z"));
        token.setLastUsedAt(Instant.parse("2024-01-02T11:15:00Z"));
        token.setClientId("mobile-client");
        token.setIpAddress("192.168.1.1");
        token.setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36");
        return token;
    }

    public static RefreshToken revokedEntity() {
        RefreshToken token = new RefreshToken();
        token.setId(UUID.fromString("019b2092-e007-7671-a9fe-b2713081ea08"));
        token.setUser(UserProvider.singleEntity());
        token.setTokenHash("revoked_hashed_refresh_token");
        token.setTokenJti(UUID.fromString("119b2092-e007-7671-a9fe-b2713081ea09"));
        token.setFamilyId(UUID.fromString("219b2092-e007-7671-a9fe-b2713081ea10"));
        token.setRevoked(true);
        token.setRevokedAt(Instant.parse("2024-01-01T09:50:00Z"));
        token.setExpiresAt(Instant.parse("2024-01-02T09:00:00Z"));
        token.setLastUsedAt(Instant.parse("2024-01-01T09:45:00Z"));
        token.setClientId("desktop-client");
        token.setIpAddress("10.0.0.1");
        token.setUserAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36");
        token.setRevokeReason("user_logout");
        return token;
    }

    public static RefreshToken createFromSession(
            Session session, UUID tokenJti, Instant expiresAt, Instant lastUsedAt) {
        return new RefreshToken(session, tokenJti, expiresAt, lastUsedAt);
    }
}
