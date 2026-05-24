package com.alpaca.unit.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alpaca.entity.Session;
import com.alpaca.entity.User;
import com.alpaca.persistence.impl.SessionDAOImpl;
import com.alpaca.repository.SessionRepo;
import com.alpaca.resources.SessionProvider;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link SessionDAOImpl} implementation. */
@ExtendWith(MockitoExtension.class)
class SessionDAOImplTest {

    @Mock private SessionRepo repo;

    @InjectMocks private SessionDAOImpl dao;

    private Session session;
    private UUID sessionId;
    private UUID userId;
    private UUID familyId;
    private String userAgent;
    private String clientId;
    private String ipAddress;

    @BeforeEach
    void setUp() {
        session = SessionProvider.singleEntity();

        sessionId = UUID.randomUUID();
        familyId = UUID.randomUUID();

        session.setId(sessionId);
        session.setFamilyId(familyId);

        userId = session.getUser().getId();
        userAgent = session.getUserAgent();
        clientId = session.getClientId();
        ipAddress = session.getIpAddress();
    }

    @Test
    void existsByUniqueProperties_WhenUserIsNull_ReturnsFalse() {
        session.setUser(null);

        boolean result = dao.existsByUniqueProperties(session);

        assertFalse(result);

        verifyNoInteractions(repo);
    }

    @Test
    void existsByUniqueProperties_WhenUserIdIsNull_ReturnsFalse() {
        User user = session.getUser();
        user.setId(null);

        boolean result = dao.existsByUniqueProperties(session);

        assertFalse(result);

        verifyNoInteractions(repo);
    }

    @Test
    void existsByUniqueProperties_WhenUserAgentIsBlank_ReturnsFalse() {
        session.setUserAgent(" ");

        boolean result = dao.existsByUniqueProperties(session);

        assertFalse(result);

        verifyNoInteractions(repo);
    }

    @Test
    void existsByUniqueProperties_WhenClientIdIsBlank_ReturnsFalse() {
        session.setClientId("");

        boolean result = dao.existsByUniqueProperties(session);

        assertFalse(result);

        verifyNoInteractions(repo);
    }

    @Test
    void existsByUniqueProperties_WhenIpAddressIsBlank_ReturnsFalse() {
        session.setIpAddress(" ");

        boolean result = dao.existsByUniqueProperties(session);

        assertFalse(result);

        verifyNoInteractions(repo);
    }

    @Test
    void existsByUniqueProperties_WhenCountGreaterThanZero_ReturnsTrue() {
        when(repo.countByUniqueProperties(userId, userAgent, clientId, ipAddress)).thenReturn(1L);

        boolean result = dao.existsByUniqueProperties(session);

        assertTrue(result);

        verify(repo).countByUniqueProperties(userId, userAgent, clientId, ipAddress);
    }

    @Test
    void existsByUniqueProperties_WhenCountIsZero_ReturnsFalse() {
        when(repo.countByUniqueProperties(userId, userAgent, clientId, ipAddress)).thenReturn(0L);

        boolean result = dao.existsByUniqueProperties(session);

        assertFalse(result);

        verify(repo).countByUniqueProperties(userId, userAgent, clientId, ipAddress);
    }

    @Test
    void revokeSessionByFamilyId_DelegatesToRepository() {
        Instant revokedAt = Instant.now();
        String reason = "manual-revocation";

        dao.revokeSessionByFamilyId(familyId, revokedAt, reason);

        verify(repo).revokeSessionByFamilyId(familyId, revokedAt, reason);
    }

    @Test
    void findByIdAndUserId_WhenSessionExists_ReturnsSession() {
        when(repo.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));

        Optional<Session> result = dao.findByIdAndUserId(sessionId, userId);

        assertTrue(result.isPresent());
        assertEquals(session, result.get());

        verify(repo).findByIdAndUserId(sessionId, userId);
    }

    @Test
    void findByIdAndUserId_WhenSessionDoesNotExist_ReturnsEmptyOptional() {
        when(repo.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.empty());

        Optional<Session> result = dao.findByIdAndUserId(sessionId, userId);

        assertTrue(result.isEmpty());

        verify(repo).findByIdAndUserId(sessionId, userId);
    }

    @Test
    void findSessionByFamilyId_WhenSessionExists_ReturnsSession() {
        when(repo.findSessionByFamilyId(familyId)).thenReturn(Optional.of(session));

        Optional<Session> result = dao.findSessionByFamilyId(familyId);

        assertTrue(result.isPresent());
        assertEquals(session, result.get());

        verify(repo).findSessionByFamilyId(familyId);
    }

    @Test
    void findSessionByFamilyId_WhenSessionDoesNotExist_ReturnsEmptyOptional() {
        when(repo.findSessionByFamilyId(familyId)).thenReturn(Optional.empty());

        Optional<Session> result = dao.findSessionByFamilyId(familyId);

        assertTrue(result.isEmpty());

        verify(repo).findSessionByFamilyId(familyId);
    }

    @Test
    void findByUniqueProperties_WhenSessionExists_ReturnsSession() {
        when(repo.findByUniqueProperties(userId, userAgent, clientId, ipAddress))
                .thenReturn(Optional.of(session));

        Optional<Session> result =
                dao.findByUniqueProperties(userId, userAgent, clientId, ipAddress);

        assertTrue(result.isPresent());
        assertEquals(session, result.get());

        verify(repo).findByUniqueProperties(userId, userAgent, clientId, ipAddress);
    }

    @Test
    void findByUniqueProperties_WhenSessionDoesNotExist_ReturnsEmptyOptional() {
        when(repo.findByUniqueProperties(userId, userAgent, clientId, ipAddress))
                .thenReturn(Optional.empty());

        Optional<Session> result =
                dao.findByUniqueProperties(userId, userAgent, clientId, ipAddress);

        assertTrue(result.isEmpty());

        verify(repo).findByUniqueProperties(userId, userAgent, clientId, ipAddress);
    }

    @Test
    void findFirstActiveSessionForUpdate_WhenSessionExists_ReturnsSession() {
        when(repo.findFirstActiveSessionForUpdate(userId)).thenReturn(Optional.of(session));

        Optional<Session> result = dao.findFirstActiveSessionForUpdate(userId);

        assertTrue(result.isPresent());
        assertEquals(session, result.get());

        verify(repo).findFirstActiveSessionForUpdate(userId);
    }

    @Test
    void findFirstActiveSessionForUpdate_WhenSessionDoesNotExist_ReturnsEmptyOptional() {
        when(repo.findFirstActiveSessionForUpdate(userId)).thenReturn(Optional.empty());

        Optional<Session> result = dao.findFirstActiveSessionForUpdate(userId);

        assertTrue(result.isEmpty());

        verify(repo).findFirstActiveSessionForUpdate(userId);
    }

    @Test
    void countByUserIdAndRevokedFalse_ReturnsRepositoryCount() {
        long expectedCount = 5L;

        when(repo.countByUserIdAndRevokedFalse(userId)).thenReturn(expectedCount);

        long result = dao.countByUserIdAndRevokedFalse(userId);

        assertEquals(expectedCount, result);

        verify(repo).countByUserIdAndRevokedFalse(userId);
    }

    @Test
    void revokeSessionsByUserId_DelegatesToRepository() {
        Instant revokedAt = Instant.now();
        String reason = "security-incident";

        dao.revokeSessionsByUserId(userId, revokedAt, reason);

        verify(repo).revokeSessionsByUserId(userId, revokedAt, reason);
    }

    @Test
    void existsAllByIds_WhenAllIdsExist_ReturnsTrue() {
        Collection<UUID> ids = SessionProvider.listEntities().stream().map(Session::getId).toList();

        when(repo.countByIds(ids)).thenReturn((long) ids.size());

        boolean result = dao.existsAllByIds(ids);

        assertTrue(result);

        verify(repo).countByIds(ids);
    }

    @Test
    void existsAllByIds_WhenNotAllIdsExist_ReturnsFalse() {
        Collection<UUID> ids = SessionProvider.listEntities().stream().map(Session::getId).toList();

        when(repo.countByIds(ids)).thenReturn((long) ids.size() - 1L);

        boolean result = dao.existsAllByIds(ids);

        assertFalse(result);

        verify(repo).countByIds(ids);
    }

    @Test
    void findAllByUserId_ReturnsPagedSessions() {
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(0, 10);

        org.springframework.data.domain.Page<Session> expectedPage =
                new org.springframework.data.domain.PageImpl<>(
                        SessionProvider.listEntities(),
                        pageable,
                        SessionProvider.listEntities().size());

        when(repo.findAllByUserId(userId, pageable)).thenReturn(expectedPage);

        org.springframework.data.domain.Page<Session> result =
                dao.findAllByUserId(userId, pageable);

        assertNotNull(result);
        assertEquals(expectedPage, result);
        assertEquals(SessionProvider.listEntities().size(), result.getContent().size());

        verify(repo).findAllByUserId(userId, pageable);
    }
}
