package com.alpaca.unit.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.alpaca.entity.Session;
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

    private Session existingSession;
    private final UUID sessionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        existingSession = SessionProvider.singleEntity();
        existingSession.setId(sessionId);
    }

    // --- existsByUniqueProperties ---

    @Test
    @DisplayName("Should check existence by ID")
    void existsByUniqueProperties_ChecksRepoById() {
        when(repo.countByUniqueProperties(any(), any(), any(), any())).thenReturn(1L);
        assertTrue(dao.existsByUniqueProperties(existingSession));
        verify(repo).countByUniqueProperties(any(), any(), any(), any());
    }

    // --- Repository Delegation Tests ---

    @Test
    @DisplayName("Should delegate revokeSessionByFamilyId to repo")
    void revokeSessionByFamilyId_DelegatesToRepo() {
        UUID familyId = UUID.randomUUID();
        Instant now = Instant.now();
        String reason = "Security Breach";

        dao.revokeSessionByFamilyId(familyId, now, reason);

        verify(repo).revokeSessionByFamilyId(familyId, now, reason);
    }

    @Test
    @DisplayName("Should delegate findSessionByFamilyId to repo")
    void findSessionByFamilyId_DelegatesToRepo() {
        UUID familyId = UUID.randomUUID();
        when(repo.findSessionByFamilyId(familyId)).thenReturn(Optional.of(existingSession));

        Optional<Session> result = dao.findSessionByFamilyId(familyId);

        assertTrue(result.isPresent());
        verify(repo).findSessionByFamilyId(familyId);
    }

    @Test
    @DisplayName("Should delegate findByUniqueProperties to repo")
    void findByUniqueProperties_DelegatesToRepo() {
        UUID userId = UUID.randomUUID();
        String ua = "Chrome";
        String cid = "client-123";
        String cip = "127.0.0.1";
        when(repo.findByUniqueProperties(userId, ua, cid, cip))
                .thenReturn(Optional.of(existingSession));

        Optional<Session> result = dao.findByUniqueProperties(userId, ua, cid, cip);

        assertTrue(result.isPresent());
        verify(repo).findByUniqueProperties(userId, ua, cid, cip);
    }

    @Test
    @DisplayName("Should delegate findActiveSessionsByUserOrderByLastSeen to repo")
    void findActiveSessionsByUserOrderByLastSeen_DelegatesToRepo() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        List<Session> sessions = List.of(existingSession);

        when(repo.findActiveSessionsByUserOrderByLastSeen(userId, pageable)).thenReturn(sessions);

        List<Session> result = dao.findActiveSessionsByUserOrderByLastSeen(userId, pageable);

        assertEquals(1, result.size());
        verify(repo).findActiveSessionsByUserOrderByLastSeen(userId, pageable);
    }

    @Test
    @DisplayName("existsAllByIds: Should compare input size with repository count")
    void existsAllByIds_Coverage() {
        List<UUID> ids = SessionProvider.listEntities().stream().map(Session::getId).toList();
        when(repo.countByIds(ids)).thenReturn((long) ids.size());
        assertThat(dao.existsAllByIds(ids)).isTrue();

        when(repo.countByIds(ids)).thenReturn(0L);
        assertThat(dao.existsAllByIds(ids)).isFalse();
    }
}
