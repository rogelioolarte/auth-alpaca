package com.alpaca.integration.service;

import static org.assertj.core.api.Assertions.*;

import com.alpaca.entity.RefreshToken;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link SessionServiceImpl}. */
@SpringBootTest
@Transactional
class SessionServiceImplIT {

    @Autowired private SessionServiceImpl sessionService;

    @Autowired private IUserDAO userDAO;

    @Autowired private ISessionDAO sessionDAO;

    @Autowired private IRefreshTokenDAO refreshTokenDAO;

    @Autowired private UUIDv7Generator uuidv7Generator;

    // ---------------------------------------------------------
    // createSession
    // ---------------------------------------------------------

    @Test
    @Transactional
    void createSession_shouldCreateNewSession_whenUserExistsAndDeviceIsNew() {
        User user = UserProvider.singleTemplate();
        userDAO.save(user);

        String userAgent = "Chrome";
        String clientId = "web";
        String ip = "127.0.0.1";

        Session result = sessionService.createSession(user.getId(), userAgent, clientId, ip);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getFamilyId()).isNotNull();
        assertThat(result.getRevokedAt()).isNull();
        assertThat(result.getUser().getId()).isEqualTo(user.getId());
        assertThat(result.getUserAgent()).isEqualTo(userAgent);
        assertThat(result.getClientId()).isEqualTo(clientId);
        assertThat(result.getIpAddress()).isEqualTo(ip);
        assertThat(result.getLastSeenAt()).isNotNull();
    }

    @Test
    @Transactional
    void createSession_shouldReuseSession_whenSameDeviceLogsAgain() {
        User user = UserProvider.singleTemplate();
        userDAO.save(user);

        Session session = SessionProvider.singleTemplate();
        session.setUser(user);
        session.setUserAgent("Chrome");
        session.setClientId("web");

        sessionDAO.save(session);

        UUID previousFamily = session.getFamilyId();

        Session result = sessionService.createSession(user.getId(), "Chrome", "web", "127.0.0.1");

        assertThat(result.getId()).isEqualTo(session.getId());
        assertThat(result.getFamilyId()).isNotEqualTo(previousFamily);
        assertThat(result.getRevokedAt()).isNull();
    }

    @Test
    @Transactional
    void createSession_shouldRevokeRefreshTokenFamily_whenSessionReused() {
        User user = UserProvider.singleTemplate();
        userDAO.save(user);

        Session session = SessionProvider.singleTemplate();
        session.setUser(user);
        session.setUserAgent("Chrome");
        session.setClientId("web");

        sessionDAO.save(session);

        UUID oldFamilyId = session.getFamilyId();

        sessionService.createSession(user.getId(), "Chrome", "web", "127.0.0.1");

        List<RefreshToken> revokedTokens = refreshTokenDAO.findAllByFamilyId(oldFamilyId);

        assertThat(revokedTokens).isNotNull();
    }

    @Test
    @Transactional
    void createSession_shouldThrowExceededSessionsException_whenMaxSessionsReached() {
        User user = UserProvider.singleTemplate();
        userDAO.save(user);

        int maxSessions = 10;

        for (int i = 0; i < maxSessions; i++) {
            Session session = SessionProvider.randomTemplate();
            session.setUser(user);
            session.setUserAgent("agent-" + i);
            session.setClientId("web");
            sessionDAO.save(session);
        }

        assertThatThrownBy(
                        () ->
                                sessionService.createSession(
                                        user.getId(), "another-device", "web", "127.0.0.1"))
                .isInstanceOf(ExceededSessionsException.class);
    }

    @Test
    @Transactional
    void createSession_shouldThrowNotFoundException_whenUserDoesNotExist() {
        UUID randomUserId = UUID.randomUUID();

        assertThatThrownBy(
                        () ->
                                sessionService.createSession(
                                        randomUserId, "Chrome", "web", "127.0.0.1"))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------------------------------------------------------
    // findSessionByFamilyId
    // ---------------------------------------------------------

    @Test
    @Transactional
    void findSessionByFamilyId_shouldReturnSession_whenExists() {
        User user = UserProvider.singleTemplate();
        userDAO.save(user);

        Session session = SessionProvider.singleTemplate();
        session.setUser(user);

        sessionDAO.save(session);

        Optional<Session> result = sessionService.findSessionByFamilyId(session.getFamilyId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(session.getId());
    }

    @Test
    @Transactional
    void findSessionByFamilyId_shouldReturnEmpty_whenNotExists() {
        Optional<Session> result = sessionService.findSessionByFamilyId(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    // ---------------------------------------------------------
    // revokeSessionByFamilyId
    // ---------------------------------------------------------

    @Test
    @Transactional
    void revokeSessionByFamilyId_shouldRevokeSession() {
        User user = UserProvider.singleTemplate();
        userDAO.save(user);

        Session session = SessionProvider.singleTemplate();
        session.setUser(user);

        sessionDAO.save(session);

        Instant revokedAt = Instant.now();

        sessionService.revokeSessionByFamilyId(session.getFamilyId(), revokedAt, "security-breach");

        Session updated = sessionDAO.findById(session.getId()).orElseThrow();

        assertThat(updated.getRevokedAt()).isNotNull();
    }

    // ---------------------------------------------------------
    // existsByUniqueProperties
    // ---------------------------------------------------------

    @Test
    @Transactional
    void existsByUniqueProperties_shouldReturnTrue_whenSessionExists() {
        User user = UserProvider.singleTemplate();
        userDAO.save(user);

        Session session = SessionProvider.singleTemplate();
        session.setUser(user);

        sessionDAO.save(session);

        boolean exists = sessionService.existsByUniqueProperties(session);

        assertThat(exists).isTrue();
    }

    @Test
    @Transactional
    void existsByUniqueProperties_shouldReturnFalse_whenSessionNotExists() {
        Session session = SessionProvider.singleTemplate();

        boolean exists = sessionService.existsByUniqueProperties(session);

        assertThat(exists).isFalse();
    }
}
