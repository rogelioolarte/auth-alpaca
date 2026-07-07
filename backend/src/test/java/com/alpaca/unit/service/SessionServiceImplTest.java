package com.alpaca.unit.service;

import com.alpaca.entity.Session;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.ExceededSessionsException;
import com.alpaca.exception.ForbiddenException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IRefreshTokenDAO;
import com.alpaca.persistence.ISessionDAO;
import com.alpaca.persistence.IUserDAO;
import com.alpaca.resources.provider.SessionProvider;
import com.alpaca.resources.provider.UserProvider;
import com.alpaca.service.impl.SessionServiceImpl;
import com.alpaca.utils.UUIDv7Generator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Unit tests for {@link SessionServiceImpl}. */
@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    private ISessionDAO dao;
    private IUserDAO userDAO;
    private IRefreshTokenDAO refreshTokenDAO;
    private UUIDv7Generator uuidv7Generator;

    private SessionServiceImpl service;

    private User user;
    private Session session;

    @BeforeEach
    void setUp() {
        dao = mock(ISessionDAO.class);
        userDAO = mock(IUserDAO.class);
        refreshTokenDAO = mock(IRefreshTokenDAO.class);
        uuidv7Generator = mock(UUIDv7Generator.class);

        service = new SessionServiceImpl(dao, userDAO, refreshTokenDAO, uuidv7Generator, 2, false);

        user = UserProvider.singleEntity();
        session = SessionProvider.singleEntity();
    }

    @Test
    void constructorShouldThrowIllegalStateExceptionWhenMaxSessionsPerUserIsInvalid() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        new SessionServiceImpl(
                                dao, userDAO, refreshTokenDAO, uuidv7Generator, 0, false));
    }

    @Test
    void revokeSessionByFamilyIdShouldDelegateToDao() {
        UUID familyId = session.getFamilyId();
        Instant revokedAt = Instant.now();
        String reason = session.getRevokeReason();

        service.revokeSessionByFamilyId(familyId, revokedAt, reason);

        verify(dao).revokeSessionByFamilyId(familyId, revokedAt, reason);
    }

    @Test
    void findSessionByFamilyIdShouldReturnSessionWhenExists() {
        UUID familyId = session.getFamilyId();

        when(dao.findSessionByFamilyId(familyId)).thenReturn(Optional.of(session));

        Optional<Session> result = service.findSessionByFamilyId(familyId);

        assertTrue(result.isPresent());
        assertEquals(session, result.get());

        verify(dao).findSessionByFamilyId(familyId);
    }

    @Test
    void findSessionByFamilyIdShouldReturnEmptyOptionalWhenSessionDoesNotExist() {
        UUID familyId = session.getFamilyId();

        when(dao.findSessionByFamilyId(familyId)).thenReturn(Optional.empty());

        Optional<Session> result = service.findSessionByFamilyId(familyId);

        assertTrue(result.isEmpty());

        verify(dao).findSessionByFamilyId(familyId);
    }

    @Test
    void createSessionShouldThrowNotFoundExceptionWhenUserDoesNotExist() {
        UUID userId = user.getId();

        when(userDAO.lockFindUserById(userId)).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> service.createSession(userId, "user-agent", "client-id", "ip-address"));

        verify(userDAO).lockFindUserById(userId);
        verifyNoInteractions(refreshTokenDAO);
    }

    @Test
    void createSessionShouldReuseExistingSessionAndRotateFamilyId() {
        UUID userId = user.getId();
        UUID oldFamilyId = session.getFamilyId();
        UUID newFamilyId = UUID.randomUUID();

        String userAgent = "Mozilla";
        String clientId = "web";
        String ipAddress = "127.0.0.1";

        when(userDAO.lockFindUserById(userId)).thenReturn(Optional.of(user));
        when(dao.findByUniqueProperties(userId, userAgent, clientId, ipAddress))
                .thenReturn(Optional.of(session));
        when(uuidv7Generator.generate()).thenReturn(newFamilyId);
        when(dao.save(session)).thenReturn(session);

        Session result = service.createSession(userId, userAgent, clientId, ipAddress);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(newFamilyId, result.getFamilyId()),
                () -> assertEquals(ipAddress, result.getIpAddress()),
                () -> assertFalse(result.isRevoked()),
                () -> assertNotNull(result.getLastSeenAt()));

        verify(refreshTokenDAO)
                .revokeFamilyWithReason(
                        eq(oldFamilyId), any(Instant.class), eq("new-session-created"));

        verify(dao).save(session);
    }

    @Test
    void
            createSessionShouldThrowExceededSessionsExceptionWhenLimitIsReachedAndInfinityLoginDisabled() {
        UUID userId = user.getId();

        String userAgent = "Mozilla";
        String clientId = "web";
        String ipAddress = "127.0.0.1";

        when(userDAO.lockFindUserById(userId)).thenReturn(Optional.of(user));
        when(dao.findByUniqueProperties(userId, userAgent, clientId, ipAddress))
                .thenReturn(Optional.empty());
        when(dao.countByUserIdAndRevokedFalse(userId)).thenReturn(2L);
        when(dao.findFirstActiveSessionForUpdate(userId)).thenReturn(Optional.of(session));

        assertThrows(
                ExceededSessionsException.class,
                () -> service.createSession(userId, userAgent, clientId, ipAddress));

        verify(dao).countByUserIdAndRevokedFalse(userId);
        verify(dao).findFirstActiveSessionForUpdate(userId);
        verify(dao, never()).save(any(Session.class));
    }

    @Test
    void createSessionShouldRevokeOldestSessionWhenInfinityLoginEnabledAndLimitIsReached() {
        SessionServiceImpl infinityLoginService =
                new SessionServiceImpl(dao, userDAO, refreshTokenDAO, uuidv7Generator, 2, true);

        UUID userId = user.getId();
        UUID newFamilyId = UUID.randomUUID();

        String userAgent = "Mozilla";
        String clientId = "web";
        String ipAddress = "127.0.0.1";

        Session oldestSession = new Session();
        oldestSession.setFamilyId(UUID.randomUUID());

        when(userDAO.lockFindUserById(userId)).thenReturn(Optional.of(user));
        when(dao.findByUniqueProperties(userId, userAgent, clientId, ipAddress))
                .thenReturn(Optional.empty());
        when(dao.countByUserIdAndRevokedFalse(userId)).thenReturn(2L);
        when(dao.findFirstActiveSessionForUpdate(userId)).thenReturn(Optional.of(oldestSession));
        when(uuidv7Generator.generate()).thenReturn(newFamilyId);

        Session savedSession = new Session();

        when(dao.save(any(Session.class))).thenReturn(savedSession);

        Session result = infinityLoginService.createSession(userId, userAgent, clientId, ipAddress);

        assertNotNull(result);

        verify(refreshTokenDAO)
                .revokeFamilyWithReason(
                        eq(oldestSession.getFamilyId()),
                        any(Instant.class),
                        eq("new-session-created"));

        verify(dao)
                .revokeSessionByFamilyId(
                        eq(oldestSession.getFamilyId()),
                        any(Instant.class),
                        eq("new-session-created"));

        verify(dao).save(any(Session.class));
    }

    @Test
    void createSessionShouldCreateNewSessionSuccessfullyWhenLimitIsNotReached() {
        UUID userId = user.getId();
        UUID newFamilyId = UUID.randomUUID();

        String userAgent = "Chrome";
        String clientId = "mobile";
        String ipAddress = "192.168.1.1";

        when(userDAO.lockFindUserById(userId)).thenReturn(Optional.of(user));
        when(dao.findByUniqueProperties(userId, userAgent, clientId, ipAddress))
                .thenReturn(Optional.empty());
        when(dao.countByUserIdAndRevokedFalse(userId)).thenReturn(1L);
        when(dao.findFirstActiveSessionForUpdate(userId)).thenReturn(Optional.empty());
        when(uuidv7Generator.generate()).thenReturn(newFamilyId);

        when(dao.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Session result = service.createSession(userId, userAgent, clientId, ipAddress);

        assertAll(
                () -> assertEquals(user, result.getUser()),
                () -> assertEquals(userAgent, result.getUserAgent()),
                () -> assertEquals(clientId, result.getClientId()),
                () -> assertEquals(ipAddress, result.getIpAddress()),
                () -> assertEquals(newFamilyId, result.getFamilyId()),
                () -> assertFalse(result.isRevoked()),
                () -> assertNotNull(result.getLastSeenAt()));

        verify(dao).save(any(Session.class));
    }

    @Test
    void revokeSessionByUserIdAndIdShouldThrowForbiddenExceptionWhenSessionDoesNotExist() {
        UUID userId = user.getId();
        UUID sessionId = session.getId();

        when(dao.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.empty());

        assertThrows(
                ForbiddenException.class,
                () -> service.revokeSessionByUserIdAndId(userId, sessionId));

        verify(dao).findByIdAndUserId(sessionId, userId);
    }

    @Test
    void revokeSessionByUserIdAndIdShouldThrowForbiddenExceptionWhenUserDoesNotOwnSession() {
        UUID userId = user.getId();
        UUID sessionId = session.getId();

        User anotherUser = UserProvider.alternativeEntity();
        session.setUser(anotherUser);

        when(dao.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));

        assertThrows(
                ForbiddenException.class,
                () -> service.revokeSessionByUserIdAndId(userId, sessionId));

        verify(dao).findByIdAndUserId(sessionId, userId);
    }

    @Test
    void revokeSessionByUserIdAndIdShouldRevokeSessionSuccessfully() {
        UUID userId = user.getId();
        UUID sessionId = session.getId();

        when(dao.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));

        service.revokeSessionByUserIdAndId(userId, sessionId);

        verify(dao).findByIdAndUserId(sessionId, userId);

        verify(dao)
                .revokeSessionByFamilyId(
                        eq(session.getFamilyId()), any(Instant.class), eq("user-self-revocation"));
    }

    @Test
    void revokeAllSessionsByUserIdShouldRevokeSessionsAndRefreshTokens() {
        UUID userId = user.getId();

        service.revokeAllSessionsByUserId(userId);

        verify(dao)
                .revokeSessionsByUserId(eq(userId), any(Instant.class), eq("user-self-revocation"));

        verify(refreshTokenDAO)
                .revokeTokensByUserId(eq(userId), any(Instant.class), eq("user-self-revocation"));
    }

    @Test
    void existsByUniquePropertiesShouldDelegateToDao() {
        when(dao.existsByUniqueProperties(session)).thenReturn(true);

        boolean result = service.existsByUniqueProperties(session);

        assertTrue(result);

        verify(dao).existsByUniqueProperties(session);
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

        assertThrows(NotFoundException.class, () -> service.updateById(session, sessionId));

        verify(dao).findById(sessionId);
        verify(dao, never()).save(any(Session.class));
    }

    @Test
    void updateByIdShouldUpdateUserWhenIncomingUserIsDifferent() {
        Session existingSession = SessionProvider.singleEntity();
        Session incomingSession = SessionProvider.alternativeEntity();

        UUID sessionId = existingSession.getId();

        when(dao.findById(sessionId)).thenReturn(Optional.of(existingSession));
        when(dao.save(existingSession)).thenReturn(existingSession);

        Session result = service.updateById(incomingSession, sessionId);

        assertEquals(incomingSession.getUser(), result.getUser());

        verify(dao).save(existingSession);
    }

    @Test
    void updateByIdShouldNotUpdateUserWhenIncomingUserIsNull() {
        Session existingSession = SessionProvider.singleEntity();
        Session incomingSession = SessionProvider.alternativeEntity();

        incomingSession.setUser(null);

        UUID sessionId = existingSession.getId();

        when(dao.findById(sessionId)).thenReturn(Optional.of(existingSession));
        when(dao.save(existingSession)).thenReturn(existingSession);

        Session result = service.updateById(incomingSession, sessionId);

        assertEquals(existingSession.getUser(), result.getUser());

        verify(dao).save(existingSession);
    }

    @Test
    void updateByIdShouldNotUpdateUserWhenIncomingUserIdIsNull() {
        Session existingSession = SessionProvider.singleEntity();
        Session incomingSession = SessionProvider.alternativeEntity();

        incomingSession.setUser(new User());

        UUID sessionId = existingSession.getId();

        when(dao.findById(sessionId)).thenReturn(Optional.of(existingSession));
        when(dao.save(existingSession)).thenReturn(existingSession);

        Session result = service.updateById(incomingSession, sessionId);

        assertEquals(existingSession.getUser(), result.getUser());

        verify(dao).save(existingSession);
    }

    @Test
    void updateByIdShouldNotUpdateUserWhenIncomingUserMatchesExistingUser() {
        Session existingSession = SessionProvider.singleEntity();
        Session incomingSession = SessionProvider.alternativeEntity();

        incomingSession.setUser(existingSession.getUser());

        UUID sessionId = existingSession.getId();

        when(dao.findById(sessionId)).thenReturn(Optional.of(existingSession));
        when(dao.save(existingSession)).thenReturn(existingSession);

        Session result = service.updateById(incomingSession, sessionId);

        assertEquals(existingSession.getUser(), result.getUser());

        verify(dao).save(existingSession);
    }

    @Test
    void updateByIdShouldUpdateAllMutableFieldsSuccessfully() {
        Session existingSession = SessionProvider.singleEntity();
        Session incomingSession = SessionProvider.alternativeEntity();

        UUID sessionId = existingSession.getId();

        when(dao.findById(sessionId)).thenReturn(Optional.of(existingSession));
        when(dao.save(existingSession)).thenReturn(existingSession);

        Session result = service.updateById(incomingSession, sessionId);

        assertAll(
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
    void updateByIdShouldKeepCurrentValuesWhenIncomingValuesAreEqual() {
        Session existingSession = SessionProvider.singleEntity();
        Session incomingSession = SessionProvider.singleEntity();

        UUID sessionId = existingSession.getId();

        when(dao.findById(sessionId)).thenReturn(Optional.of(existingSession));
        when(dao.save(existingSession)).thenReturn(existingSession);

        Session result = service.updateById(incomingSession, sessionId);

        assertAll(
                () -> assertEquals(existingSession.getFamilyId(), result.getFamilyId()),
                () -> assertEquals(existingSession.getIpAddress(), result.getIpAddress()),
                () -> assertEquals(existingSession.getUserAgent(), result.getUserAgent()),
                () -> assertEquals(existingSession.getClientId(), result.getClientId()),
                () -> assertEquals(existingSession.isRevoked(), result.isRevoked()),
                () -> assertEquals(existingSession.getRevokedAt(), result.getRevokedAt()),
                () -> assertEquals(existingSession.getRevokeReason(), result.getRevokeReason()));

        verify(dao).save(existingSession);
    }

    @Test
    void updateByIdShouldIgnoreNullOptionalFields() {
        Session existingSession = SessionProvider.singleEntity();

        Session incomingSession = new Session();
        incomingSession.setRevoked(existingSession.isRevoked());

        UUID sessionId = existingSession.getId();

        when(dao.findById(sessionId)).thenReturn(Optional.of(existingSession));
        when(dao.save(existingSession)).thenReturn(existingSession);

        Session result = service.updateById(incomingSession, sessionId);

        assertAll(
                () -> assertEquals(existingSession.getUser(), result.getUser()),
                () -> assertEquals(existingSession.getFamilyId(), result.getFamilyId()),
                () -> assertEquals(existingSession.getIpAddress(), result.getIpAddress()),
                () -> assertEquals(existingSession.getUserAgent(), result.getUserAgent()),
                () -> assertEquals(existingSession.getClientId(), result.getClientId()),
                () -> assertEquals(existingSession.getRevokedAt(), result.getRevokedAt()),
                () -> assertEquals(existingSession.getRevokeReason(), result.getRevokeReason()));

        verify(dao).save(existingSession);
    }

    @Test
    void findAllByUserIdShouldDelegateToDao() {
        UUID userId = user.getId();

        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(0, 5);

        org.springframework.data.domain.Page<Session> expectedPage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.List.of(session), pageable, 1);

        when(dao.findAllByUserId(userId, pageable)).thenReturn(expectedPage);

        org.springframework.data.domain.Page<Session> result =
                service.findAllByUserId(userId, pageable);

        assertNotNull(result);
        assertEquals(expectedPage, result);
        assertEquals(1, result.getContent().size());
        assertEquals(session, result.getContent().getFirst());

        verify(dao).findAllByUserId(userId, pageable);
    }

    @Test
    void
            createSessionShouldCreateSessionWithoutRevokingWhenInfinityLoginEnabledAndNoOldestSession() {
        SessionServiceImpl infinityLoginService =
                new SessionServiceImpl(dao, userDAO, refreshTokenDAO, uuidv7Generator, 2, true);

        UUID userId = user.getId();
        UUID newFamilyId = UUID.randomUUID();

        String userAgent = "Safari";
        String clientId = "desktop";
        String ipAddress = "10.0.0.1";

        when(userDAO.lockFindUserById(userId)).thenReturn(Optional.of(user));
        when(dao.findByUniqueProperties(userId, userAgent, clientId, ipAddress))
                .thenReturn(Optional.empty());
        when(dao.countByUserIdAndRevokedFalse(userId)).thenReturn(2L);
        when(dao.findFirstActiveSessionForUpdate(userId)).thenReturn(Optional.empty());
        when(uuidv7Generator.generate()).thenReturn(newFamilyId);

        when(dao.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Session result = infinityLoginService.createSession(userId, userAgent, clientId, ipAddress);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(user, result.getUser()),
                () -> assertEquals(userAgent, result.getUserAgent()),
                () -> assertEquals(clientId, result.getClientId()),
                () -> assertEquals(ipAddress, result.getIpAddress()),
                () -> assertEquals(newFamilyId, result.getFamilyId()),
                () -> assertFalse(result.isRevoked()));

        verify(dao).countByUserIdAndRevokedFalse(userId);
        verify(dao).findFirstActiveSessionForUpdate(userId);
        verify(dao).save(any(Session.class));

        verify(refreshTokenDAO, never())
                .revokeFamilyWithReason(any(UUID.class), any(Instant.class), anyString());

        verify(dao, never())
                .revokeSessionByFamilyId(any(UUID.class), any(Instant.class), anyString());
    }
}
