package com.alpaca.resources;

import com.alpaca.entity.Session;
import java.time.Instant;
import java.util.*;

public class SessionProvider {

    public static Session singleEntity() {
        Session session = new Session();
        session.setId(UUID.fromString("1632eb79-63a4-4213-b905-0ad176f0004a"));
        session.setUser(UserProvider.singleEntity());
        session.setFamilyId(UUID.fromString("2632eb79-63a4-4213-b905-0ad176f0004b"));
        session.setCreatedAt(Instant.parse("2024-01-01T10:00:00Z"));
        session.setLastSeenAt(Instant.parse("2024-01-01T10:30:00Z"));
        session.setIpAddress("127.0.0.1");
        session.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        session.setClientId("web-client");
        session.setRevoked(false);
        return session;
    }

    public static Session alternativeEntity() {
        Session session = new Session();
        session.setId(UUID.fromString("982a1001-b033-48f6-b2e6-6b327f0a61eb"));
        session.setUser(UserProvider.alternativeEntity());
        session.setFamilyId(UUID.fromString("a82a1001-b033-48f6-b2e6-6b327f0a61ec"));
        session.setCreatedAt(Instant.parse("2024-01-02T11:00:00Z"));
        session.setLastSeenAt(Instant.parse("2024-01-02T11:15:00Z"));
        session.setIpAddress("192.168.1.1");
        session.setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36");
        session.setClientId("mobile-client");
        session.setRevoked(false);
        return session;
    }

    public static Session revokedEntity() {
        Session session = new Session();
        session.setId(UUID.fromString("019b2092-e007-7671-a9fe-b2713081ea08"));
        session.setUser(UserProvider.singleEntity());
        session.setFamilyId(UUID.fromString("119b2092-e007-7671-a9fe-b2713081ea09"));
        session.setCreatedAt(Instant.parse("2024-01-01T09:00:00Z"));
        session.setLastSeenAt(Instant.parse("2024-01-01T09:45:00Z"));
        session.setIpAddress("10.0.0.1");
        session.setUserAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36");
        session.setClientId("desktop-client");
        session.setRevoked(true);
        session.setRevokedAt(Instant.parse("2024-01-01T09:50:00Z"));
        session.setRevokeReason("user_logout");
        return session;
    }
}
