package com.alpaca.integration.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.entity.Session;
import com.alpaca.entity.User;
import com.alpaca.exception.NotFoundException;
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
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

/** Integration tests for {@link SessionDAOImpl} */
@DataJpaTest
@Import(SessionDAOImpl.class)
class SessionDAOImplIT {

    @Autowired private ISessionDAO dao;
    @Autowired private SessionRepo repo;
    @Autowired private UserRepo userRepo;

    private User user;
    private Instant now;

    @BeforeEach
    void setup() {
        now = Instant.now();
        user = UserProvider.singleTemplate();
    }

    private Session newSession(User user) {
        if (user == null) {
            User unsaved = UserProvider.singleTemplate();
            unsaved.setCreatedBy(UUID.randomUUID().toString());
            unsaved.setCreatedAt(Instant.now());
            user = userRepo.save(unsaved);
        }
        Session s = SessionProvider.singleTemplate();
        s.setUser(user);
        s.setFamilyId(UUID.randomUUID());
        return s;
    }

    @Test
    @DisplayName("save persists session")
    void save() {

        Session session = newSession(null);

        Session saved = dao.save(session);

        assertNotNull(saved.getId());

        Session db = repo.findById(saved.getId()).orElseThrow();

        assertEquals(session.getIpAddress(), db.getIpAddress());
        assertEquals(session.getUserAgent(), db.getUserAgent());
    }

    @Test
    @DisplayName("findById returns existing session")
    void findById() {

        Session saved = dao.save(newSession(null));

        Optional<Session> result = dao.findById(saved.getId());

        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("findById returns empty when not found")
    void findByIdNotFound() {

        Optional<Session> result = dao.findById(UUID.randomUUID());

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("updateById updates all fields")
    void updateById() {

        Session existing = dao.save(newSession(null));

        Session update = new Session();
        update.setFamilyId(UUID.randomUUID());
        update.setIpAddress("127.0.0.1");
        update.setUserAgent("Chrome");
        update.setClientId("mobile");
        update.setRevoked(true);
        update.setRevokedAt(now);
        update.setRevokeReason("logout");

        Session updated = dao.updateById(update, existing.getId());

        assertAll(
                () -> assertEquals("127.0.0.1", updated.getIpAddress()),
                () -> assertEquals("Chrome", updated.getUserAgent()),
                () -> assertEquals("mobile", updated.getClientId()),
                () -> assertTrue(updated.isRevoked()),
                () -> assertEquals("logout", updated.getRevokeReason()));
    }

    @Test
    @DisplayName("updateById ignores null fields")
    void updatePartial() {

        Session existing = dao.save(newSession(null));

        Session update = new Session();
        update.setIpAddress("8.8.8.8");

        Session updated = dao.updateById(update, existing.getId());

        assertEquals("8.8.8.8", updated.getIpAddress());
        assertEquals(existing.getUserAgent(), updated.getUserAgent());
    }

    @Test
    @DisplayName("updateById changes user when different")
    void updateUser() {

        Session existing = dao.save(newSession(null));

        User newUser = userRepo.save(UserProvider.alternativeTemplate());

        Session update = new Session();
        update.setUser(newUser);

        Session updated = dao.updateById(update, existing.getId());

        assertEquals(newUser.getId(), updated.getUser().getId());
    }

    @Test
    @DisplayName("updateById ignores user when same")
    void updateUserSameIgnored() {
        Session existing = dao.save(newSession(null));

        Session update = new Session();
        update.setId(UUID.randomUUID());
        User unsaved = UserProvider.singleTemplate();
        unsaved.setCreatedBy(UUID.randomUUID().toString());
        unsaved.setCreatedAt(Instant.now());
        user = userRepo.save(unsaved);
        update.setUser(user);

        Session updated = dao.updateById(update, existing.getId());

        assertEquals(user.getId(), updated.getUser().getId());
    }

    @Test
    @DisplayName("updateById throws when session not found")
    void updateNotFound() {

        assertThrows(
                NotFoundException.class, () -> dao.updateById(new Session(), UUID.randomUUID()));
    }

    @Test
    @DisplayName("existsByUniqueProperties works")
    void existsByUniqueProperties() {

        Session existing = dao.save(newSession(null));

        assertTrue(dao.existsByUniqueProperties(existing));

        Session nonExisting = new Session();
        nonExisting.setId(UUID.randomUUID());

        assertFalse(dao.existsByUniqueProperties(nonExisting));
    }

    @Test
    @DisplayName("findSessionByFamilyId returns session")
    void findSessionByFamilyId() {

        Session session = newSession(null);
        dao.save(session);

        Optional<Session> result = dao.findSessionByFamilyId(session.getFamilyId());

        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("findSessionByFamilyId returns empty")
    void findSessionByFamilyIdNotFound() {

        Optional<Session> result = dao.findSessionByFamilyId(UUID.randomUUID());

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByUniqueProperties returns session")
    void findByUniqueProperties() {

        Session saved = dao.save(newSession(null));

        Optional<Session> result =
                dao.findByUniqueProperties(
                        saved.getUser().getId(), saved.getUserAgent(), saved.getClientId(), null);

        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("findByUniqueProperties returns empty when not found")
    void findByUniquePropertiesNotFound() {

        Optional<Session> result =
                dao.findByUniqueProperties(UUID.randomUUID(), "unknown", "client", "ipAddress");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findActiveSessionsByUserOrderByLastSeen respects pagination")
    void findActiveSessionsByUserOrderByLastSeen() {

        Session s1 = dao.save(newSession(null));
        User unsavedSec = UserProvider.alternativeTemplate();
        unsavedSec.setCreatedAt(Instant.now());
        unsavedSec.setCreatedBy(UUID.randomUUID().toString());
        User secondUser = userRepo.save(unsavedSec);
        Session secondSession = newSession(secondUser);
        Session s2 = dao.save(secondSession);

        UUID firstUserId = s1.getUser().getId();

        List<Session> result =
                dao.findActiveSessionsByUserOrderByLastSeen(firstUserId, PageRequest.of(0, 1));

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("revokeSessionByFamilyId revokes only active sessions")
    void revokeSessionByFamilyId() {

        Session session = newSession(null);
        Session saved = dao.save(session);

        dao.revokeSessionByFamilyId(saved.getFamilyId(), now, "security");

        Session db = repo.findById(saved.getId()).orElseThrow();

        assertTrue(db.isRevoked());
        assertEquals("security", db.getRevokeReason());
    }
}
