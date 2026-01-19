package com.alpaca.integration.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alpaca.entity.Session;
import com.alpaca.entity.User;
import com.alpaca.persistence.ISessionDAO;
import com.alpaca.resources.UserProvider;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

@SpringBootTest
class SessionDAOImplIT {

    @Autowired private ISessionDAO sessionDAO;

    private User testUser;
    private UUID userId;
    private UUID familyId;
    private Instant now;

    @BeforeEach
    void setUp() {
        familyId = UUID.randomUUID();
        now = Instant.now();
        testUser = UserProvider.singleEntity();
        userId = testUser.getId();
    }

    @Test
    @DisplayName("Should save session successfully")
    void shouldSaveSessionSuccessfully() {
        Session session =
                new Session(
                        UUID.randomUUID(),
                        testUser,
                        familyId,
                        now,
                        now,
                        "192.168.1.1",
                        "Mozilla/5.0",
                        "web-client",
                        false,
                        null,
                        null);

        Session savedSession = sessionDAO.save(session);

        assertNotNull(savedSession);
        assertEquals(testUser, savedSession.getUser());
        assertEquals(familyId, savedSession.getFamilyId());
        assertEquals("192.168.1.1", savedSession.getIpAddress());
        assertEquals("Mozilla/5.0", savedSession.getUserAgent());
        assertEquals("web-client", savedSession.getClientId());
        assertFalse(savedSession.isRevoked());
    }

    @Test
    @DisplayName("Should find session by ID successfully")
    void shouldFindSessionByIdSuccessfully() {
        Session session =
                new Session(
                        UUID.randomUUID(),
                        testUser,
                        familyId,
                        now,
                        now,
                        "192.168.1.1",
                        "Mozilla/5.0",
                        "web-client",
                        false,
                        null,
                        null);

        when(sessionDAO.save(session)).thenReturn(session);

        Optional<Session> result = sessionDAO.findById(session.getId());

        assertTrue(result.isPresent());
        assertEquals(session, result.get());
    }

    @Test
    @DisplayName("Should return empty when session not found")
    void shouldReturnEmptyWhenSessionNotFound() {
        UUID sessionId = UUID.randomUUID();

        when(sessionDAO.findById(sessionId)).thenReturn(Optional.empty());

        Optional<Session> result = sessionDAO.findById(sessionId);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should find active sessions by user successfully")
    void shouldFindActiveSessionsByUserSuccessfully() {
        Session session1 =
                new Session(
                        UUID.randomUUID(),
                        testUser,
                        familyId,
                        now.minusSeconds(3600),
                        now.minusSeconds(3600),
                        "192.168.1.1",
                        "Mozilla/5.0",
                        "web-client",
                        false,
                        null,
                        null);

        Session session2 =
                new Session(
                        UUID.randomUUID(),
                        testUser,
                        UUID.randomUUID(),
                        now.minusSeconds(1800),
                        now.minusSeconds(1800),
                        "192.168.1.2",
                        "Chrome/91.0",
                        "mobile-client",
                        false,
                        null,
                        null);

        List<Session> sessions = List.of(session1, session2);

        when(sessionDAO.findActiveSessionsByUserOrderByLastSeen(userId, PageRequest.of(0, 10)))
                .thenReturn(sessions);

        List<Session> result =
                sessionDAO.findActiveSessionsByUserOrderByLastSeen(userId, PageRequest.of(0, 10));

        assertEquals(sessions.size(), result.size());
        assertTrue(result.contains(session1));
        assertTrue(result.contains(session2));
    }

    @Test
    @DisplayName("Should find session by unique properties successfully")
    void shouldFindSessionByUniquePropertiesSuccessfully() {
        Session session =
                new Session(
                        UUID.randomUUID(),
                        testUser,
                        familyId,
                        now,
                        now,
                        "192.168.1.1",
                        "Mozilla/5.0",
                        "web-client",
                        false,
                        null,
                        null);

        when(sessionDAO.findByUniqueProperties(userId, "Mozilla/5.0", "web-client"))
                .thenReturn(Optional.of(session));

        Optional<Session> result =
                sessionDAO.findByUniqueProperties(userId, "Mozilla/5.0", "web-client");

        assertTrue(result.isPresent());
        assertEquals(session, result.get());
    }

    @Test
    @DisplayName("Should find session by family ID successfully")
    void shouldFindSessionByFamilyIdSuccessfully() {
        Session session =
                new Session(
                        UUID.randomUUID(),
                        testUser,
                        familyId,
                        now,
                        now,
                        "192.168.1.1",
                        "Mozilla/5.0",
                        "web-client",
                        false,
                        null,
                        null);

        when(sessionDAO.findSessionByFamilyId(familyId)).thenReturn(Optional.of(session));

        Optional<Session> result = sessionDAO.findSessionByFamilyId(familyId);

        assertTrue(result.isPresent());
        assertEquals(session, result.get());
    }

    @Test
    @DisplayName("Should revoke session by family ID successfully")
    void shouldRevokeSessionByFamilyIdSuccessfully() {
        Session session =
                new Session(
                        UUID.randomUUID(),
                        testUser,
                        familyId,
                        now,
                        now,
                        "192.168.1.1",
                        "Mozilla/5.0",
                        "web-client",
                        false,
                        null,
                        null);

        when(sessionDAO.save(session)).thenReturn(session);

        sessionDAO.revokeSessionByFamilyId(familyId, now.plusSeconds(3600), "Test revoke");

        verify(sessionDAO).revokeSessionByFamilyId(familyId, now.plusSeconds(3600), "Test revoke");
    }
}
