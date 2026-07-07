package com.alpaca.integration.persistence;

import com.alpaca.entity.Session;
import com.alpaca.entity.User;
import com.alpaca.persistence.ISessionDAO;
import com.alpaca.persistence.IUserDAO;
import com.alpaca.persistence.impl.SessionDAOImpl;
import com.alpaca.persistence.impl.UserDAOImpl;
import com.alpaca.resources.provider.SessionProvider;
import com.alpaca.resources.provider.UserProvider;
import com.alpaca.resources.utility.DataJpaIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Integration tests for {@link SessionDAOImpl}. */
@DataJpaIntegrationTest
@Import({SessionDAOImpl.class, UserDAOImpl.class})
@DisplayName("SessionDAOImpl Integration Tests")
class SessionDAOImplIT {

    @Autowired private ISessionDAO sessionDAO;
    @Autowired private IUserDAO userDAO;

    private Instant now;

    @BeforeEach
    void setup() {
        now = Instant.now();
    }

    private User buildUser() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);
        return user;
    }

    private User buildAlternativeUser() {
        User user = UserProvider.alternativeTemplate();
        user.setCreatedAt(now);
        return user;
    }

    private Session buildSession() {
        Session session = SessionProvider.singleTemplate();
        session.setCreatedAt(now);
        return session;
    }

    private Session buildAlternativeSession() {
        Session session = SessionProvider.alternativeTemplate();
        session.setCreatedAt(now);
        return session;
    }

    // -------------------------------------------------------------------------
    // existsByUniqueProperties
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("existsByUniqueProperties: should return false when user is null")
    void existsByUniqueProperties_ShouldReturnFalse_WhenUserIsNull() {

        Session session = buildSession();
        session.setUser(null);

        boolean result = sessionDAO.existsByUniqueProperties(session);

        assertThat(result).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("existsByUniqueProperties: should return false when user id is null")
    void existsByUniqueProperties_ShouldReturnFalse_WhenUserIdIsNull() {

        Session session = buildSession();

        User user = new User();
        user.setId(null);

        session.setUser(user);

        boolean result = sessionDAO.existsByUniqueProperties(session);

        assertThat(result).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("existsByUniqueProperties: should return false when userAgent is blank")
    void existsByUniqueProperties_ShouldReturnFalse_WhenUserAgentIsBlank() {

        User user = buildUser();
        userDAO.save(user);

        Session session = buildSession();
        session.setUser(user);
        session.setUserAgent(" ");

        boolean result = sessionDAO.existsByUniqueProperties(session);

        assertThat(result).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("existsByUniqueProperties: should return false when clientId is blank")
    void existsByUniqueProperties_ShouldReturnFalse_WhenClientIdIsBlank() {

        User user = buildUser();
        userDAO.save(user);

        Session session = buildSession();
        session.setUser(user);
        session.setClientId(" ");

        boolean result = sessionDAO.existsByUniqueProperties(session);

        assertThat(result).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("existsByUniqueProperties: should return false when ipAddress is blank")
    void existsByUniqueProperties_ShouldReturnFalse_WhenIpAddressIsBlank() {

        User user = buildUser();
        userDAO.save(user);

        Session session = buildSession();
        session.setUser(user);
        session.setIpAddress(" ");

        boolean result = sessionDAO.existsByUniqueProperties(session);

        assertThat(result).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("existsByUniqueProperties: should return true when matching session exists")
    void existsByUniqueProperties_ShouldReturnTrue_WhenSessionExists() {

        User user = buildUser();
        userDAO.save(user);

        Session session = buildSession();
        session.setUser(user);

        Session saved = sessionDAO.save(session);

        Session probe = buildAlternativeSession();
        probe.setUser(user);
        probe.setUserAgent(saved.getUserAgent());
        probe.setClientId(saved.getClientId());
        probe.setIpAddress(saved.getIpAddress());

        boolean result = sessionDAO.existsByUniqueProperties(probe);

        assertThat(result).isTrue();
    }

    @Test
    @Transactional
    @DisplayName(
            "existsByUniqueProperties: should return false when matching session does not exist")
    void existsByUniqueProperties_ShouldReturnFalse_WhenSessionDoesNotExist() {

        User user = buildUser();
        userDAO.save(user);

        Session session = buildSession();
        session.setUser(user);
        sessionDAO.save(session);

        Session probe = buildAlternativeSession();
        probe.setUser(user);
        probe.setUserAgent("non-existent-agent");
        probe.setClientId("non-existent-client");
        probe.setIpAddress("255.255.255.255");

        boolean result = sessionDAO.existsByUniqueProperties(probe);

        assertThat(result).isFalse();
    }

    // -------------------------------------------------------------------------
    // revokeSessionByFamilyId
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("revokeSessionByFamilyId: should revoke matching session")
    void revokeSessionByFamilyId_ShouldRevokeSession() {

        User user = buildUser();
        userDAO.save(user);

        Session session = buildSession();
        session.setUser(user);

        Session saved = sessionDAO.save(session);

        Instant revokedAt = now.plusSeconds(60);

        sessionDAO.revokeSessionByFamilyId(saved.getFamilyId(), revokedAt, "manual-revoke");

        Session updated = sessionDAO.findById(saved.getId()).orElseThrow();

        assertThat(updated.isRevoked()).isTrue();
        assertThat(updated.getRevokedAt()).isCloseTo(revokedAt, within(1, ChronoUnit.SECONDS));
        assertThat(updated.getRevokeReason()).isEqualTo("manual-revoke");
    }

    // -------------------------------------------------------------------------
    // findByIdAndUserId
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("findByIdAndUserId: should return session when ids match")
    void findByIdAndUserId_ShouldReturnSession_WhenIdsMatch() {

        User user = buildUser();
        userDAO.save(user);

        Session session = buildSession();
        session.setUser(user);

        Session saved = sessionDAO.save(session);

        Optional<Session> result = sessionDAO.findByIdAndUserId(saved.getId(), user.getId());

        assertTrue(result.isPresent());
        assertThat(result.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @Transactional
    @DisplayName("findByIdAndUserId: should return empty when user id does not match")
    void findByIdAndUserId_ShouldReturnEmpty_WhenUserIdDoesNotMatch() {

        User user = buildUser();
        userDAO.save(user);

        User anotherUser = buildAlternativeUser();
        userDAO.save(anotherUser);

        Session session = buildSession();
        session.setUser(user);

        Session saved = sessionDAO.save(session);

        Optional<Session> result = sessionDAO.findByIdAndUserId(saved.getId(), anotherUser.getId());

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // findSessionByFamilyId
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("findSessionByFamilyId: should return matching session")
    void findSessionByFamilyId_ShouldReturnSession() {

        User user = buildUser();
        userDAO.save(user);

        Session session = buildSession();
        session.setUser(user);

        Session saved = sessionDAO.save(session);

        Optional<Session> result = sessionDAO.findSessionByFamilyId(saved.getFamilyId());

        assertTrue(result.isPresent());
        assertThat(result.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @Transactional
    @DisplayName("findSessionByFamilyId: should return empty when family id does not exist")
    void findSessionByFamilyId_ShouldReturnEmpty_WhenFamilyIdDoesNotExist() {

        Optional<Session> result = sessionDAO.findSessionByFamilyId(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // findByUniqueProperties
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("findByUniqueProperties: should return session when unique properties match")
    void findByUniqueProperties_ShouldReturnSession_WhenPropertiesMatch() {

        User user = buildUser();
        userDAO.save(user);

        Session session = buildSession();
        session.setUser(user);

        Session saved = sessionDAO.save(session);

        Optional<Session> result =
                sessionDAO.findByUniqueProperties(
                        user.getId(),
                        saved.getUserAgent(),
                        saved.getClientId(),
                        saved.getIpAddress());

        assertTrue(result.isPresent());
        assertThat(result.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @Transactional
    @DisplayName("findByUniqueProperties: should return empty when unique properties do not match")
    void findByUniqueProperties_ShouldReturnEmpty_WhenPropertiesDoNotMatch() {

        User user = buildUser();
        userDAO.save(user);

        Session session = buildSession();
        session.setUser(user);

        sessionDAO.save(session);

        Optional<Session> result =
                sessionDAO.findByUniqueProperties(
                        user.getId(), "invalid-agent", "invalid-client", "0.0.0.0");

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // findFirstActiveSessionForUpdate
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("findFirstActiveSessionForUpdate: should return active session")
    void findFirstActiveSessionForUpdate_ShouldReturnActiveSession() {

        User user = buildUser();
        userDAO.save(user);

        Session session = buildSession();
        session.setUser(user);
        session.setRevoked(false);

        Session saved = sessionDAO.save(session);

        Optional<Session> result = sessionDAO.findFirstActiveSessionForUpdate(user.getId());

        assertTrue(result.isPresent());
        assertThat(result.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @Transactional
    @DisplayName("findFirstActiveSessionForUpdate: should return empty when all sessions revoked")
    void findFirstActiveSessionForUpdate_ShouldReturnEmpty_WhenAllSessionsRevoked() {

        User user = buildUser();
        userDAO.save(user);

        Session session = buildSession();
        session.setUser(user);
        session.setRevoked(true);
        session.setRevokedAt(now);

        sessionDAO.save(session);

        Optional<Session> result = sessionDAO.findFirstActiveSessionForUpdate(user.getId());

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // countByUserIdAndRevokedFalse
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("countByUserIdAndRevokedFalse: should count only active sessions")
    void countByUserIdAndRevokedFalse_ShouldCountOnlyActiveSessions() {

        User user = userDAO.save(buildUser());

        Session activeOne = buildSession();
        activeOne.setUser(user);
        activeOne.setRevoked(false);

        Session activeTwo = buildAlternativeSession();
        activeTwo.setUser(user);
        activeTwo.setRevoked(false);

        Session revoked = buildSession();
        revoked.setFamilyId(UUID.randomUUID());
        revoked.setCreatedAt(now.plusSeconds(1));
        revoked.setUser(user);
        revoked.setRevoked(true);
        revoked.setRevokedAt(now);

        sessionDAO.save(activeOne);
        sessionDAO.save(activeTwo);
        sessionDAO.save(revoked);

        long result = sessionDAO.countByUserIdAndRevokedFalse(user.getId());

        assertThat(result).isEqualTo(2L);
    }

    // -------------------------------------------------------------------------
    // revokeSessionsByUserId
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("revokeSessionsByUserId: should revoke all user sessions")
    void revokeSessionsByUserId_ShouldRevokeAllSessions() {

        User user = buildUser();
        userDAO.save(user);

        Session first = buildSession();
        first.setUser(user);

        Session second = buildAlternativeSession();
        second.setUser(user);

        Session savedFirst = sessionDAO.save(first);
        Session savedSecond = sessionDAO.save(second);

        Instant revokedAt = now.plusSeconds(120);

        sessionDAO.revokeSessionsByUserId(user.getId(), revokedAt, "global-logout");

        Session updatedFirst = sessionDAO.findById(savedFirst.getId()).orElseThrow();
        Session updatedSecond = sessionDAO.findById(savedSecond.getId()).orElseThrow();

        assertThat(updatedFirst.isRevoked()).isTrue();
        assertThat(updatedSecond.isRevoked()).isTrue();
        assertThat(updatedFirst.getRevokedAt()).isCloseTo(revokedAt, within(1, ChronoUnit.SECONDS));
        assertThat(updatedSecond.getRevokedAt())
                .isCloseTo(revokedAt, within(1, ChronoUnit.SECONDS));
        assertThat(updatedFirst.getRevokeReason()).isEqualTo("global-logout");
        assertThat(updatedSecond.getRevokeReason()).isEqualTo("global-logout");
    }

    // -------------------------------------------------------------------------
    // existsAllByIds
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("existsAllByIds: should return true when all ids exist")
    void existsAllByIds_ShouldReturnTrue_WhenAllIdsExist() {

        User user = buildUser();
        userDAO.save(user);

        Session first = buildSession();
        first.setUser(user);

        Session second = buildAlternativeSession();
        second.setUser(user);

        Session savedFirst = sessionDAO.save(first);
        Session savedSecond = sessionDAO.save(second);

        boolean result =
                sessionDAO.existsAllByIds(List.of(savedFirst.getId(), savedSecond.getId()));

        assertThat(result).isTrue();
    }

    @Test
    @Transactional
    @DisplayName("existsAllByIds: should return false when at least one id does not exist")
    void existsAllByIds_ShouldReturnFalse_WhenAtLeastOneIdDoesNotExist() {

        User user = buildUser();
        userDAO.save(user);

        Session session = buildSession();
        session.setUser(user);

        Session saved = sessionDAO.save(session);

        boolean result = sessionDAO.existsAllByIds(List.of(saved.getId(), UUID.randomUUID()));

        assertThat(result).isFalse();
    }

    @Test
    @Transactional
    @DisplayName(
            "revokeSessionByFamilyId: should not update sessions when family id does not exist")
    void revokeSessionByFamilyId_ShouldNotUpdateSessions_WhenFamilyIdDoesNotExist() {

        User user = buildUser();
        userDAO.save(user);

        Session session = buildSession();
        session.setUser(user);

        Session saved = sessionDAO.save(session);

        sessionDAO.revokeSessionByFamilyId(UUID.randomUUID(), now.plusSeconds(30), "not-applied");

        Session persisted = sessionDAO.findById(saved.getId()).orElseThrow();

        assertThat(persisted.isRevoked()).isFalse();
        assertThat(persisted.getRevokedAt()).isNull();
        assertThat(persisted.getRevokeReason()).isNull();
    }

    @Test
    @Transactional
    @DisplayName("findByIdAndUserId: should return empty when session id does not exist")
    void findByIdAndUserId_ShouldReturnEmpty_WhenSessionIdDoesNotExist() {

        User user = buildUser();
        userDAO.save(user);

        Optional<Session> result = sessionDAO.findByIdAndUserId(UUID.randomUUID(), user.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @Transactional
    @DisplayName("findFirstActiveSessionForUpdate: should return empty when user has no sessions")
    void findFirstActiveSessionForUpdate_ShouldReturnEmpty_WhenUserHasNoSessions() {

        User user = buildUser();
        userDAO.save(user);

        Optional<Session> result = sessionDAO.findFirstActiveSessionForUpdate(user.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @Transactional
    @DisplayName(
            "countByUserIdAndRevokedFalse: should return zero when user has no active sessions")
    void countByUserIdAndRevokedFalse_ShouldReturnZero_WhenUserHasNoActiveSessions() {

        User user = buildUser();
        userDAO.save(user);

        long result = sessionDAO.countByUserIdAndRevokedFalse(user.getId());

        assertThat(result).isZero();
    }

    @Test
    @Transactional
    @DisplayName("revokeSessionsByUserId: should not revoke sessions from other users")
    void revokeSessionsByUserId_ShouldNotRevokeSessionsFromOtherUsers() {

        User targetUser = buildUser();
        userDAO.save(targetUser);

        User anotherUser = buildAlternativeUser();
        userDAO.save(anotherUser);

        Session targetSession = buildSession();
        targetSession.setUser(targetUser);

        Session anotherSession = buildAlternativeSession();
        anotherSession.setUser(anotherUser);

        Session savedTarget = sessionDAO.save(targetSession);
        Session savedAnother = sessionDAO.save(anotherSession);

        Instant revokedAt = now.plusSeconds(90);

        sessionDAO.revokeSessionsByUserId(targetUser.getId(), revokedAt, "target-only");

        Session updatedTarget = sessionDAO.findById(savedTarget.getId()).orElseThrow();
        Session updatedAnother = sessionDAO.findById(savedAnother.getId()).orElseThrow();

        assertThat(updatedTarget.isRevoked()).isTrue();
        assertThat(updatedTarget.getRevokedAt())
                .isCloseTo(revokedAt, within(1, ChronoUnit.SECONDS));
        assertThat(updatedTarget.getRevokeReason()).isEqualTo("target-only");

        assertThat(updatedAnother.isRevoked()).isFalse();
        assertThat(updatedAnother.getRevokedAt()).isNull();
        assertThat(updatedAnother.getRevokeReason()).isNull();
    }

    @Test
    @Transactional
    @DisplayName("findAllByUserId: should return paginated sessions for user")
    void findAllByUserId_ShouldReturnPaginatedSessions() {

        User user = buildUser();
        userDAO.save(user);

        Session first = buildSession();
        first.setUser(user);

        Session second = buildAlternativeSession();
        second.setUser(user);

        sessionDAO.save(first);
        sessionDAO.save(second);

        Page<Session> result = sessionDAO.findAllByUserId(user.getId(), Pageable.ofSize(1));

        assertThat(result).isNotNull();
        assertEquals(1, result.getContent().size());
        assertThat(result.getTotalElements()).isEqualTo(2L);
    }

    @Test
    @Transactional
    @DisplayName("findAllByUserId: should return empty page when user has no sessions")
    void findAllByUserId_ShouldReturnEmptyPage_WhenUserHasNoSessions() {

        User user = buildUser();
        userDAO.save(user);

        Page<Session> result = sessionDAO.findAllByUserId(user.getId(), Pageable.ofSize(10));

        assertThat(result).isNotNull();
        assertTrue(result.getContent().isEmpty());
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @Transactional
    @DisplayName("existsAllByIds: should return true when ids collection is empty")
    void existsAllByIds_ShouldReturnTrue_WhenIdsCollectionIsEmpty() {

        boolean result = sessionDAO.existsAllByIds(List.of());

        assertThat(result).isTrue();
    }
}
