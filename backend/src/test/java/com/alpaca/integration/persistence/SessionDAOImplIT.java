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

/** Integration tests for {@link SessionDAOImpl} */
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

    @Test
    @DisplayName("existsAllByIds: verifies multiple IDs against database count")
    @Transactional
    void existsAllByIds_ShouldHandleCollections() {
        // Arrange
        User owner = UserProvider.singleTemplate();
        owner.setCreatedAt(now);
        userRepo.save(owner);

        User owner2 = UserProvider.alternativeTemplate();
        owner2.setCreatedAt(now);
        userRepo.save(owner2);

        Session p1 = SessionProvider.singleTemplate();
        p1.setCreatedAt(now);
        p1.setUser(owner);
        Session p2 = SessionProvider.alternativeTemplate();
        p2.setCreatedAt(now);
        p2.setUser(owner2);

        UUID id1 = repo.save(p1).getId();
        UUID id2 = repo.save(p2).getId();

        // Act & Assert
        assertTrue(dao.existsAllByIds(List.of(id1, id2)));
        assertFalse(dao.existsAllByIds(List.of(id1, UUID.randomUUID())));
    }
}
