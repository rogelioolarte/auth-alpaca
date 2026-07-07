package com.alpaca.unit.service;

import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.entity.RefreshToken;
import com.alpaca.entity.Session;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.AuthCode;
import com.alpaca.model.UserPrincipal;
import com.alpaca.persistence.IRefreshTokenDAO;
import com.alpaca.resources.provider.RefreshTokenProvider;
import com.alpaca.resources.provider.SessionProvider;
import com.alpaca.resources.provider.UserProvider;
import com.alpaca.security.manager.JJwtManager;
import com.alpaca.service.ISessionService;
import com.alpaca.service.IUserService;
import com.alpaca.service.impl.RefreshTokenServiceImpl;
import com.alpaca.utils.UUIDv7Generator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Unit tests for {@link RefreshTokenServiceImpl}. */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock private IRefreshTokenDAO dao;
    @Mock private ISessionService sessionService;
    @Mock private IUserService userService;
    @Mock private JJwtManager manager;
    @Mock private UUIDv7Generator uuidv7Generator;

    @InjectMocks private RefreshTokenServiceImpl service;

    private RefreshToken refreshToken;
    private Session session;
    private User user;
    private UserPrincipal userPrincipal;
    private String clientId;
    private String userAgent;
    private String ipAddress;

    @BeforeEach
    void setUp() {
        user = UserProvider.singleEntity();
        session = SessionProvider.singleEntity();
        refreshToken = RefreshTokenProvider.singleEntity();

        refreshToken.setUser(user);
        refreshToken.setFamilyId(session.getFamilyId());
        refreshToken.setClientId(session.getClientId());
        refreshToken.setUserAgent(session.getUserAgent());
        refreshToken.setCreatedAt(Instant.now());
        refreshToken.setExpiresAt(Instant.now().plusSeconds(300));

        clientId = refreshToken.getClientId();
        userAgent = refreshToken.getUserAgent();
        ipAddress = refreshToken.getIpAddress();

        userPrincipal = new UserPrincipal(user);
    }

    @Test
    void rotateRefreshToken_WhenValidRequest_ThenRotateSuccessfully() {
        String oldRefreshToken = "old-refresh-token";
        String oldRefreshTokenHash = "old-refresh-token-hash";
        String newRefreshTokenJwt = "new-refresh-token";
        String newRefreshTokenHash = "new-refresh-token-hash";
        String accessToken = "access-token";
        UUID newJti = UUID.randomUUID();

        when(manager.createTokenHash(oldRefreshToken)).thenReturn(oldRefreshTokenHash);
        when(dao.findByTokenHashSecure(oldRefreshTokenHash)).thenReturn(Optional.of(refreshToken));
        when(sessionService.findSessionByFamilyId(refreshToken.getFamilyId()))
                .thenReturn(Optional.of(session));
        when(uuidv7Generator.generate()).thenReturn(newJti);
        when(manager.getJwtTimeExpRefresh()).thenReturn(300_000L);
        when(manager.createRefreshToken(any(RefreshToken.class))).thenReturn(newRefreshTokenJwt);
        when(manager.createTokenHash(newRefreshTokenJwt)).thenReturn(newRefreshTokenHash);
        when(dao.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(manager.createAccessToken(any(UserPrincipal.class), any(Instant.class)))
                .thenReturn(accessToken);

        AuthResponseDTO response =
                service.rotateRefreshToken(
                        oldRefreshToken,
                        refreshToken.getClientId(),
                        refreshToken.getUserAgent(),
                        refreshToken.getIpAddress());

        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(accessToken, response.accessToken()),
                () -> assertEquals(newRefreshTokenJwt, response.refreshToken()),
                () -> assertTrue(refreshToken.isRevoked()),
                () -> assertEquals("rotation", refreshToken.getRevokeReason()),
                () -> assertNotNull(refreshToken.getRevokedAt()),
                () -> assertNotNull(refreshToken.getReplacedBy()));

        verify(dao, times(2)).save(any(RefreshToken.class));
        verify(manager).createAccessToken(any(UserPrincipal.class), any(Instant.class));
    }

    @Test
    void rotateRefreshToken_WhenSessionRevoked_ThenThrowUnauthorizedException() {
        String oldRefreshToken = "old-refresh-token";
        String oldRefreshTokenHash = "old-refresh-token-hash";

        session.setRevoked(true);

        when(manager.createTokenHash(oldRefreshToken)).thenReturn(oldRefreshTokenHash);
        when(dao.findByTokenHashSecure(oldRefreshTokenHash)).thenReturn(Optional.of(refreshToken));
        when(sessionService.findSessionByFamilyId(refreshToken.getFamilyId()))
                .thenReturn(Optional.of(session));

        UnauthorizedException exception =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        oldRefreshToken, clientId, userAgent, ipAddress));

        assertEquals("Revoked Session", exception.getReason());

        verify(dao, never()).save(any(RefreshToken.class));
    }

    @Test
    void rotateRefreshToken_WhenSessionRevokedAtBeforeNow_ThenThrowUnauthorizedException() {
        String oldRefreshToken = "old-refresh-token";
        String oldRefreshTokenHash = "old-refresh-token-hash";

        session.setRevoked(false);
        session.setRevokedAt(Instant.now().minusSeconds(60));

        when(manager.createTokenHash(oldRefreshToken)).thenReturn(oldRefreshTokenHash);
        when(dao.findByTokenHashSecure(oldRefreshTokenHash)).thenReturn(Optional.of(refreshToken));
        when(sessionService.findSessionByFamilyId(refreshToken.getFamilyId()))
                .thenReturn(Optional.of(session));

        UnauthorizedException exception =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        oldRefreshToken, clientId, userAgent, ipAddress));

        assertEquals("Revoked Session", exception.getReason());

        verify(dao, never()).save(any(RefreshToken.class));
    }

    @Test
    void rotateRefreshToken_WhenRefreshTokenIsBlank_ThenThrowBadRequestException() {
        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> service.rotateRefreshToken(" ", clientId, userAgent, ipAddress));

        assertEquals("Invalid Refresh Token", exception.getReason());
    }

    @Test
    void rotateRefreshToken_WhenClientIdIsBlank_ThenThrowBadRequestException() {
        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> service.rotateRefreshToken("token", " ", userAgent, ipAddress));

        assertEquals("Invalid Client ID", exception.getReason());
    }

    @Test
    void rotateRefreshToken_WhenUserAgentIsBlank_ThenThrowBadRequestException() {
        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> service.rotateRefreshToken("token", clientId, " ", ipAddress));

        assertEquals("Invalid User Agent", exception.getReason());
    }

    @Test
    void rotateRefreshToken_WhenClientIpIsBlank_ThenThrowBadRequestException() {
        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> service.rotateRefreshToken("token", clientId, userAgent, " "));

        assertEquals("Invalid Client IP", exception.getReason());
    }

    @Test
    void rotateRefreshToken_WhenTokenDoesNotExist_ThenThrowUnauthorizedException() {
        String oldRefreshToken = "old-refresh-token";
        String oldRefreshTokenHash = "old-refresh-token-hash";

        when(manager.createTokenHash(oldRefreshToken)).thenReturn(oldRefreshTokenHash);
        when(dao.findByTokenHashSecure(oldRefreshTokenHash)).thenReturn(Optional.empty());

        UnauthorizedException exception =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        oldRefreshToken, clientId, userAgent, ipAddress));

        assertEquals("Invalid Refresh Token", exception.getReason());

        verify(dao, never()).save(any(RefreshToken.class));
    }

    @Test
    void
            rotateRefreshToken_WhenRefreshTokenAlreadyRevoked_ThenRevokeFamilyAndThrowUnauthorizedException() {
        String oldRefreshToken = "old-refresh-token";
        String oldRefreshTokenHash = "old-refresh-token-hash";

        refreshToken.setRevoked(true);

        when(manager.createTokenHash(oldRefreshToken)).thenReturn(oldRefreshTokenHash);
        when(dao.findByTokenHashSecure(oldRefreshTokenHash)).thenReturn(Optional.of(refreshToken));

        UnauthorizedException exception =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        oldRefreshToken, clientId, userAgent, ipAddress));

        assertEquals("Refresh Token already revoked", exception.getReason());

        verify(dao)
                .revokeFamilyWithReason(
                        eq(refreshToken.getFamilyId()), any(Instant.class), eq("reuse-detected"));
        verify(sessionService)
                .revokeSessionByFamilyId(
                        eq(refreshToken.getFamilyId()), any(Instant.class), eq("reuse-detected"));
    }

    @Test
    void
            rotateRefreshToken_WhenRefreshTokenAlreadyReplaced_ThenRevokeFamilyAndThrowUnauthorizedException() {
        String oldRefreshToken = "old-refresh-token";
        String oldRefreshTokenHash = "old-refresh-token-hash";

        refreshToken.setReplacedBy(new RefreshToken());

        when(manager.createTokenHash(oldRefreshToken)).thenReturn(oldRefreshTokenHash);
        when(dao.findByTokenHashSecure(oldRefreshTokenHash)).thenReturn(Optional.of(refreshToken));

        UnauthorizedException exception =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        oldRefreshToken, clientId, userAgent, ipAddress));

        assertEquals("Reuse Detected Refresh Token", exception.getReason());

        verify(dao)
                .revokeFamilyWithReason(
                        eq(refreshToken.getFamilyId()), any(Instant.class), eq("reuse-detected"));
    }

    @Test
    void
            rotateRefreshToken_WhenRefreshTokenExpired_ThenRevokeFamilyAndThrowUnauthorizedException() {
        String oldRefreshToken = "old-refresh-token";
        String oldRefreshTokenHash = "old-refresh-token-hash";

        refreshToken.setExpiresAt(Instant.now().minusSeconds(60));

        when(manager.createTokenHash(oldRefreshToken)).thenReturn(oldRefreshTokenHash);
        when(dao.findByTokenHashSecure(oldRefreshTokenHash)).thenReturn(Optional.of(refreshToken));

        UnauthorizedException exception =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        oldRefreshToken, clientId, userAgent, ipAddress));

        assertEquals("Reuse Detected Refresh Token", exception.getReason());

        verify(dao)
                .revokeFamilyWithReason(
                        eq(refreshToken.getFamilyId()), any(Instant.class), eq("reuse-detected"));
    }

    @Test
    void
            rotateRefreshToken_WhenTokenIssuedBeforeTokensInvalidBefore_ThenThrowUnauthorizedException() {
        String oldRefreshToken = "old-refresh-token";
        String oldRefreshTokenHash = "old-refresh-token-hash";

        user.setTokensInvalidBefore(Instant.now().plusSeconds(60));

        when(manager.createTokenHash(oldRefreshToken)).thenReturn(oldRefreshTokenHash);
        when(dao.findByTokenHashSecure(oldRefreshTokenHash)).thenReturn(Optional.of(refreshToken));

        UnauthorizedException exception =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        oldRefreshToken, clientId, userAgent, ipAddress));

        assertEquals("Refresh Token already revoked", exception.getReason());
    }

    @Test
    void rotateRefreshToken_WhenClientIdMismatch_ThenRevokeFamilyAndThrowUnauthorizedException() {
        String oldRefreshToken = "old-refresh-token";
        String oldRefreshTokenHash = "old-refresh-token-hash";
        String invalidClientId = "invalid-client-id";

        when(manager.createTokenHash(oldRefreshToken)).thenReturn(oldRefreshTokenHash);
        when(dao.findByTokenHashSecure(oldRefreshTokenHash)).thenReturn(Optional.of(refreshToken));

        UnauthorizedException exception =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        oldRefreshToken, invalidClientId, userAgent, ipAddress));

        assertEquals("Client mismatch", exception.getReason());

        verify(dao)
                .revokeFamilyWithReason(
                        eq(refreshToken.getFamilyId()), any(Instant.class), eq("client-mismatch"));
    }

    @Test
    void rotateRefreshToken_WhenUserAgentMismatch_ThenRevokeFamilyAndThrowUnauthorizedException() {
        String oldRefreshToken = "old-refresh-token";
        String oldRefreshTokenHash = "old-refresh-token-hash";
        String invalidUserAgent = "invalid-user-agent";

        when(manager.createTokenHash(oldRefreshToken)).thenReturn(oldRefreshTokenHash);
        when(dao.findByTokenHashSecure(oldRefreshTokenHash)).thenReturn(Optional.of(refreshToken));

        UnauthorizedException exception =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        oldRefreshToken, clientId, invalidUserAgent, ipAddress));

        assertEquals("User-Agent mismatch", exception.getReason());

        verify(dao)
                .revokeFamilyWithReason(
                        eq(refreshToken.getFamilyId()), any(Instant.class), eq("ua-mismatch"));
    }

    @Test
    void generateJWTTokens_WhenUsingUserPrincipal_ThenGenerateTokensSuccessfully() {
        UUID refreshTokenId = UUID.randomUUID();
        String refreshTokenJwt = "refresh-token";
        String refreshTokenHash = "refresh-token-hash";
        String accessToken = "access-token";

        when(uuidv7Generator.generate()).thenReturn(refreshTokenId);
        when(manager.getJwtTimeExpRefresh()).thenReturn(300_000L);
        when(manager.createRefreshToken(any(RefreshToken.class))).thenReturn(refreshTokenJwt);
        when(manager.createTokenHash(refreshTokenJwt)).thenReturn(refreshTokenHash);
        when(manager.createAccessToken(userPrincipal, session.getLastSeenAt()))
                .thenReturn(accessToken);

        AuthResponseDTO response = service.generateJWTTokens(userPrincipal, session);

        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(accessToken, response.accessToken()),
                () -> assertEquals(refreshTokenJwt, response.refreshToken()));

        verify(dao).save(any(RefreshToken.class));
    }

    @Test
    void generateJWTTokens_WhenUsingAuthCode_ThenGenerateTokensSuccessfully() {
        UUID refreshTokenId = UUID.randomUUID();
        String refreshTokenJwt = "refresh-token";
        String refreshTokenHash = "refresh-token-hash";
        String accessToken = "access-token";

        AuthCode authCode =
                new AuthCode(
                        "authorization-code",
                        "challenge",
                        session.getClientId(),
                        session.getUserAgent(),
                        session.getIpAddress(),
                        user.getId(),
                        "redirect-uri");

        when(userService.findById(authCode.getUserId())).thenReturn(user);
        when(sessionService.createSession(
                        authCode.getUserId(),
                        authCode.getUserAgent(),
                        authCode.getClientId(),
                        authCode.getClientIp()))
                .thenReturn(session);
        when(uuidv7Generator.generate()).thenReturn(refreshTokenId);
        when(manager.getJwtTimeExpRefresh()).thenReturn(300_000L);
        when(manager.createRefreshToken(any(RefreshToken.class))).thenReturn(refreshTokenJwt);
        when(manager.createTokenHash(refreshTokenJwt)).thenReturn(refreshTokenHash);
        when(manager.createAccessToken(any(UserPrincipal.class), any(Instant.class)))
                .thenReturn(accessToken);

        AuthResponseDTO response = service.generateJWTTokens(authCode);

        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(accessToken, response.accessToken()),
                () -> assertEquals(refreshTokenJwt, response.refreshToken()));

        verify(userService).findById(authCode.getUserId());
        verify(sessionService)
                .createSession(
                        authCode.getUserId(),
                        authCode.getUserAgent(),
                        authCode.getClientId(),
                        authCode.getClientIp());
        verify(dao).save(any(RefreshToken.class));
    }

    @Test
    void revokeRefreshTokensAndSessionByFamilyId_WhenInvoked_ThenDelegateCorrectly() {
        UUID familyId = UUID.randomUUID();
        Instant revokedAt = Instant.now();
        String reason = "reuse-detected";

        service.revokeRefreshTokensAndSessionByFamilyId(familyId, revokedAt, reason);

        verify(dao).revokeFamilyWithReason(familyId, revokedAt, reason);
        verify(sessionService).revokeSessionByFamilyId(familyId, revokedAt, reason);
    }

    @Test
    void revokeFamilyWithReason_WhenInvoked_ThenDelegateCorrectly() {
        UUID familyId = UUID.randomUUID();
        Instant revokedAt = Instant.now();
        String reason = "manual-revocation";

        service.revokeFamilyWithReason(familyId, revokedAt, reason);

        verify(dao).revokeFamilyWithReason(familyId, revokedAt, reason);
    }

    @Test
    void findFamilyIdByTokenHash_WhenInvoked_ThenReturnExpectedResult() {
        String hash = "token-hash";
        UUID familyId = UUID.randomUUID();

        when(dao.findFamilyIdByTokenHash(hash)).thenReturn(Optional.of(familyId));

        Optional<UUID> result = service.findFamilyIdByTokenHash(hash);

        assertTrue(result.isPresent());
        assertEquals(familyId, result.get());

        verify(dao).findFamilyIdByTokenHash(hash);
    }

    @Test
    void findByTokenHashSecure_WhenInvoked_ThenReturnExpectedResult() {
        String hash = "token-hash";

        when(dao.findByTokenHashSecure(hash)).thenReturn(Optional.of(refreshToken));

        Optional<RefreshToken> result = service.findByTokenHashSecure(hash);

        assertTrue(result.isPresent());
        assertEquals(refreshToken, result.get());

        verify(dao).findByTokenHashSecure(hash);
    }

    @Test
    void validateRefreshToken_WhenTokenRevoked_ThenRevokeFamilyAndThrowUnauthorizedException() {
        Instant now = Instant.now();

        refreshToken.setRevoked(true);

        UnauthorizedException exception =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.validateRefreshToken(
                                        refreshToken, clientId, now, ipAddress, userAgent));

        assertEquals("Refresh Token already revoked", exception.getReason());

        verify(dao).revokeFamilyWithReason(refreshToken.getFamilyId(), now, "reuse-detected");

        verify(sessionService)
                .revokeSessionByFamilyId(refreshToken.getFamilyId(), now, "reuse-detected");
    }

    @Test
    void
            validateRefreshToken_WhenTokenReplacedByExists_ThenRevokeFamilyAndThrowUnauthorizedException() {
        Instant now = Instant.now();

        RefreshToken replacedBy = new RefreshToken();
        replacedBy.setId(UUID.randomUUID());

        refreshToken.setReplacedBy(replacedBy);

        UnauthorizedException exception =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.validateRefreshToken(
                                        refreshToken, clientId, now, ipAddress, userAgent));

        assertEquals("Reuse Detected Refresh Token", exception.getReason());

        verify(dao).revokeFamilyWithReason(refreshToken.getFamilyId(), now, "reuse-detected");

        verify(sessionService)
                .revokeSessionByFamilyId(refreshToken.getFamilyId(), now, "reuse-detected");
    }

    @Test
    void validateRefreshToken_WhenTokenExpired_ThenRevokeFamilyAndThrowUnauthorizedException() {
        Instant now = Instant.now();

        refreshToken.setExpiresAt(now.minusSeconds(1));

        UnauthorizedException exception =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.validateRefreshToken(
                                        refreshToken, clientId, now, ipAddress, userAgent));

        assertEquals("Reuse Detected Refresh Token", exception.getReason());

        verify(dao).revokeFamilyWithReason(refreshToken.getFamilyId(), now, "reuse-detected");

        verify(sessionService)
                .revokeSessionByFamilyId(refreshToken.getFamilyId(), now, "reuse-detected");
    }

    @Test
    void
            validateRefreshToken_WhenTokenIssuedBeforeTokensInvalidBefore_ThenThrowUnauthorizedException() {
        Instant now = Instant.now();

        user.setTokensInvalidBefore(now.plusSeconds(60));
        refreshToken.setCreatedAt(now.minusSeconds(60));

        UnauthorizedException exception =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.validateRefreshToken(
                                        refreshToken, clientId, now, ipAddress, userAgent));

        assertEquals("Refresh Token already revoked", exception.getReason());

        verify(dao).revokeFamilyWithReason(refreshToken.getFamilyId(), now, "reuse-detected");
        verify(sessionService)
                .revokeSessionByFamilyId(refreshToken.getFamilyId(), now, "reuse-detected");
    }

    @Test
    void validateRefreshToken_WhenClientIdMismatch_ThenRevokeFamilyAndThrowUnauthorizedException() {
        Instant now = Instant.now();

        String invalidClientId = "invalid-client-id";

        UnauthorizedException exception =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.validateRefreshToken(
                                        refreshToken, invalidClientId, now, ipAddress, userAgent));

        assertEquals("Client mismatch", exception.getReason());

        verify(dao).revokeFamilyWithReason(refreshToken.getFamilyId(), now, "client-mismatch");

        verify(sessionService)
                .revokeSessionByFamilyId(refreshToken.getFamilyId(), now, "client-mismatch");
    }

    @Test
    void
            validateRefreshToken_WhenUserAgentMismatch_ThenRevokeFamilyAndThrowUnauthorizedException() {
        Instant now = Instant.now();

        String invalidUserAgent = "invalid-user-agent";

        UnauthorizedException exception =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.validateRefreshToken(
                                        refreshToken, clientId, now, ipAddress, invalidUserAgent));

        assertEquals("User-Agent mismatch", exception.getReason());

        verify(dao).revokeFamilyWithReason(refreshToken.getFamilyId(), now, "ua-mismatch");

        verify(sessionService)
                .revokeSessionByFamilyId(refreshToken.getFamilyId(), now, "ua-mismatch");
    }

    @Test
    void validateRefreshToken_WhenTokenIsValid_ThenDoNotThrowException() {
        Instant now = Instant.now();

        assertDoesNotThrow(
                () ->
                        service.validateRefreshToken(
                                refreshToken, clientId, now, ipAddress, userAgent));

        verify(dao, never()).revokeFamilyWithReason(any(), any(), any());
        verify(sessionService, never()).revokeSessionByFamilyId(any(), any(), any());
    }

    @Test
    void updateById_WhenRefreshTokenIsNull_ThenThrowBadRequestException() {
        UUID refreshTokenId = UUID.randomUUID();

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class, () -> service.updateById(null, refreshTokenId));

        assertEquals(
                String.format("RefreshToken with ID %s cannot be updated", refreshTokenId),
                exception.getReason());

        verify(dao, never()).findById(any(UUID.class));
    }

    @Test
    void updateById_WhenIdIsNull_ThenThrowBadRequestException() {
        RefreshToken updatedRefreshToken = new RefreshToken();

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> service.updateById(updatedRefreshToken, null));

        assertEquals("RefreshToken with ID null cannot be updated", exception.getReason());

        verify(dao, never()).findById(any(UUID.class));
    }

    @Test
    void updateById_WhenRefreshTokenDoesNotExist_ThenThrowNotFoundException() {
        UUID refreshTokenId = UUID.randomUUID();

        RefreshToken updatedRefreshToken = new RefreshToken();

        when(dao.findById(refreshTokenId)).thenReturn(Optional.empty());

        assertThrows(
                com.alpaca.exception.NotFoundException.class,
                () -> service.updateById(updatedRefreshToken, refreshTokenId));

        verify(dao).findById(refreshTokenId);
        verify(dao, never()).save(any(RefreshToken.class));
    }

    @Test
    void updateById_WhenRefreshTokenExists_ThenUpdateSuccessfully() {
        UUID refreshTokenId = UUID.randomUUID();

        RefreshToken existingRefreshToken = new RefreshToken();
        existingRefreshToken.setId(refreshTokenId);
        existingRefreshToken.setUser(user);
        existingRefreshToken.setTokenJti(UUID.randomUUID());
        existingRefreshToken.setFamilyId(UUID.randomUUID());
        existingRefreshToken.setRevoked(false);
        existingRefreshToken.setTokenHash("old-token-hash");
        existingRefreshToken.setClientId("old-client-id");
        existingRefreshToken.setIpAddress("old-ip");
        existingRefreshToken.setUserAgent("old-user-agent");
        existingRefreshToken.setRevokeReason("old-reason");

        RefreshToken replacedBy = new RefreshToken();
        replacedBy.setId(UUID.randomUUID());

        RefreshToken updatedRefreshToken = new RefreshToken();

        User updatedUser = new User();
        updatedUser.setId(UUID.randomUUID());

        UUID updatedTokenJti = UUID.randomUUID();
        UUID updatedFamilyId = UUID.randomUUID();
        Instant revokedAt = Instant.now();
        Instant expiresAt = Instant.now().plusSeconds(300);
        Instant lastUsedAt = Instant.now().plusSeconds(60);

        updatedRefreshToken.setUser(updatedUser);
        updatedRefreshToken.setReplacedBy(replacedBy);
        updatedRefreshToken.setTokenJti(updatedTokenJti);
        updatedRefreshToken.setFamilyId(updatedFamilyId);
        updatedRefreshToken.setRevoked(true);
        updatedRefreshToken.setRevokedAt(revokedAt);
        updatedRefreshToken.setExpiresAt(expiresAt);
        updatedRefreshToken.setLastUsedAt(lastUsedAt);
        updatedRefreshToken.setTokenHash("new-token-hash");
        updatedRefreshToken.setClientId("new-client-id");
        updatedRefreshToken.setIpAddress("new-ip");
        updatedRefreshToken.setUserAgent("new-user-agent");
        updatedRefreshToken.setRevokeReason("new-reason");

        when(dao.findById(refreshTokenId)).thenReturn(Optional.of(existingRefreshToken));

        when(dao.save(existingRefreshToken)).thenReturn(existingRefreshToken);

        RefreshToken result = service.updateById(updatedRefreshToken, refreshTokenId);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(updatedUser, result.getUser()),
                () -> assertEquals(replacedBy, result.getReplacedBy()),
                () -> assertEquals(updatedTokenJti, result.getTokenJti()),
                () -> assertEquals(updatedFamilyId, result.getFamilyId()),
                () -> assertTrue(result.isRevoked()),
                () -> assertEquals(revokedAt, result.getRevokedAt()),
                () -> assertEquals(expiresAt, result.getExpiresAt()),
                () -> assertEquals(lastUsedAt, result.getLastUsedAt()),
                () -> assertEquals("new-token-hash", result.getTokenHash()),
                () -> assertEquals("new-client-id", result.getClientId()),
                () -> assertEquals("new-ip", result.getIpAddress()),
                () -> assertEquals("new-user-agent", result.getUserAgent()),
                () -> assertEquals("new-reason", result.getRevokeReason()));

        verify(dao).findById(refreshTokenId);
        verify(dao).save(existingRefreshToken);
    }
}
