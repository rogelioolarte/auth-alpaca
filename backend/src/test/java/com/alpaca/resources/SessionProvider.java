package com.alpaca.resources;

import com.alpaca.entity.Session;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SessionProvider {

    public static Session singleEntity() {
        Session session = new Session();
        Instant now = Instant.now();
        session.setId(UUID.fromString("019e0f51-038c-7f79-96b5-be2e0b329111"));
        session.setUser(UserProvider.singleEntity());
        session.setFamilyId(UUID.fromString("2632eb79-63a4-4213-b905-0ad176f0004b"));
        session.setCreatedAt(now);
        session.setLastSeenAt(now);
        session.setIpAddress("127.0.0.1");
        session.setUserAgent("Mozilla");
        session.setClientId("web-client");
        session.setRevoked(false);
        session.setRevokedAt(null);
        session.setRevokeReason(null);
        return session;
    }

    public static Session alternativeEntity() {
        Session session = new Session();
        session.setId(UUID.fromString("019e0f52-a9da-7560-a196-359bbcf6571c"));
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

    public static Session singleTemplate() {
        Session session = new Session();
        Instant now = Instant.now();
        session.setCreatedAt(now);
        session.setLastSeenAt(now);
        session.setFamilyId(UUID.fromString("119b2092-e007-7671-a9fe-b2713081ea09"));
        session.setIpAddress("127.0.0.1");
        session.setUserAgent("Mozilla");
        session.setClientId("web-client");
        session.setRevoked(false);
        session.setRevokedAt(null);
        session.setRevokeReason(null);
        return session;
    }

    public static Session alternativeTemplate() {
        Session session = new Session();
        Instant now = Instant.now();
        session.setCreatedAt(now);
        session.setLastSeenAt(now);
        session.setFamilyId(UUID.fromString("019e101e-d981-7edf-a4cf-04e4a03fffed"));
        session.setIpAddress("127.0.0.1");
        session.setUserAgent("Mozilla");
        session.setClientId("web-client");
        session.setRevoked(false);
        session.setRevokedAt(null);
        session.setRevokeReason(null);
        return session;
    }

    public static Session randomTemplate() {
        Session session = new Session();
        Instant now = Instant.now();
        session.setCreatedAt(now);
        session.setLastSeenAt(now);
        session.setFamilyId(UUID.randomUUID());
        session.setIpAddress("127.0.0.1");
        session.setUserAgent("Mozilla");
        session.setClientId("web-client");
        session.setRevoked(false);
        session.setRevokedAt(null);
        session.setRevokeReason(null);
        return session;
    }

    public static List<Session> listEntities() {
        return new ArrayList<>(List.of(singleEntity(), alternativeEntity()));
    }
}
