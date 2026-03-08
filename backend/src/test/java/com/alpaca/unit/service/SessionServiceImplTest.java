package com.alpaca.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.alpaca.entity.Session;
import com.alpaca.entity.User;
import com.alpaca.exception.ExceededSessionsException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IRefreshTokenDAO;
import com.alpaca.persistence.ISessionDAO;
import com.alpaca.persistence.IUserDAO;
import com.alpaca.resources.SessionProvider;
import com.alpaca.resources.UserProvider;
import com.alpaca.service.impl.SessionServiceImpl;
import com.alpaca.utils.UUIDv7Generator;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

/** Unit tests for {@link SessionServiceImpl} */
@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock private ISessionDAO dao;
    @Mock private IUserDAO userDAO;
    @Mock private IRefreshTokenDAO refreshTokenDAO;
    @Mock private UUIDv7Generator uuidv7Generator;

    private SessionServiceImpl service;

    private User testUser;
    private Session testSession;
    private UUID testUserId;
    private UUID testFamilyId;

    @BeforeEach
    void setUp() {
        // normal valid maxSessionsPerUser = 10
        service = new SessionServiceImpl(dao, userDAO, refreshTokenDAO, uuidv7Generator, 10);
        testUser = UserProvider.singleEntity();
        testSession = SessionProvider.singleEntity();
        testUserId = testUser.getId();
        testFamilyId = testSession.getFamilyId();
    }

    @AfterEach
    void tearDown() {
        clearInvocations(dao, userDAO, refreshTokenDAO, uuidv7Generator);
    }

    // -------------------------
    // Constructor validation
    // -------------------------
    @Test
    void constructor_whenMaxSessionsLessThanOne_throwsIllegalState() {
        IllegalStateException ex =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                new SessionServiceImpl(
                                        dao, userDAO, refreshTokenDAO, uuidv7Generator, 0));
        assertTrue(ex.getMessage().contains("security.max.session.per.user must be >= 1"));
    }

    // -------------------------
    // Simple delegations
    // -------------------------
    @Test
    void revokeSessionByFamilyId_delegatesToDao() {
        UUID family = UUID.randomUUID();
        service.revokeSessionByFamilyId(family, java.time.Instant.EPOCH, "reason");
        verify(dao).revokeSessionByFamilyId(eq(family), eq(java.time.Instant.EPOCH), eq("reason"));
    }

    @Test
    void findSessionByFamilyId_delegatesToDao() {
        when(dao.findSessionByFamilyId(testFamilyId)).thenReturn(Optional.of(testSession));
        Optional<Session> found = service.findSessionByFamilyId(testFamilyId);
        assertTrue(found.isPresent());
        assertEquals(testSession, found.get());
        verify(dao).findSessionByFamilyId(testFamilyId);
    }

    // -------------------------
    // createSession - existing session reuse
    // -------------------------
    @Test
    void createSession_existingSession_reusesAndRevokesOldFamily_andUpdatesFields() {
        // Arrange
        String userAgent = "Mozilla/5.0";
        String clientId = "web-client";
        String clientIp = "127.0.0.1";
        UUID newFamilyId = UUID.randomUUID();
        UUID oldFamilyId = testSession.getFamilyId();

        // ensure user exists
        when(userDAO.lockFindUserById(testUserId)).thenReturn(Optional.of(testUser));
        // dao returns an existing session (from same device)
        when(dao.findByUniqueProperties(testUserId, userAgent, clientId))
                .thenReturn(Optional.of(testSession));
        when(uuidv7Generator.generate()).thenReturn(newFamilyId);
        when(dao.save(any(Session.class))).thenReturn(testSession);

        // Act
        Session result = service.createSession(testUserId, userAgent, clientId, clientIp);

        // Assert basic
        assertNotNull(result);
        // verify the DAO saved the session
        verify(dao).save(any(Session.class));
        // verify refresh tokens revoked for previous family id
        verify(refreshTokenDAO)
                .revokeFamilyWithReason(eq(oldFamilyId), any(), eq("new-session-created"));

        // verify that saved session has updated family id and ip address (captured)
        ArgumentCaptor<Session> cap = ArgumentCaptor.forClass(Session.class);
        verify(dao).save(cap.capture());
        Session saved = cap.getValue();
        assertEquals(newFamilyId, saved.getFamilyId());
        assertEquals(clientIp, saved.getIpAddress());
        assertFalse(saved.isRevoked());
    }

    @Test
    void
            createSession_existingSession_whenGeneratorReturnsSameFamily_doesNotChangeFamilyButStillRevokesOldTokens() {
        // Arrange: generator returns same family id as existing session
        String userAgent = "Mozilla/5.0";
        String clientId = "web-client";
        String clientIp = "1.2.3.4";
        UUID oldFamilyId = testSession.getFamilyId();

        when(userDAO.lockFindUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(dao.findByUniqueProperties(testUserId, userAgent, clientId))
                .thenReturn(Optional.of(testSession));
        // generator returns same family id
        when(uuidv7Generator.generate()).thenReturn(oldFamilyId);
        when(dao.save(any(Session.class))).thenReturn(testSession);

        service.createSession(testUserId, userAgent, clientId, clientIp);

        // should still call revokeFamilyWithReason (all previous refresh tokens revoked)
        verify(refreshTokenDAO)
                .revokeFamilyWithReason(eq(oldFamilyId), any(), eq("new-session-created"));

        // family id remains equal (generator returned same value)
        ArgumentCaptor<Session> cap = ArgumentCaptor.forClass(Session.class);
        verify(dao).save(cap.capture());
        Session saved = cap.getValue();
        assertEquals(oldFamilyId, saved.getFamilyId());
        assertEquals(clientIp, saved.getIpAddress());
    }

    // -------------------------
    // createSession - new session creation
    // -------------------------
    @Test
    void createSession_newSession_createsWithUserAndProperties_andDoesNotRevokeTokens() {
        // Arrange
        String userAgent = "Mozilla/5.0";
        String clientId = "web-client";
        String clientIp = "127.0.0.1";
        UUID newFamilyId = UUID.randomUUID();

        when(userDAO.lockFindUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(dao.findByUniqueProperties(testUserId, userAgent, clientId))
                .thenReturn(Optional.empty());
        // no active sessions
        when(dao.findActiveSessionsByUserOrderByLastSeen(eq(testUserId), any(Pageable.class)))
                .thenReturn(Collections.emptyList());
        when(uuidv7Generator.generate()).thenReturn(newFamilyId);
        when(dao.save(any(Session.class)))
                .thenAnswer(inv -> inv.getArgument(0)); // return same session saved

        Session created = service.createSession(testUserId, userAgent, clientId, clientIp);

        assertNotNull(created);
        // verify user was attached and properties set
        verify(dao).save(any(Session.class));
        ArgumentCaptor<Session> cap = ArgumentCaptor.forClass(Session.class);
        verify(dao).save(cap.capture());
        Session saved = cap.getValue();
        assertNotNull(saved.getUser());
        assertEquals(testUserId, saved.getUser().getId());
        assertEquals(userAgent, saved.getUserAgent());
        assertEquals(clientId, saved.getClientId());
        assertEquals(clientIp, saved.getIpAddress());
        assertEquals(newFamilyId, saved.getFamilyId());
        assertFalse(saved.isRevoked());

        // ensure refreshTokenDAO never called for new session creation
        verify(refreshTokenDAO, never()).revokeFamilyWithReason(any(), any(), anyString());
    }

    // -------------------------
    // createSession - user not found
    // -------------------------
    @Test
    void createSession_whenUserMissing_throwsNotFound() {
        String userAgent = "UA";
        String clientId = "client";
        String clientIp = "ip";

        when(userDAO.lockFindUserById(testUserId)).thenReturn(Optional.empty());

        NotFoundException ex =
                assertThrows(
                        NotFoundException.class,
                        () -> service.createSession(testUserId, userAgent, clientId, clientIp));
        assertTrue(ex.getMessage().contains("User not found"));
    }

    // -------------------------
    // createSession - exceeded sessions
    // -------------------------
    @Test
    void createSession_whenActiveSessionsExceedMax_throwsExceededSessions() {
        String userAgent = "UA";
        String clientId = "client";
        String clientIp = "ip";
        int maxSessions = 10;

        // create list with size == maxSessions (service created with maxSessions=10)
        List<Session> activeSessions = Collections.nCopies(maxSessions, testSession);

        when(userDAO.lockFindUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(dao.findByUniqueProperties(testUserId, userAgent, clientId))
                .thenReturn(Optional.empty());
        when(dao.findActiveSessionsByUserOrderByLastSeen(eq(testUserId), any(Pageable.class)))
                .thenReturn(activeSessions);

        ExceededSessionsException ex =
                assertThrows(
                        ExceededSessionsException.class,
                        () -> service.createSession(testUserId, userAgent, clientId, clientIp));
        assertEquals(maxSessions, ex.getMaxOfSessions());
    }

    // -------------------------
    // Misc - ensure method call order for existing session branch
    // -------------------------
    @Test
    void createSession_existingSession_invokesRevokeThenSave_inOrder() {
        String userAgent = "UA";
        String clientId = "c";
        String clientIp = "1.2.3.4";
        UUID oldFamilyId = testSession.getFamilyId();

        when(userDAO.lockFindUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(dao.findByUniqueProperties(testUserId, userAgent, clientId))
                .thenReturn(Optional.of(testSession));
        when(uuidv7Generator.generate()).thenReturn(UUID.randomUUID());
        when(dao.save(any(Session.class))).thenReturn(testSession);

        service.createSession(testUserId, userAgent, clientId, clientIp);

        InOrder inOrder = inOrder(refreshTokenDAO, dao);
        inOrder.verify(refreshTokenDAO)
                .revokeFamilyWithReason(eq(oldFamilyId), any(), eq("new-session-created"));
        inOrder.verify(dao).save(any(Session.class));
    }
}
