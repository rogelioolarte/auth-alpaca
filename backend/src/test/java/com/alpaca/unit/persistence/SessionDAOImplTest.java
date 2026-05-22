package com.alpaca.unit.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alpaca.entity.Session;
import com.alpaca.entity.User;
import com.alpaca.persistence.impl.SessionDAOImpl;
import com.alpaca.repository.SessionRepo;
import com.alpaca.resources.SessionProvider;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/** Unit tests for {@link SessionDAOImpl}. */
@ExtendWith(MockitoExtension.class)
class SessionDAOImplTest {

    @Mock private SessionRepo repo;

    @InjectMocks private SessionDAOImpl dao;

    private Session session;

    @BeforeEach
    void setUp() {
        session = SessionProvider.singleEntity();
        session.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("existsByUniqueProperties should return false when user is null")
    void existsByUniqueProperties_ShouldReturnFalse_WhenUserIsNull() {
        session.setUser(null);

        boolean result = dao.existsByUniqueProperties(session);

        assertFalse(result);
        verifyNoInteractions(repo);
    }

    @Test
    @DisplayName("existsByUniqueProperties should return false when user id is null")
    void existsByUniqueProperties_ShouldReturnFalse_WhenUserIdIsNull() {
        User user = session.getUser();
        user.setId(null);

        boolean result = dao.existsByUniqueProperties(session);

        assertFalse(result);
        verifyNoInteractions(repo);
    }

    @Test
    @DisplayName("existsByUniqueProperties should return false when user agent is blank")
    void existsByUniqueProperties_ShouldReturnFalse_WhenUserAgentIsBlank() {
        session.setUserAgent(" ");

        boolean result = dao.existsByUniqueProperties(session);

        assertFalse(result);
        verifyNoInteractions(repo);
    }

    @Test
    @DisplayName("existsByUniqueProperties should return false when client id is blank")
    void existsByUniqueProperties_ShouldReturnFalse_WhenClientIdIsBlank() {
        session.setClientId("");

        boolean result = dao.existsByUniqueProperties(session);

        assertFalse(result);
        verifyNoInteractions(repo);
    }

    @Test
    @DisplayName("existsByUniqueProperties should return false when ip address is blank")
    void existsByUniqueProperties_ShouldReturnFalse_WhenIpAddressIsBlank() {
        session.setIpAddress(" ");

        boolean result = dao.existsByUniqueProperties(session);

        assertFalse(result);
        verifyNoInteractions(repo);
    }

    @Test
    @DisplayName(
            "existsByUniqueProperties should return true when repository count is greater than"
                    + " zero")
    void existsByUniqueProperties_ShouldReturnTrue_WhenRepositoryCountIsGreaterThanZero() {
        UUID userId = session.getUser().getId();
        String userAgent = session.getUserAgent();
        String clientId = session.getClientId();
        String ipAddress = session.getIpAddress();

        when(repo.countByUniqueProperties(userId, userAgent, clientId, ipAddress)).thenReturn(1L);

        boolean result = dao.existsByUniqueProperties(session);

        assertTrue(result);

        verify(repo).countByUniqueProperties(userId, userAgent, clientId, ipAddress);
    }

    @Test
    @DisplayName("existsByUniqueProperties should return false when repository count is zero")
    void existsByUniqueProperties_ShouldReturnFalse_WhenRepositoryCountIsZero() {
        UUID userId = session.getUser().getId();
        String userAgent = session.getUserAgent();
        String clientId = session.getClientId();
        String ipAddress = session.getIpAddress();

        when(repo.countByUniqueProperties(userId, userAgent, clientId, ipAddress)).thenReturn(0L);

        boolean result = dao.existsByUniqueProperties(session);

        assertFalse(result);

        verify(repo).countByUniqueProperties(userId, userAgent, clientId, ipAddress);
    }

    @Test
    @DisplayName("revokeSessionByFamilyId should delegate to repository")
    void revokeSessionByFamilyId_ShouldDelegateToRepository() {
        UUID familyId = UUID.randomUUID();
        Instant revokedAt = Instant.now();
        String reason = "security-incident";

        dao.revokeSessionByFamilyId(familyId, revokedAt, reason);

        verify(repo).revokeSessionByFamilyId(familyId, revokedAt, reason);
    }

    @Test
    @DisplayName("findSessionByFamilyId should return repository result")
    void findSessionByFamilyId_ShouldReturnRepositoryResult() {
        UUID familyId = UUID.randomUUID();

        when(repo.findSessionByFamilyId(familyId)).thenReturn(Optional.of(session));

        Optional<Session> result = dao.findSessionByFamilyId(familyId);

        assertTrue(result.isPresent());
        assertEquals(session, result.get());

        verify(repo).findSessionByFamilyId(familyId);
    }

    @Test
    @DisplayName("findSessionByFamilyId should return empty optional")
    void findSessionByFamilyId_ShouldReturnEmptyOptional() {
        UUID familyId = UUID.randomUUID();

        when(repo.findSessionByFamilyId(familyId)).thenReturn(Optional.empty());

        Optional<Session> result = dao.findSessionByFamilyId(familyId);

        assertTrue(result.isEmpty());

        verify(repo).findSessionByFamilyId(familyId);
    }

    @Test
    @DisplayName("findByUniqueProperties should return repository result")
    void findByUniqueProperties_ShouldReturnRepositoryResult() {
        UUID userId = session.getUser().getId();
        String userAgent = session.getUserAgent();
        String clientId = session.getClientId();
        String ipAddress = session.getIpAddress();

        when(repo.findByUniqueProperties(userId, userAgent, clientId, ipAddress))
                .thenReturn(Optional.of(session));

        Optional<Session> result =
                dao.findByUniqueProperties(userId, userAgent, clientId, ipAddress);

        assertTrue(result.isPresent());
        assertEquals(session, result.get());

        verify(repo).findByUniqueProperties(userId, userAgent, clientId, ipAddress);
    }

    @Test
    @DisplayName("findByUniqueProperties should return empty optional")
    void findByUniqueProperties_ShouldReturnEmptyOptional() {
        UUID userId = session.getUser().getId();
        String userAgent = session.getUserAgent();
        String clientId = session.getClientId();
        String ipAddress = session.getIpAddress();

        when(repo.findByUniqueProperties(userId, userAgent, clientId, ipAddress))
                .thenReturn(Optional.empty());

        Optional<Session> result =
                dao.findByUniqueProperties(userId, userAgent, clientId, ipAddress);

        assertTrue(result.isEmpty());

        verify(repo).findByUniqueProperties(userId, userAgent, clientId, ipAddress);
    }

    @Test
    @DisplayName("findActiveSessionsByUserOrderByLastSeen should return repository result")
    void findActiveSessionsByUserOrderByLastSeen_ShouldReturnRepositoryResult() {
        UUID userId = session.getUser().getId();
        Pageable pageable = PageRequest.of(0, 10);
        List<Session> expectedSessions = List.of(session);

        when(repo.findActiveSessionsByUserOrderByLastSeen(userId, pageable))
                .thenReturn(expectedSessions);

        List<Session> result = dao.findActiveSessionsByUserOrderByLastSeen(userId, pageable);

        assertThat(result).hasSize(1).containsExactly(session);

        verify(repo).findActiveSessionsByUserOrderByLastSeen(userId, pageable);
    }

    @Test
    @DisplayName("existsAllByIds should return true when all ids exist")
    void existsAllByIds_ShouldReturnTrue_WhenAllIdsExist() {
        List<UUID> ids = SessionProvider.listEntities().stream().map(Session::getId).toList();

        when(repo.countByIds(ids)).thenReturn((long) ids.size());

        boolean result = dao.existsAllByIds(ids);

        assertTrue(result);

        verify(repo).countByIds(ids);
    }

    @Test
    @DisplayName("existsAllByIds should return false when not all ids exist")
    void existsAllByIds_ShouldReturnFalse_WhenNotAllIdsExist() {
        List<UUID> ids = SessionProvider.listEntities().stream().map(Session::getId).toList();

        when(repo.countByIds(ids)).thenReturn((long) ids.size() - 1L);

        boolean result = dao.existsAllByIds(ids);

        assertFalse(result);

        verify(repo).countByIds(ids);
    }
}
