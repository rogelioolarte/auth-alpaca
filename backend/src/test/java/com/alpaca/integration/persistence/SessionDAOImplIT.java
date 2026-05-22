package com.alpaca.integration.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.entity.Session;
import com.alpaca.entity.User;
import com.alpaca.persistence.ISessionDAO;
import com.alpaca.persistence.impl.SessionDAOImpl;
import com.alpaca.repository.SessionRepo;
import com.alpaca.repository.UserRepo;
import com.alpaca.resources.SessionProvider;
import com.alpaca.resources.UserProvider;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link SessionDAOImpl}. */
@DataJpaTest
@Import(SessionDAOImpl.class)
class SessionDAOImplIT {

    @Autowired private ISessionDAO dao;

    @Autowired private SessionRepo repo;

    @Autowired private UserRepo userRepo;

    private Instant now;

    @BeforeEach
    void setup() {
        now = Instant.now();
    }

    private User persistUser(User user) {
        user.setCreatedAt(now);
        user.setCreatedBy(UUID.randomUUID().toString());

        return userRepo.save(user);
    }

    private Session buildSession(User user) {
        Session session = SessionProvider.singleTemplate();
        session.setCreatedAt(now);
        session.setCreatedBy(UUID.randomUUID().toString());
        session.setFamilyId(UUID.randomUUID());
        session.setUser(user);

        return session;
    }

    @Test
    @Transactional
    @DisplayName("save persists session")
    void save_ShouldPersistSession() {

        User user = persistUser(UserProvider.singleTemplate());

        Session session = buildSession(user);

        Session saved = dao.save(session);

        assertNotNull(saved.getId());

        Session dbSession = repo.findById(saved.getId()).orElseThrow();

        assertEquals(session.getIpAddress(), dbSession.getIpAddress());
        assertEquals(session.getUserAgent(), dbSession.getUserAgent());
        assertEquals(session.getClientId(), dbSession.getClientId());
        assertEquals(session.getUser().getId(), dbSession.getUser().getId());
    }

    @Test
    @Transactional
    @DisplayName("findById returns existing session")
    void findById_ShouldReturnExistingSession() {

        User user = persistUser(UserProvider.singleTemplate());

        Session saved = dao.save(buildSession(user));

        Optional<Session> result = dao.findById(saved.getId());

        assertTrue(result.isPresent());
        assertEquals(saved.getId(), result.orElseThrow().getId());
    }

    @Test
    @DisplayName("findById returns empty when session does not exist")
    void findById_ShouldReturnEmptyWhenSessionDoesNotExist() {

        Optional<Session> result = dao.findById(UUID.randomUUID());

        assertTrue(result.isEmpty());
    }

    @Test
    @Transactional
    @DisplayName("existsByUniqueProperties returns true when session exists")
    void existsByUniqueProperties_ShouldReturnTrueWhenSessionExists() {

        User user = persistUser(UserProvider.singleTemplate());

        Session saved = dao.save(buildSession(user));

        boolean exists = dao.existsByUniqueProperties(saved);

        assertTrue(exists);
    }

    @Test
    @DisplayName("existsByUniqueProperties returns false when user is null")
    void existsByUniqueProperties_ShouldReturnFalseWhenUserIsNull() {

        Session session = SessionProvider.singleTemplate();
        session.setCreatedAt(now);
        session.setCreatedBy(UUID.randomUUID().toString());
        session.setUser(null);

        boolean exists = dao.existsByUniqueProperties(session);

        assertFalse(exists);
    }

    @Test
    @DisplayName("existsByUniqueProperties returns false when user id is null")
    void existsByUniqueProperties_ShouldReturnFalseWhenUserIdIsNull() {

        User user = UserProvider.singleTemplate();
        user.setId(null);

        Session session = SessionProvider.singleTemplate();
        session.setCreatedAt(now);
        session.setCreatedBy(UUID.randomUUID().toString());
        session.setUser(user);

        boolean exists = dao.existsByUniqueProperties(session);

        assertFalse(exists);
    }

    @Test
    @DisplayName("existsByUniqueProperties returns false when userAgent is blank")
    void existsByUniqueProperties_ShouldReturnFalseWhenUserAgentIsBlank() {

        User user = UserProvider.singleTemplate();

        Session session = SessionProvider.singleTemplate();
        session.setCreatedAt(now);
        session.setCreatedBy(UUID.randomUUID().toString());
        session.setUser(user);
        session.setUserAgent(" ");

        boolean exists = dao.existsByUniqueProperties(session);

        assertFalse(exists);
    }

    @Test
    @DisplayName("existsByUniqueProperties returns false when clientId is blank")
    void existsByUniqueProperties_ShouldReturnFalseWhenClientIdIsBlank() {

        User user = UserProvider.singleTemplate();

        Session session = SessionProvider.singleTemplate();
        session.setCreatedAt(now);
        session.setCreatedBy(UUID.randomUUID().toString());
        session.setUser(user);
        session.setClientId("");

        boolean exists = dao.existsByUniqueProperties(session);

        assertFalse(exists);
    }

    @Test
    @DisplayName("existsByUniqueProperties returns false when ipAddress is blank")
    void existsByUniqueProperties_ShouldReturnFalseWhenIpAddressIsBlank() {

        User user = UserProvider.singleTemplate();

        Session session = SessionProvider.singleTemplate();
        session.setCreatedAt(now);
        session.setCreatedBy(UUID.randomUUID().toString());
        session.setUser(user);
        session.setIpAddress(" ");

        boolean exists = dao.existsByUniqueProperties(session);

        assertFalse(exists);
    }

    @Test
    @Transactional
    @DisplayName("findSessionByFamilyId returns session")
    void findSessionByFamilyId_ShouldReturnSession() {

        User user = persistUser(UserProvider.singleTemplate());

        Session session = buildSession(user);

        dao.save(session);

        Optional<Session> result = dao.findSessionByFamilyId(session.getFamilyId());

        assertTrue(result.isPresent());
        assertEquals(session.getFamilyId(), result.orElseThrow().getFamilyId());
    }

    @Test
    @DisplayName("findSessionByFamilyId returns empty when session does not exist")
    void findSessionByFamilyId_ShouldReturnEmptyWhenSessionDoesNotExist() {

        Optional<Session> result = dao.findSessionByFamilyId(UUID.randomUUID());

        assertTrue(result.isEmpty());
    }

    @Test
    @Transactional
    @DisplayName("findByUniqueProperties returns matching session")
    void findByUniqueProperties_ShouldReturnMatchingSession() {

        User user = persistUser(UserProvider.singleTemplate());

        Session saved = dao.save(buildSession(user));

        Optional<Session> result =
                dao.findByUniqueProperties(
                        saved.getUser().getId(),
                        saved.getUserAgent(),
                        saved.getClientId(),
                        saved.getIpAddress());

        assertTrue(result.isPresent());
        assertEquals(saved.getId(), result.orElseThrow().getId());
    }

    @Test
    @DisplayName("findByUniqueProperties returns empty when session does not exist")
    void findByUniqueProperties_ShouldReturnEmptyWhenSessionDoesNotExist() {

        Optional<Session> result =
                dao.findByUniqueProperties(
                        UUID.randomUUID(), "unknown-user-agent", "unknown-client", "unknown-ip");

        assertTrue(result.isEmpty());
    }

    @Test
    @Transactional
    @DisplayName("findActiveSessionsByUserOrderByLastSeen respects pagination")
    void findActiveSessionsByUserOrderByLastSeen_ShouldRespectPagination() {

        User user = persistUser(UserProvider.singleTemplate());

        Session firstSession = buildSession(user);
        firstSession.setLastSeenAt(now.minusSeconds(60));

        Session secondSession = SessionProvider.alternativeTemplate();
        secondSession.setCreatedAt(now);
        secondSession.setCreatedBy(UUID.randomUUID().toString());
        secondSession.setFamilyId(UUID.randomUUID());
        secondSession.setUser(user);
        secondSession.setLastSeenAt(now);

        dao.save(firstSession);
        dao.save(secondSession);

        List<Session> result =
                dao.findActiveSessionsByUserOrderByLastSeen(user.getId(), PageRequest.of(0, 1));

        assertEquals(1, result.size());
    }

    @Test
    @Transactional
    @DisplayName("revokeSessionByFamilyId revokes matching session")
    void revokeSessionByFamilyId_ShouldRevokeMatchingSession() {

        User user = persistUser(UserProvider.singleTemplate());

        Session saved = dao.save(buildSession(user));

        dao.revokeSessionByFamilyId(saved.getFamilyId(), now, "security");

        Session updated = repo.findById(saved.getId()).orElseThrow();

        assertTrue(updated.isRevoked());
        assertEquals("security", updated.getRevokeReason());
    }

    @Test
    @Transactional
    @DisplayName("revokeSessionByFamilyId does nothing when family id does not exist")
    void revokeSessionByFamilyId_ShouldDoNothingWhenFamilyIdDoesNotExist() {

        User user = persistUser(UserProvider.singleTemplate());

        Session saved = dao.save(buildSession(user));

        dao.revokeSessionByFamilyId(UUID.randomUUID(), now, "security");

        Session dbSession = repo.findById(saved.getId()).orElseThrow();

        assertFalse(dbSession.isRevoked());
        assertNull(dbSession.getRevokeReason());
    }

    @Test
    @Transactional
    @DisplayName("existsAllByIds returns true when all ids exist")
    void existsAllByIds_ShouldReturnTrueWhenAllIdsExist() {

        User firstUser = persistUser(UserProvider.singleTemplate());

        User secondUser = persistUser(UserProvider.alternativeTemplate());

        Session firstSession = buildSession(firstUser);

        Session secondSession = SessionProvider.alternativeTemplate();
        secondSession.setCreatedAt(now);
        secondSession.setCreatedBy(UUID.randomUUID().toString());
        secondSession.setFamilyId(UUID.randomUUID());
        secondSession.setUser(secondUser);

        UUID firstId = repo.save(firstSession).getId();
        UUID secondId = repo.save(secondSession).getId();

        boolean result = dao.existsAllByIds(List.of(firstId, secondId));

        assertTrue(result);
    }

    @Test
    @Transactional
    @DisplayName("existsAllByIds returns false when one id does not exist")
    void existsAllByIds_ShouldReturnFalseWhenOneIdDoesNotExist() {

        User user = persistUser(UserProvider.singleTemplate());

        UUID existingId = repo.save(buildSession(user)).getId();

        boolean result = dao.existsAllByIds(List.of(existingId, UUID.randomUUID()));

        assertFalse(result);
    }

    @Test
    @Transactional
    @DisplayName("existsAllByIds returns true for empty collection")
    void existsAllByIds_ShouldReturnTrueForEmptyCollection() {

        boolean result = dao.existsAllByIds(List.of());

        assertTrue(result);
    }
}
