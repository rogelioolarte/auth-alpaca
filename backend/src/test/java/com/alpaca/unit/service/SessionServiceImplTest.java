package com.alpaca.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.alpaca.entity.Session;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.ExceededSessionsException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IRefreshTokenDAO;
import com.alpaca.persistence.ISessionDAO;
import com.alpaca.persistence.IUserDAO;
import com.alpaca.resources.SessionProvider;
import com.alpaca.resources.UserProvider;
import com.alpaca.service.impl.SessionServiceImpl;
import com.alpaca.utils.UUIDv7Generator;
import java.time.Instant;
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

/** Unit tests for {@link SessionServiceImpl}. */
@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock private ISessionDAO dao;
    @Mock private IUserDAO userDAO;
    @Mock private IRefreshTokenDAO refreshTokenDAO;
    @Mock private UUIDv7Generator uuidv7Generator;

    private SessionServiceImpl service;
    private User user;
    private Session session;
    private final int maxSessions = 2;

    @BeforeEach
    void setUp() {
        service =
                new SessionServiceImpl(
                        dao, userDAO, refreshTokenDAO, uuidv7Generator, maxSessions, false);
        user = UserProvider.singleEntity();
        session = SessionProvider.singleEntity();
    }

    @Test
    void constructor_WhenMaxSessionsInvalid_ThrowsIllegalStateException() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        new SessionServiceImpl(
                                dao, userDAO, refreshTokenDAO, uuidv7Generator, 0, false));
    }

    @Test
    void revokeSessionByFamilyId_DelegatesToDao() {
        UUID familyId = session.getFamilyId();
        Instant now = Instant.now();
        String reason = "logout";

        service.revokeSessionByFamilyId(familyId, now, reason);

        verify(dao).revokeSessionByFamilyId(familyId, now, reason);
    }

    @Test
    void findSessionByFamilyId_DelegatesToDao() {
        UUID familyId = session.getFamilyId();
        when(dao.findSessionByFamilyId(familyId)).thenReturn(Optional.of(session));

        Optional<Session> result = service.findSessionByFamilyId(familyId);

        assertTrue(result.isPresent());
        assertEquals(session, result.get());
    }

    @Test
    void createSession_WhenUserNotFound_ThrowsNotFoundException() {
        UUID userId = user.getId();
        when(userDAO.lockFindUserById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.createSession(userId, "ua", "c", "ip"));
    }

    @Test
    void createSession_WhenSessionExists_ReusesAndRevokesOldTokens() {
        UUID userId = user.getId();
        UUID newFamilyId = UUID.randomUUID();
        String ua = "Mozilla";
        String client = "web";
        String ip = "127.0.0.1";

        when(userDAO.lockFindUserById(userId)).thenReturn(Optional.of(user));
        when(dao.findByUniqueProperties(userId, ua, client, ip)).thenReturn(Optional.of(session));
        when(uuidv7Generator.generate()).thenReturn(newFamilyId);
        when(dao.save(any(Session.class))).thenAnswer(i -> i.getArgument(0));

        Session result = service.createSession(userId, ua, client, ip);

        assertEquals(newFamilyId, result.getFamilyId());
        assertEquals(ip, result.getIpAddress());
        assertFalse(result.isRevoked());
        verify(refreshTokenDAO).revokeFamilyWithReason(any(), any(), eq("new-session-created"));
    }

    @Test
    void createSession_WhenNewSessionAndLimitExceeded_ThrowsExceededSessionsException() {
        UUID userId = user.getId();
        String ua = "Mozilla";
        String client = "web";
        String ip = "127.0.0.1";
        List<Session> activeSessions =
                List.of(new Session(), new Session()); // Size 2 == MAX_SESSIONS

        when(userDAO.lockFindUserById(userId)).thenReturn(Optional.of(user));
        when(dao.findByUniqueProperties(userId, ua, client, ip)).thenReturn(Optional.empty());
        when(dao.findActiveSessionsByUserOrderByLastSeen(eq(userId), any(Pageable.class)))
                .thenReturn(activeSessions);

        assertThrows(
                ExceededSessionsException.class,
                () -> service.createSession(userId, ua, client, ip));
    }

    @Test
    void createSession_WhenInfinityLoginEnabled_RevokesOldestSession() {
        SessionServiceImpl infinityService =
                new SessionServiceImpl(
                        dao, userDAO, refreshTokenDAO, uuidv7Generator, maxSessions, true);
        UUID userId = user.getId();
        UUID newFamilyId = UUID.randomUUID();
        UUID oldestFamilyId = UUID.randomUUID();

        Session oldestSession = new Session();
        oldestSession.setFamilyId(oldestFamilyId);
        List<Session> activeSessions = List.of(oldestSession, new Session());

        when(userDAO.lockFindUserById(userId)).thenReturn(Optional.of(user));
        when(dao.findByUniqueProperties(userId, "ua", "c", "ip")).thenReturn(Optional.empty());
        when(dao.findActiveSessionsByUserOrderByLastSeen(eq(userId), any(Pageable.class)))
                .thenReturn(activeSessions);
        when(uuidv7Generator.generate()).thenReturn(newFamilyId);
        when(dao.save(any(Session.class))).thenAnswer(i -> i.getArgument(0));

        Session result = infinityService.createSession(userId, "ua", "c", "ip");

        assertNotNull(result);
        verify(refreshTokenDAO)
                .revokeFamilyWithReason(eq(oldestFamilyId), any(), eq("new-session-created"));
        verify(dao).revokeSessionByFamilyId(eq(oldestFamilyId), any(), eq("new-session-created"));
    }

    @Test
    void createSession_WhenNewSessionWithinLimit_CreatesSuccessfully() {
        UUID userId = user.getId();
        UUID newFamilyId = UUID.randomUUID();
        String ua = "Chrome";
        String client = "mobile";
        String ip = "192.168.1.1";

        when(userDAO.lockFindUserById(userId)).thenReturn(Optional.of(user));
        when(dao.findByUniqueProperties(userId, ua, client, ip)).thenReturn(Optional.empty());
        when(dao.findActiveSessionsByUserOrderByLastSeen(eq(userId), any(Pageable.class)))
                .thenReturn(Collections.emptyList());
        when(uuidv7Generator.generate()).thenReturn(newFamilyId);
        when(dao.save(any(Session.class))).thenAnswer(i -> i.getArgument(0));

        Session result = service.createSession(userId, ua, client, ip);

        assertEquals(user, result.getUser());
        assertEquals(ua, result.getUserAgent());
        assertEquals(client, result.getClientId());
        assertEquals(ip, result.getIpAddress());
        assertEquals(newFamilyId, result.getFamilyId());
    }

    @Test
    void existsByUniqueProperties_DelegatesToDao() {
        when(dao.existsByUniqueProperties(session)).thenReturn(true);
        boolean exists = service.existsByUniqueProperties(session);
        assertTrue(exists);
    }

    @Test
    void updateByIdShouldThrowBadRequestExceptionWhenSessionIsNull() {
        UUID sessionId = session.getId();

        assertThrows(BadRequestException.class, () -> service.updateById(null, sessionId));

        verify(dao, never()).findById(any(UUID.class));
        verify(dao, never()).save(any(Session.class));
    }

    @Test
    void updateByIdShouldThrowBadRequestExceptionWhenIdIsNull() {
        assertThrows(BadRequestException.class, () -> service.updateById(session, null));

        verify(dao, never()).findById(any(UUID.class));
        verify(dao, never()).save(any(Session.class));
    }

    @Test
    void updateByIdShouldThrowNotFoundExceptionWhenSessionDoesNotExist() {
        UUID sessionId = session.getId();

        when(dao.findById(sessionId)).thenReturn(Optional.empty());

        assertThrowsExactly(
                NotFoundException.class,
                () -> service.updateById(SessionProvider.singleEntity(), sessionId));

        verify(dao).findById(sessionId);
        verify(dao, never()).save(any(Session.class));
    }

    @Test
    void updateByIdShouldUpdateUserWhenUserIsDifferent() {
        Session existingSession = SessionProvider.singleEntity();

        Session incomingSession = SessionProvider.alternativeEntity();

        UUID sessionId = existingSession.getId();

        when(dao.findById(sessionId)).thenReturn(Optional.of(existingSession));
        when(dao.save(existingSession)).thenReturn(existingSession);

        Session result = service.updateById(incomingSession, sessionId);

        assertNotNull(result);
        assertEquals(incomingSession.getUser(), result.getUser());

        verify(dao).findById(sessionId);
        verify(dao).save(existingSession);
    }

    @Test
    void updateByIdShouldNotUpdateUserWhenUserIsNull() {
        Session existingSession = SessionProvider.singleEntity();

        Session incomingSession = SessionProvider.alternativeEntity();
        incomingSession.setUser(null);

        UUID sessionId = existingSession.getId();
        User originalUser = existingSession.getUser();

        when(dao.findById(sessionId)).thenReturn(Optional.of(existingSession));
        when(dao.save(existingSession)).thenReturn(existingSession);

        Session result = service.updateById(incomingSession, sessionId);

        assertNotNull(result);
        assertEquals(originalUser, result.getUser());

        verify(dao).save(existingSession);
    }

    @Test
    void updateByIdShouldNotUpdateUserWhenUserIdIsNull() {
        Session existingSession = SessionProvider.singleEntity();

        Session incomingSession = SessionProvider.alternativeEntity();
        incomingSession.setUser(new User());

        UUID sessionId = existingSession.getId();
        User originalUser = existingSession.getUser();

        when(dao.findById(sessionId)).thenReturn(Optional.of(existingSession));
        when(dao.save(existingSession)).thenReturn(existingSession);

        Session result = service.updateById(incomingSession, sessionId);

        assertNotNull(result);
        assertEquals(originalUser, result.getUser());

        verify(dao).save(existingSession);
    }

    @Test
    void updateByIdShouldNotUpdateUserWhenUserIsEqual() {
        Session existingSession = SessionProvider.singleEntity();

        Session incomingSession = SessionProvider.alternativeEntity();
        incomingSession.setUser(existingSession.getUser());

        UUID sessionId = existingSession.getId();

        when(dao.findById(sessionId)).thenReturn(Optional.of(existingSession));
        when(dao.save(existingSession)).thenReturn(existingSession);

        Session result = service.updateById(incomingSession, sessionId);

        assertNotNull(result);
        assertEquals(existingSession.getUser(), result.getUser());

        verify(dao).save(existingSession);
    }

    @Test
    void updateByIdShouldUpdateFieldsSuccessfully() {
        Session existingSession = SessionProvider.singleEntity();

        Session incomingSession = SessionProvider.alternativeEntity();

        UUID sessionId = existingSession.getId();

        when(dao.findById(sessionId)).thenReturn(Optional.of(existingSession));
        when(dao.save(existingSession)).thenReturn(existingSession);

        Session result = service.updateById(incomingSession, sessionId);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(incomingSession.getFamilyId(), result.getFamilyId()),
                () -> assertEquals(incomingSession.getIpAddress(), result.getIpAddress()),
                () -> assertEquals(incomingSession.getUserAgent(), result.getUserAgent()),
                () -> assertEquals(incomingSession.getClientId(), result.getClientId()),
                () -> assertEquals(incomingSession.isRevoked(), result.isRevoked()),
                () -> assertEquals(incomingSession.getRevokedAt(), result.getRevokedAt()),
                () -> assertEquals(incomingSession.getRevokeReason(), result.getRevokeReason()));

        verify(dao).save(existingSession);
    }

    @Test
    void updateByIdShouldNotUpdateFieldsWhenValuesAreEqualOrNull() {
        Session existingSession = SessionProvider.singleEntity();

        Session incomingSession = SessionProvider.singleEntity();

        incomingSession.setFamilyId(existingSession.getFamilyId());
        incomingSession.setIpAddress(existingSession.getIpAddress());
        incomingSession.setUserAgent(existingSession.getUserAgent());
        incomingSession.setClientId(existingSession.getClientId());
        incomingSession.setRevoked(existingSession.isRevoked());
        incomingSession.setRevokedAt(existingSession.getRevokedAt());
        incomingSession.setRevokeReason(existingSession.getRevokeReason());

        UUID sessionId = existingSession.getId();

        when(dao.findById(sessionId)).thenReturn(Optional.of(existingSession));
        when(dao.save(existingSession)).thenReturn(existingSession);

        Session result = service.updateById(incomingSession, sessionId);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(existingSession.getFamilyId(), result.getFamilyId()),
                () -> assertEquals(existingSession.getIpAddress(), result.getIpAddress()),
                () -> assertEquals(existingSession.getUserAgent(), result.getUserAgent()),
                () -> assertEquals(existingSession.getClientId(), result.getClientId()),
                () -> assertEquals(existingSession.isRevoked(), result.isRevoked()),
                () -> assertEquals(existingSession.getRevokedAt(), result.getRevokedAt()),
                () -> assertEquals(existingSession.getRevokeReason(), result.getRevokeReason()));

        verify(dao).save(existingSession);
    }
}
