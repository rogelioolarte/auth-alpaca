package com.alpaca.unit.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alpaca.entity.Session;
import com.alpaca.entity.User;
import com.alpaca.exception.NotFoundException;
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

    // --- updateById Tests ---

    @Test
    @DisplayName("Should throw NotFoundException when session does not exist")
    void updateById_WhenNotFound_ThrowsException() {
        when(repo.findById(sessionId)).thenReturn(Optional.empty());
        Session updateData = new Session();

        assertThrows(NotFoundException.class, () -> dao.updateById(updateData, sessionId));
        verify(repo).findById(sessionId);
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("Should update all fields when valid and different data is provided")
    void updateById_WhenDataIsNew_UpdatesAllFields() {
        // Arrange
        User oldUser = new User();
        oldUser.setId(UUID.randomUUID());
        existingSession.setUser(oldUser);
        existingSession.setFamilyId(UUID.randomUUID());
        existingSession.setRevoked(false);

        User newUser = new User();
        newUser.setId(UUID.randomUUID());

        UUID newFamilyId = UUID.randomUUID();
        Instant now = Instant.now();

        Session updateData = new Session();
        updateData.setUser(newUser);
        updateData.setFamilyId(newFamilyId);
        updateData.setIpAddress("192.168.1.1");
        updateData.setUserAgent("Mozilla/5.0");
        updateData.setClientId("web-client");
        updateData.setRevoked(true);
        updateData.setRevokedAt(now);
        updateData.setRevokeReason("Logout");

        when(repo.findById(sessionId)).thenReturn(Optional.of(existingSession));
        when(repo.save(any(Session.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Session result = dao.updateById(updateData, sessionId);

        // Assert
        assertAll(
                () -> assertEquals(newUser.getId(), result.getUser().getId()),
                () -> assertEquals(newFamilyId, result.getFamilyId()),
                () -> assertEquals("192.168.1.1", result.getIpAddress()),
                () -> assertEquals("Mozilla/5.0", result.getUserAgent()),
                () -> assertEquals("web-client", result.getClientId()),
                () -> assertTrue(result.isRevoked()),
                () -> assertEquals(now, result.getRevokedAt()),
                () -> assertEquals("Logout", result.getRevokeReason()));
        verify(repo).save(existingSession);
    }

    @Test
    @DisplayName("Should skip updates for null, blank or identical values")
    void updateById_WhenDataIsRedundant_SkipsUpdates() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        existingSession.setUser(user);
        existingSession.setIpAddress("127.0.0.1");
        existingSession.setRevoked(true);

        Session updateData = new Session();
        // Case 1: User ID is same
        User sameUser = new User();
        sameUser.setId(userId);
        updateData.setUser(sameUser);
        // Case 2: Text fields are null/blank
        updateData.setIpAddress("");
        updateData.setClientId(null);
        // Case 3: Boolean is identical
        updateData.setRevoked(true);

        when(repo.findById(sessionId)).thenReturn(Optional.of(existingSession));
        when(repo.save(any(Session.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Session result = dao.updateById(updateData, sessionId);

        // Assert
        assertEquals("127.0.0.1", result.getIpAddress());
        assertSame(user, result.getUser());
        verify(repo).save(existingSession);
    }

    @Test
    @DisplayName("Should update User when current user is null and new user ID is provided")
    void updateById_WhenCurrentUserIsNull_UpdatesUser() {
        existingSession.setUser(null);

        User newUser = new User();
        newUser.setId(UUID.randomUUID());
        Session updateData = new Session();
        updateData.setUser(newUser);

        when(repo.findById(sessionId)).thenReturn(Optional.of(existingSession));
        when(repo.save(any(Session.class))).thenAnswer(i -> i.getArgument(0));

        Session result = dao.updateById(updateData, sessionId);

        assertEquals(newUser.getId(), result.getUser().getId());
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
