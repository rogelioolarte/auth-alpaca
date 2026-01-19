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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

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
        // Create service instance manually since it has custom constructor
        service = new SessionServiceImpl(dao, userDAO, refreshTokenDAO, uuidv7Generator, 10);
        testUser = UserProvider.singleEntity();
        testSession = SessionProvider.singleEntity();
        testUserId = testUser.getId();
        testFamilyId = testSession.getFamilyId();
    }

    @Test
    void findSessionByFamilyId_existingSession_returnsSession() {
        // Arrange
        when(dao.findSessionByFamilyId(testFamilyId)).thenReturn(Optional.of(testSession));

        // Act
        Optional<Session> result = service.findSessionByFamilyId(testFamilyId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testSession, result.get());
        verify(dao).findSessionByFamilyId(testFamilyId);
    }

    @Test
    void findSessionByFamilyId_nonExistingSession_returnsEmpty() {
        // Arrange
        when(dao.findSessionByFamilyId(testFamilyId)).thenReturn(Optional.empty());

        // Act
        Optional<Session> result = service.findSessionByFamilyId(testFamilyId);

        // Assert
        assertFalse(result.isPresent());
        verify(dao).findSessionByFamilyId(testFamilyId);
    }

    @Test
    void createSession_existingSession_reusesAndRevokesOldFamily() {
        // Arrange
        String userAgent = "Mozilla/5.0";
        String clientId = "web-client";
        String clientIp = "127.0.0.1";
        UUID newFamilyId = UUID.randomUUID();

        when(userDAO.lockFindUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(dao.findByUniqueProperties(testUserId, userAgent, clientId))
                .thenReturn(Optional.of(testSession));
        when(uuidv7Generator.generate()).thenReturn(newFamilyId);
        when(dao.save(any(Session.class))).thenReturn(testSession);

        // Act
        Session result = service.createSession(testUserId, userAgent, clientId, clientIp);

        // Assert
        assertNotNull(result);
        verify(refreshTokenDAO)
                .revokeFamilyWithReason(
                        eq(UUID.fromString("2632eb79-63a4-4213-b905-0ad176f0004b")),
                        any(),
                        eq("new-session-created"));
        verify(dao).save(any(Session.class));
    }

    @Test
    void createSession_newSession_createsNewSession() {
        // Arrange
        String userAgent = "Mozilla/5.0";
        String clientId = "web-client";
        String clientIp = "127.0.0.1";
        UUID newFamilyId = UUID.randomUUID();

        when(userDAO.lockFindUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(dao.findByUniqueProperties(testUserId, userAgent, clientId))
                .thenReturn(Optional.empty());
        when(dao.findActiveSessionsByUserOrderByLastSeen(eq(testUserId), any(Pageable.class)))
                .thenReturn(Collections.emptyList());
        when(uuidv7Generator.generate()).thenReturn(newFamilyId);
        when(dao.save(any(Session.class))).thenReturn(testSession);

        // Act
        Session result = service.createSession(testUserId, userAgent, clientId, clientIp);

        // Assert
        assertNotNull(result);
        verify(dao).save(any(Session.class));
        verify(refreshTokenDAO, never()).revokeFamilyWithReason(any(), any(), anyString());
    }

    @Test
    void createSession_userNotFound_throwsNotFoundException() {
        // Arrange
        String userAgent = "Mozilla/5.0";
        String clientId = "web-client";
        String clientIp = "127.0.0.1";

        when(userDAO.lockFindUserById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception =
                assertThrows(
                        NotFoundException.class,
                        () -> service.createSession(testUserId, userAgent, clientId, clientIp));

        assertTrue(exception.getMessage().contains("User not found"));
    }

    @Test
    void createSession_maxSessionsExceeded_throwsExceededSessionsException() {
        // Arrange
        String userAgent = "Mozilla/5.0";
        String clientId = "web-client";
        String clientIp = "127.0.0.1";
        int maxSessions = 10;

        // Create a list with maxSessions active sessions
        List<Session> activeSessions = Collections.nCopies(maxSessions, testSession);

        when(userDAO.lockFindUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(dao.findByUniqueProperties(testUserId, userAgent, clientId))
                .thenReturn(Optional.empty());
        when(dao.findActiveSessionsByUserOrderByLastSeen(eq(testUserId), any(Pageable.class)))
                .thenReturn(activeSessions);

        // Act & Assert
        ExceededSessionsException exception =
                assertThrows(
                        ExceededSessionsException.class,
                        () -> service.createSession(testUserId, userAgent, clientId, clientIp));

        assertEquals(maxSessions, exception.getMaxOfSessions());
    }

    @Test
    void createSession_updatesSessionProperties() {
        // Arrange
        String userAgent = "New User Agent";
        String clientId = "new-client";
        String clientIp = "192.168.1.1";
        UUID newFamilyId = UUID.randomUUID();

        when(userDAO.lockFindUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(dao.findByUniqueProperties(testUserId, userAgent, clientId))
                .thenReturn(Optional.of(testSession));
        when(uuidv7Generator.generate()).thenReturn(newFamilyId);
        when(dao.save(any(Session.class))).thenReturn(testSession);

        // Act
        service.createSession(testUserId, userAgent, clientId, clientIp);

        // Assert
        verify(dao)
                .save(
                        argThat(
                                session -> {
                                    // Verify that session properties are updated
                                    return !session.isRevoked()
                                            && session.getFamilyId().equals(newFamilyId)
                                            && session.getIpAddress().equals(clientIp);
                                }));
    }
}
