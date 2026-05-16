package com.alpaca.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.alpaca.entity.Session;
import com.alpaca.entity.User;
import com.alpaca.exception.ExceededSessionsException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IRefreshTokenDAO;
import com.alpaca.persistence.ISessionDAO;
import com.alpaca.persistence.IUserDAO;
import com.alpaca.resources.SessionProvider;
import com.alpaca.resources.UserProvider;
import com.alpaca.service.impl.SessionServiceImpl;
import com.alpaca.utils.UUIDv7Generator;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link SessionServiceImpl} */
@SpringBootTest
@Transactional
@DisplayName("SessionServiceImpl Integration Tests")
class SessionServiceImplIT {

    @Autowired private SessionServiceImpl sessionService;
    @Autowired private IUserDAO userDAO;
    @Autowired private ISessionDAO sessionDAO;
    @Autowired private IRefreshTokenDAO refreshTokenDAO;
    @Autowired private UUIDv7Generator uuidv7Generator;

    private Instant now;

    @BeforeEach
    void setup() {
        // No save operations here per best practices
        now = Instant.now();
    }

    @Test
    @DisplayName("createSession: Create new session when user exists and device is unique")
    @Transactional
    void createSession_ShouldCreateNew_WhenDeviceIsNew() {
        // Arrange
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);
        userDAO.save(user);

        String agent = "Mozilla/5.0";
        String client = "web-app";
        String ip = "192.168.1.1";

        // Act
        Session result = sessionService.createSession(user.getId(), agent, client, ip);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUser().getId()).isEqualTo(user.getId());
        assertThat(result.getUserAgent()).isEqualTo(agent);
        assertThat(result.getIpAddress()).isEqualTo(ip);
        assertThat(result.isRevoked()).isFalse();
        assertThat(result.getLastSeenAt()).isNotNull();
    }

    @Test
    @DisplayName("createSession: Reuse existing session and rotate family ID when device matches")
    @Transactional
    void createSession_ShouldReuse_WhenDeviceMatches() {
        // Arrange
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);
        userDAO.save(user);

        Session existing = SessionProvider.singleTemplate();
        existing.setUser(user);
        existing.setUserAgent("Mobile-App");
        existing.setClientId("ios");
        existing.setIpAddress("10.0.0.1");
        existing.setCreatedAt(now);
        sessionDAO.save(existing);

        UUID oldFamilyId = existing.getFamilyId();

        // Act
        Session result =
                sessionService.createSession(user.getId(), "Mobile-App", "ios", "10.0.0.1");

        // Assert
        assertThat(result.getId()).isEqualTo(existing.getId());
        assertThat(result.getFamilyId()).isNotEqualTo(oldFamilyId);
        assertThat(result.getLastSeenAt()).isAfterOrEqualTo(now);
    }

    @Test
    @DisplayName(
            "createSession: Throw ExceededSessionsException when limit is reached"
                    + " (infinityLogin=false)")
    @Transactional
    void createSession_ShouldThrow_WhenMaxSessionsReached() {
        // Manually instantiate service to control property values for this edge case
        SessionServiceImpl restrictedService =
                new SessionServiceImpl(
                        sessionDAO, userDAO, refreshTokenDAO, uuidv7Generator, 2, false);

        // Arrange
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);
        userDAO.save(user);

        for (int i = 0; i < 2; i++) {
            Session s = SessionProvider.randomTemplate();
            s.setUser(user);
            s.setUserAgent("Agent-" + i);
            s.setCreatedAt(now);
            sessionDAO.save(s);
        }
        UUID id = user.getId();
        String agent = "New-Agent";
        String client = "web";
        String ip = "1.1.1.1";

        // Act & Assert
        assertThatThrownBy(() -> restrictedService.createSession(id, agent, client, ip))
                .isInstanceOf(ExceededSessionsException.class);
    }

    @Test
    @DisplayName("createSession: Revoke oldest session when limit is reached (infinityLogin=true)")
    @Transactional
    void createSession_ShouldRevokeOldest_WhenInfinityLoginTrue() {
        // Arrange: limit of 1 session, infinity login enabled
        SessionServiceImpl infinityService =
                new SessionServiceImpl(
                        sessionDAO, userDAO, refreshTokenDAO, uuidv7Generator, 1, true);

        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);
        userDAO.save(user);

        Session oldest = SessionProvider.singleTemplate();
        oldest.setUser(user);
        oldest.setCreatedAt(now.minusSeconds(100)); // Make it clearly oldest
        oldest.setLastSeenAt(now.minusSeconds(100));
        sessionDAO.save(oldest);

        // Act
        Session newest =
                infinityService.createSession(user.getId(), "New-Device", "web", "2.2.2.2");

        // Assert
        Session updatedOldest = sessionDAO.findById(oldest.getId()).orElseThrow();
        assertThat(updatedOldest.getRevokedAt()).isNotNull(); // Oldest was revoked
        assertThat(newest.getId()).isNotEqualTo(oldest.getId()); // New session created
    }

    @Test
    @DisplayName("createSession: Throw NotFoundException when userId is invalid")
    @Transactional
    void createSession_ShouldThrowNotFound_WhenUserMissing() {
        UUID randomId = UUID.randomUUID();
        assertThatThrownBy(() -> sessionService.createSession(randomId, "agent", "client", "ip"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("revokeSessionByFamilyId: Mark session as revoked")
    @Transactional
    void revokeSessionByFamilyId_ShouldUpdateStatus() {
        // Arrange
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);
        userDAO.save(user);

        Session session = SessionProvider.singleTemplate();
        session.setUser(user);
        session.setCreatedAt(now);
        sessionDAO.save(session);

        // Act
        sessionService.revokeSessionByFamilyId(session.getFamilyId(), now, "Log out");

        // Assert
        Optional<Session> updated = sessionDAO.findById(session.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getRevokedAt()).isNotNull();
    }

    @Test
    @DisplayName("findSessionByFamilyId: Retrieve session by family correlation")
    @Transactional
    void findSessionByFamilyId_ShouldReturnSession() {
        // Arrange
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);
        userDAO.save(user);

        Session session = SessionProvider.singleTemplate();
        session.setUser(user);
        session.setCreatedAt(now);
        sessionDAO.save(session);

        // Act
        Optional<Session> result = sessionService.findSessionByFamilyId(session.getFamilyId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(session.getId());
    }

    @Test
    @DisplayName("existsByUniqueProperties: Check existence based on unique constraints")
    @Transactional
    void existsByUniqueProperties_ShouldReturnCorrectBoolean() {
        // Arrange
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);
        userDAO.save(user);

        Session session = SessionProvider.singleTemplate();
        session.setUser(user);
        session.setCreatedAt(now);
        sessionDAO.save(session);

        // Act & Assert
        assertThat(sessionService.existsByUniqueProperties(session)).isTrue();

        Session nonExistent = SessionProvider.alternativeTemplate();
        assertThat(sessionService.existsByUniqueProperties(nonExistent)).isFalse();
    }
}
