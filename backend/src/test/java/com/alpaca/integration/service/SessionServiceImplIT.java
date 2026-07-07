package com.alpaca.integration.service;

import com.alpaca.entity.Session;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.ForbiddenException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IRefreshTokenDAO;
import com.alpaca.persistence.ISessionDAO;
import com.alpaca.persistence.IUserDAO;
import com.alpaca.resources.provider.SessionProvider;
import com.alpaca.resources.provider.UserProvider;
import com.alpaca.resources.utility.BaseIntegrationTests;
import com.alpaca.service.impl.SessionServiceImpl;
import com.alpaca.utils.UUIDv7Generator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

/** Integration tests for {@link SessionServiceImpl} */
@DisplayName("SessionServiceImpl Integration Tests")
class SessionServiceImplIT extends BaseIntegrationTests {

    @Autowired private SessionServiceImpl sessionService;
    @Autowired private IUserDAO userDAO;
    @Autowired private ISessionDAO sessionDAO;
    @Autowired private IRefreshTokenDAO refreshTokenDAO;

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

    @Test
    @Transactional
    @DisplayName("constructor: should throw when max sessions is lower than one")
    void constructor_ShouldThrow_WhenMaxSessionsLowerThanOne() {
        Throwable thrown =
                catchThrowable(
                        () ->
                                new SessionServiceImpl(
                                        sessionDAO,
                                        userDAO,
                                        refreshTokenDAO,
                                        new UUIDv7Generator(),
                                        0,
                                        false));
        assertThat(thrown)
                .as("security.max.session.per.user must be >= 1")
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @Transactional
    @DisplayName("findSessionByFamilyId: should return session when family id exists")
    void findSessionByFamilyId_ShouldReturnSession_WhenFamilyIdExists() {

        User user = buildUser();
        userDAO.save(user);

        Session session = buildSession();
        session.setUser(user);

        Session saved = sessionDAO.save(session);

        Optional<Session> result = sessionService.findSessionByFamilyId(saved.getFamilyId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @Transactional
    @DisplayName("findSessionByFamilyId: should return empty when family id does not exist")
    void findSessionByFamilyId_ShouldReturnEmpty_WhenFamilyIdDoesNotExist() {

        Optional<Session> result = sessionService.findSessionByFamilyId(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    @Transactional
    @DisplayName("existsByUniqueProperties: should return true when session exists")
    void existsByUniqueProperties_ShouldReturnTrue_WhenSessionExists() {

        User user = buildUser();
        userDAO.save(user);

        Session session = buildSession();
        session.setUser(user);

        Session saved = sessionDAO.save(session);

        boolean exists = sessionService.existsByUniqueProperties(saved);

        assertThat(exists).isTrue();
    }

    @Test
    @Transactional
    @DisplayName("existsByUniqueProperties: should return false when session does not exist")
    void existsByUniqueProperties_ShouldReturnFalse_WhenSessionDoesNotExist() {

        Session session = buildSession();

        boolean exists = sessionService.existsByUniqueProperties(session);

        assertThat(exists).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("createSession: should throw when user does not exist")
    void createSession_ShouldThrow_WhenUserDoesNotExist() {

        UUID userId = UUID.randomUUID();

        assertThatThrownBy(
                        () ->
                                sessionService.createSession(
                                        userId, "agent", "client-id", "127.0.0.1"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @Transactional
    @DisplayName("createSession: should create new session")
    void createSession_ShouldCreateNewSession() {

        User user = buildUser();
        User savedUser = userDAO.save(user);

        Session result =
                sessionService.createSession(
                        savedUser.getId(), "Mozilla", "web-client", "127.0.0.1");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getUser().getId()).isEqualTo(savedUser.getId());
        assertThat(result.getUserAgent()).isEqualTo("Mozilla");
        assertThat(result.getClientId()).isEqualTo("web-client");
        assertThat(result.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(result.isRevoked()).isFalse();
        assertThat(result.getFamilyId()).isNotNull();
        assertThat(result.getLastSeenAt()).isNotNull();
    }

    @Test
    @Transactional
    @DisplayName("createSession: should reuse existing session with same unique properties")
    void createSession_ShouldReuseExistingSession_WhenUniquePropertiesMatch() {

        User user = buildUser();
        User savedUser = userDAO.save(user);

        Session existing = buildSession();
        existing.setUser(savedUser);
        existing.setUserAgent("Mozilla");
        existing.setClientId("client");
        existing.setIpAddress("127.0.0.1");

        Session savedSession = sessionDAO.save(existing);

        UUID previousFamilyId = savedSession.getFamilyId();

        Session result =
                sessionService.createSession(savedUser.getId(), "Mozilla", "client", "127.0.0.1");

        assertThat(result.getId()).isEqualTo(savedSession.getId());
        assertThat(result.getFamilyId()).isNotEqualTo(previousFamilyId);
        assertThat(result.isRevoked()).isFalse();
        assertThat(result.getLastSeenAt()).isNotNull();
    }

    @Test
    @Transactional
    @DisplayName("revokeSessionByUserIdAndId: should revoke session")
    void revokeSessionByUserIdAndId_ShouldRevokeSession() {

        User user = buildUser();
        User savedUser = userDAO.save(user);

        Session session = buildSession();
        session.setUser(savedUser);

        Session savedSession = sessionDAO.save(session);

        sessionService.revokeSessionByUserIdAndId(savedUser.getId(), savedSession.getId());

        Optional<Session> updated = sessionDAO.findById(savedSession.getId());

        assertThat(updated).isPresent();
        assertThat(updated.get().isRevoked()).isTrue();
        assertThat(updated.get().getRevokedAt()).isNotNull();
        assertThat(updated.get().getRevokeReason()).isEqualTo("user-self-revocation");
    }

    @Test
    @Transactional
    @DisplayName("revokeSessionByUserIdAndId: should throw when session does not belong to user")
    void revokeSessionByUserIdAndId_ShouldThrow_WhenSessionDoesNotBelongToUser() {

        User user = buildUser();
        User savedUser = userDAO.save(user);

        Session session = buildSession();
        session.setUser(savedUser);

        Session savedSession = sessionDAO.save(session);

        Throwable thrown =
                catchThrowable(
                        () ->
                                sessionService.revokeSessionByUserIdAndId(
                                        UUID.randomUUID(), savedSession.getId()));
        assertThat(thrown).as("Invalid Session to revoke").isInstanceOf(ForbiddenException.class);
    }

    @Test
    @Transactional
    @DisplayName("revokeAllSessionsByUserId: should revoke all sessions")
    void revokeAllSessionsByUserId_ShouldRevokeAllSessions() {

        User user = buildUser();
        User savedUser = userDAO.save(user);

        Session first = buildSession();
        first.setUser(savedUser);

        Session second = buildAlternativeSession();
        second.setCreatedAt(now.plusSeconds(1));
        second.setUser(savedUser);

        Session savedFirst = sessionDAO.save(first);
        Session savedSecond = sessionDAO.save(second);

        sessionService.revokeAllSessionsByUserId(savedUser.getId());

        Session updatedFirst = sessionDAO.findById(savedFirst.getId()).orElseThrow();

        Session updatedSecond = sessionDAO.findById(savedSecond.getId()).orElseThrow();

        assertThat(updatedFirst.isRevoked()).isTrue();
        assertThat(updatedSecond.isRevoked()).isTrue();
        assertThat(updatedFirst.getRevokeReason()).isEqualTo("user-self-revocation");
        assertThat(updatedSecond.getRevokeReason()).isEqualTo("user-self-revocation");
        assertThat(updatedFirst.getRevokedAt()).isNotNull();
        assertThat(updatedSecond.getRevokedAt()).isNotNull();
    }

    @Test
    @Transactional
    @DisplayName("revokeSessionByFamilyId: should revoke session by family id")
    void revokeSessionByFamilyId_ShouldRevokeSessionByFamilyId() {

        User user = buildUser();
        User savedUser = userDAO.save(user);

        Session session = buildSession();
        session.setUser(savedUser);

        Session savedSession = sessionDAO.save(session);

        Instant revokedAt = Instant.now();

        sessionService.revokeSessionByFamilyId(
                savedSession.getFamilyId(), revokedAt, "manual-revoke");

        Session updated = sessionDAO.findById(savedSession.getId()).orElseThrow();

        assertThat(updated.isRevoked()).isTrue();
        assertThat(updated.getRevokedAt()).isCloseTo(revokedAt, within(1, ChronoUnit.SECONDS));

        assertThat(updated.getRevokeReason()).isEqualTo("manual-revoke");
    }

    @Test
    @Transactional
    @DisplayName("updateById: should throw when session is null")
    void updateById_ShouldThrow_WhenSessionIsNull() {

        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> sessionService.updateById(null, id))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Session with ID " + id + " cannot be updated");
    }

    @Test
    @Transactional
    @DisplayName("updateById: should throw when id is null")
    void updateById_ShouldThrow_WhenIdIsNull() {

        Session update = buildAlternativeSession();

        assertThatThrownBy(() -> sessionService.updateById(update, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Session with ID null cannot be updated");
    }

    @Test
    @Transactional
    @DisplayName("updateById: should throw when session does not exist")
    void updateById_ShouldThrow_WhenSessionDoesNotExist() {

        Session update = buildAlternativeSession();

        UUID randomId = UUID.randomUUID();

        assertThatThrownBy(() -> sessionService.updateById(update, randomId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Session with ID " + randomId + " not found");
    }

    @Test
    @Transactional
    @DisplayName("updateById: should update all mutable fields")
    void updateById_ShouldUpdateAllMutableFields() {

        User user = buildUser();
        userDAO.save(user);

        User alternativeUser = buildAlternativeUser();
        userDAO.save(alternativeUser);

        Session existing = buildSession();
        existing.setUser(user);
        existing.setRevoked(false);
        existing.setRevokedAt(null);
        existing.setRevokeReason(null);

        Session saved = sessionDAO.save(existing);

        UUID newFamilyId = UUID.randomUUID();

        Instant revokedAt = now.plusSeconds(300);

        Session update = buildAlternativeSession();
        update.setUser(alternativeUser);
        update.setFamilyId(newFamilyId);
        update.setIpAddress("10.10.10.10");
        update.setUserAgent("Updated-Agent");
        update.setClientId("updated-client");
        update.setRevoked(true);
        update.setRevokedAt(revokedAt);
        update.setRevokeReason("manual-revoke");

        Session result = sessionService.updateById(update, saved.getId());

        assertThat(result.getUser().getId()).isEqualTo(alternativeUser.getId());
        assertThat(result.getFamilyId()).isEqualTo(newFamilyId);
        assertThat(result.getIpAddress()).isEqualTo("10.10.10.10");
        assertThat(result.getUserAgent()).isEqualTo("Updated-Agent");
        assertThat(result.getClientId()).isEqualTo("updated-client");
        assertThat(result.isRevoked()).isTrue();
        assertThat(result.getRevokedAt()).isEqualTo(revokedAt);
        assertThat(result.getRevokeReason()).isEqualTo("manual-revoke");
    }

    @Test
    @Transactional
    @DisplayName("updateById: should ignore blank text values")
    void updateById_ShouldIgnoreBlankTextValues() {

        User user = buildUser();
        userDAO.save(user);

        Session existing = buildSession();
        existing.setUser(user);

        Session saved = sessionDAO.save(existing);

        String originalIp = saved.getIpAddress();
        String originalAgent = saved.getUserAgent();
        String originalClient = saved.getClientId();

        Session update = buildAlternativeSession();
        update.setIpAddress(" ");
        update.setUserAgent(" ");
        update.setClientId(" ");

        Session result = sessionService.updateById(update, saved.getId());

        assertThat(result.getIpAddress()).isEqualTo(originalIp);
        assertThat(result.getUserAgent()).isEqualTo(originalAgent);
        assertThat(result.getClientId()).isEqualTo(originalClient);
    }

    @Test
    @Transactional
    @DisplayName("updateById: should ignore null familyId and revokedAt")
    void updateById_ShouldIgnoreNullFields() {

        User user = buildUser();
        userDAO.save(user);

        Session existing = buildSession();
        existing.setUser(user);

        UUID originalFamilyId = existing.getFamilyId();

        Session saved = sessionDAO.save(existing);

        Session update = buildAlternativeSession();
        update.setFamilyId(null);
        update.setRevokedAt(null);

        Session result = sessionService.updateById(update, saved.getId());

        assertThat(result.getFamilyId()).isEqualTo(originalFamilyId);
        assertThat(result.getRevokedAt()).isNull();
    }

    @Test
    @Transactional
    @DisplayName("updateById: should not replace user when ids are equal")
    void updateById_ShouldNotReplaceUser_WhenIdsAreEqual() {

        User user = buildUser();
        userDAO.save(user);

        Session existing = buildSession();
        existing.setUser(user);

        Session saved = sessionDAO.save(existing);

        Session update = buildAlternativeSession();

        User sameUserReference = new User();
        sameUserReference.setId(user.getId());

        update.setUser(sameUserReference);

        Session result = sessionService.updateById(update, saved.getId());

        assertThat(result.getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    @Transactional
    @DisplayName("updateById: should ignore user when incoming user id is null")
    void updateById_ShouldIgnoreUser_WhenIncomingUserIdIsNull() {

        User user = buildUser();
        userDAO.save(user);

        Session existing = buildSession();
        existing.setUser(user);

        Session saved = sessionDAO.save(existing);

        Session update = buildAlternativeSession();

        User incomingUser = new User();
        incomingUser.setId(null);

        update.setUser(incomingUser);

        Session result = sessionService.updateById(update, saved.getId());

        assertThat(result.getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    @Transactional
    @DisplayName("updateById: should ignore null user")
    void updateById_ShouldIgnoreNullUser() {

        User user = buildUser();
        userDAO.save(user);

        Session existing = buildSession();
        existing.setUser(user);

        Session saved = sessionDAO.save(existing);

        Session update = buildAlternativeSession();
        update.setUser(null);

        Session result = sessionService.updateById(update, saved.getId());

        assertThat(result.getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    @Transactional
    @DisplayName("findAllByUserId: should return paginated sessions for specific user only")
    void findAllByUserId_ShouldReturnPaginatedSessions_ForSpecificUserOnly() {

        User user = buildUser();
        User savedUser = userDAO.save(user);

        User anotherUser = buildAlternativeUser();
        User savedAnotherUser = userDAO.save(anotherUser);

        Session first = buildSession();
        first.setUser(savedUser);

        Session second = buildAlternativeSession();
        second.setCreatedAt(now.plusSeconds(1));
        second.setUser(savedUser);

        Session third = buildSession();
        third.setCreatedAt(now.plusSeconds(2));
        third.setFamilyId(UUID.randomUUID());
        third.setUser(savedAnotherUser);

        sessionDAO.save(first);
        sessionDAO.save(second);
        sessionDAO.save(third);

        Page<Session> result =
                sessionService.findAllByUserId(savedUser.getId(), Pageable.ofSize(10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2L);
        assertThat(result.getContent())
                .allMatch(session -> session.getUser().getId().equals(savedUser.getId()));
    }

    @Test
    @Transactional
    @DisplayName("findAllByUserId: should return empty page when user has no sessions")
    void findAllByUserId_ShouldReturnEmptyPage_WhenUserHasNoSessions() {

        User user = buildUser();
        User savedUser = userDAO.save(user);

        Page<Session> result =
                sessionService.findAllByUserId(savedUser.getId(), Pageable.ofSize(10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
    }

    @Test
    @Transactional
    @DisplayName("findAllByUserId: should respect pagination boundaries")
    void findAllByUserId_ShouldRespectPaginationBoundaries() {

        User user = buildUser();
        User savedUser = userDAO.save(user);

        Session first = buildSession();
        first.setUser(savedUser);

        Session second = buildAlternativeSession();
        second.setCreatedAt(now.plusSeconds(1));
        second.setUser(savedUser);

        sessionDAO.save(first);
        sessionDAO.save(second);

        Pageable pageable = Pageable.ofSize(1).withPage(1);

        Page<Session> result = sessionService.findAllByUserId(savedUser.getId(), pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(2L);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getNumber()).isEqualTo(1);
    }

    @Test
    @Transactional
    @DisplayName("findAllByUserId: should return all sessions when pageable is unpaged")
    void findAllByUserId_ShouldReturnAllSessions_WhenPageableIsUnpaged() {

        User user = buildUser();
        User savedUser = userDAO.save(user);

        Session first = buildSession();
        first.setUser(savedUser);

        Session second = buildAlternativeSession();
        second.setCreatedAt(now.plusSeconds(1));
        second.setUser(savedUser);

        sessionDAO.save(first);
        sessionDAO.save(second);

        Page<Session> result =
                sessionService.findAllByUserId(savedUser.getId(), Pageable.unpaged());

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2L);
    }
}
