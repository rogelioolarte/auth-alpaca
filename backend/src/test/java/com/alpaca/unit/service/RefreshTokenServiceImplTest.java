package com.alpaca.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.entity.RefreshToken;
import com.alpaca.entity.Session;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.persistence.IRefreshTokenDAO;
import com.alpaca.resources.RefreshTokenProvider;
import com.alpaca.resources.SessionProvider;
import com.alpaca.resources.UserProvider;
import com.alpaca.security.manager.JJwtManager;
import com.alpaca.service.ISessionService;
import com.alpaca.service.impl.RefreshTokenServiceImpl;
import com.alpaca.utils.UUIDv7Generator;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link RefreshTokenServiceImpl} */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock private IRefreshTokenDAO dao;
    @Mock private ISessionService sessionService;
    @Mock private JJwtManager manager;
    @Mock private UUIDv7Generator uuidv7Generator;

    @InjectMocks private RefreshTokenServiceImpl service;

    private Session testSession;
    private RefreshToken testRefreshToken;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        User testUser = UserProvider.singleEntity();
        testSession = SessionProvider.singleEntity();
        testRefreshToken = RefreshTokenProvider.singleEntity();
        userPrincipal = new UserPrincipal(testUser, null);
    }

    // --------------------------
    // rotateRefreshToken - happy path
    // --------------------------
    @Test
    void rotateRefreshToken_validToken_returnsNewTokens() {
        String oldRefreshToken = "valid.jwt.token";
        String clientId = "web-client";
        String userAgent = testRefreshToken.getUserAgent();
        String clientIp = "127.0.0.1";
        String tokenHash = "hashed_token";
        String newJwtToken = "new.jwt.token";
        String newTokenHash = "new_hashed_token";
        UUID newTokenJti = UUID.randomUUID();

        // arrange
        when(manager.createRefreshTokenHash(oldRefreshToken)).thenReturn(tokenHash);
        when(dao.findByTokenHashSecure(tokenHash)).thenReturn(Optional.of(testRefreshToken));
        when(sessionService.findSessionByFamilyId(testRefreshToken.getFamilyId()))
                .thenReturn(Optional.of(testSession));
        when(uuidv7Generator.generate()).thenReturn(newTokenJti);
        when(manager.getJwtTimeExpRefresh()).thenReturn(86400000L);
        when(manager.createRefreshToken(any(RefreshToken.class))).thenReturn(newJwtToken);
        when(manager.createRefreshTokenHash(newJwtToken)).thenReturn(newTokenHash);
        when(dao.save(any(RefreshToken.class))).thenReturn(testRefreshToken);
        when(manager.createAccessToken(any(UserPrincipal.class), any(Instant.class)))
                .thenReturn("access.token");

        // act
        AuthResponseDTO result =
                service.rotateRefreshToken(oldRefreshToken, clientId, userAgent, clientIp);

        // assert
        assertNotNull(result);
        assertEquals("access.token", result.accessToken());
        assertEquals(newJwtToken, result.refreshToken());
        verify(dao, times(2)).save(any(RefreshToken.class)); // new token + update actual token
    }

    // --------------------------
    // rotateRefreshToken - param validations
    // --------------------------
    @Test
    void rotateRefreshToken_nullToken_throwsBadRequestException() {
        BadRequestException ex =
                assertThrows(
                        BadRequestException.class,
                        () -> service.rotateRefreshToken(null, "cid", "ua", "ip"));
        assertTrue(ex.getMessage().contains("Invalid Refresh Token"));
    }

    @Test
    void rotateRefreshToken_emptyToken_throwsBadRequestException() {
        BadRequestException ex =
                assertThrows(
                        BadRequestException.class,
                        () -> service.rotateRefreshToken("", "cid", "ua", "ip"));
        assertTrue(ex.getMessage().contains("Invalid Refresh Token"));
    }

    @Test
    void rotateRefreshToken_nullClientId_throwsBadRequestException() {
        BadRequestException ex =
                assertThrows(
                        BadRequestException.class,
                        () -> service.rotateRefreshToken("tok", null, "ua", "ip"));
        assertTrue(ex.getMessage().contains("Invalid Client ID"));
    }

    @Test
    void rotateRefreshToken_nullUserAgent_throwsBadRequestException() {
        BadRequestException ex =
                assertThrows(
                        BadRequestException.class,
                        () -> service.rotateRefreshToken("tok", "cid", null, "ip"));
        assertTrue(ex.getMessage().contains("Invalid User Agent"));
    }

    @Test
    void rotateRefreshToken_nullClientIp_throwsBadRequestException() {
        BadRequestException ex =
                assertThrows(
                        BadRequestException.class,
                        () -> service.rotateRefreshToken("tok", "cid", "ua", null));
        assertTrue(ex.getMessage().contains("Invalid Client IP"));
    }

    // --------------------------
    // rotateRefreshToken - token not found -> reuse detection path
    // --------------------------
    @Test
    void rotateRefreshToken_tokenNotFound_reuseDetected() {
        String oldRefreshToken = "invalid.jwt.token";
        String tokenHash = "hashed_token";
        UUID familyId = UUID.randomUUID();

        when(manager.createRefreshTokenHash(oldRefreshToken)).thenReturn(tokenHash);
        when(dao.findByTokenHashSecure(tokenHash)).thenReturn(Optional.empty());
        when(dao.findFamilyIdByTokenHash(tokenHash)).thenReturn(Optional.of(familyId));

        UnauthorizedException ex =
                assertThrows(
                        UnauthorizedException.class,
                        () -> service.rotateRefreshToken(oldRefreshToken, "cid", "ua", "ip"));
        assertTrue(ex.getMessage().contains("Reuse Detected Refresh Token"));

        // verify revoke path invoked with reuse-detected reason
        verify(dao).revokeFamilyWithReason(eq(familyId), any(Instant.class), eq("reuse-detected"));
    }

    // --------------------------
    // rotateRefreshToken - revoked session -> Unauthorized
    // --------------------------
    @Test
    void rotateRefreshToken_revokedSession_throwsUnauthorizedException() {
        String oldRefreshToken = "valid.jwt.token";
        String tokenHash = "hashed_token";
        Session revokedSession = SessionProvider.revokedEntity(); // session.isRevoked() == true

        when(manager.createRefreshTokenHash(oldRefreshToken)).thenReturn(tokenHash);
        when(dao.findByTokenHashSecure(tokenHash)).thenReturn(Optional.of(testRefreshToken));
        when(sessionService.findSessionByFamilyId(testRefreshToken.getFamilyId()))
                .thenReturn(Optional.of(revokedSession));

        UnauthorizedException ex =
                assertThrows(
                        UnauthorizedException.class,
                        () -> service.rotateRefreshToken(oldRefreshToken, "cid", "ua", "ip"));
        assertTrue(ex.getMessage().contains("Revoked Session"));
    }

    // --------------------------
    // validateRefreshToken branches via rotateRefreshToken
    // --------------------------

    @Test
    void rotateRefreshToken_tokenWithoutFamilyId_throwsBadRequest() {
        String oldRefreshToken = "token-no-family";
        String tokenHash = "thash";
        RefreshToken tokenNoFamily = RefreshTokenProvider.singleEntity();
        tokenNoFamily.setFamilyId(null);

        when(manager.createRefreshTokenHash(oldRefreshToken)).thenReturn(tokenHash);
        when(dao.findByTokenHashSecure(tokenHash)).thenReturn(Optional.of(tokenNoFamily));

        BadRequestException ex =
                assertThrows(
                        BadRequestException.class,
                        () -> service.rotateRefreshToken(oldRefreshToken, "cid", "ua", "ip"));
        assertTrue(ex.getMessage().contains("RefreshToken without familyId"));
    }

    @Test
    void rotateRefreshToken_tokenRevokedAndReplaced_triggersReuseAndThrows() {
        String oldRefreshToken = "revoked.replaced";
        String tokenHash = "thash";
        RefreshToken revokedReplaced = RefreshTokenProvider.singleEntity();
        revokedReplaced.setRevoked(true);
        // create a replacement token minimal
        RefreshToken replacement = RefreshTokenProvider.singleEntity();
        revokedReplaced.setReplacedBy(replacement);

        when(manager.createRefreshTokenHash(oldRefreshToken)).thenReturn(tokenHash);
        when(dao.findByTokenHashSecure(tokenHash)).thenReturn(Optional.of(revokedReplaced));

        UnauthorizedException ex =
                assertThrows(
                        UnauthorizedException.class,
                        () -> service.rotateRefreshToken(oldRefreshToken, "cid", "ua", "ip"));
        assertTrue(ex.getMessage().contains("Reuse Detected Refresh Token"));

        // revoke path must be called with reuse-detected
        verify(dao)
                .revokeFamilyWithReason(
                        eq(revokedReplaced.getFamilyId()),
                        any(Instant.class),
                        eq("reuse-detected"));
        verify(sessionService)
                .revokeSessionByFamilyId(
                        eq(revokedReplaced.getFamilyId()),
                        any(Instant.class),
                        eq("reuse-detected"));
    }

    @Test
    void rotateRefreshToken_tokenRevokedWithoutReplacement_throwsUnauthorized() {
        String oldRefreshToken = "revoked.only";
        String tokenHash = "thash";
        RefreshToken revoked = RefreshTokenProvider.singleEntity();
        revoked.setRevoked(true);
        revoked.setReplacedBy(null);

        when(manager.createRefreshTokenHash(oldRefreshToken)).thenReturn(tokenHash);
        when(dao.findByTokenHashSecure(tokenHash)).thenReturn(Optional.of(revoked));

        UnauthorizedException ex =
                assertThrows(
                        UnauthorizedException.class,
                        () -> service.rotateRefreshToken(oldRefreshToken, "cid", "ua", "ip"));
        assertTrue(ex.getMessage().contains("Revoked Refresh Token"));
    }

    @Test
    void rotateRefreshToken_tokenExpired_throwsUnauthorized() {
        String oldRefreshToken = "expired.token";
        String tokenHash = "thash";
        RefreshToken expired = RefreshTokenProvider.singleEntity();
        expired.setExpiresAt(Instant.now().minusSeconds(10)); // already expired

        when(manager.createRefreshTokenHash(oldRefreshToken)).thenReturn(tokenHash);
        when(dao.findByTokenHashSecure(tokenHash)).thenReturn(Optional.of(expired));

        UnauthorizedException ex =
                assertThrows(
                        UnauthorizedException.class,
                        () -> service.rotateRefreshToken(oldRefreshToken, "cid", "ua", "ip"));
        assertTrue(ex.getMessage().contains("Expired Refresh Token"));
    }

    @Test
    void rotateRefreshToken_tokensInvalidBefore_throwsUnauthorized() {
        String oldRefreshToken = "tokens.invalid.before";
        String tokenHash = "thash";
        RefreshToken token = RefreshTokenProvider.singleEntity();
        // set token createdAt earlier than user's tokensInvalidBefore
        User user = UserProvider.singleEntity();
        user.setTokensInvalidBefore(Instant.now().plusSeconds(3600)); // future
        token.setUser(user);
        token.setCreatedAt(Instant.now());

        when(manager.createRefreshTokenHash(oldRefreshToken)).thenReturn(tokenHash);
        when(dao.findByTokenHashSecure(tokenHash)).thenReturn(Optional.of(token));

        UnauthorizedException ex =
                assertThrows(
                        UnauthorizedException.class,
                        () -> service.rotateRefreshToken(oldRefreshToken, "cid", "ua", "ip"));
        assertTrue(ex.getMessage().contains("Refresh Token issued before tokens_invalid_before"));
    }

    @Test
    void rotateRefreshToken_clientMismatch_revokesFamilyAndThrows() {
        String oldRefreshToken = "client.mismatch";
        String tokenHash = "thash";
        RefreshToken token = RefreshTokenProvider.singleEntity();
        token.setClientId("different-client"); // token has different clientId

        when(manager.createRefreshTokenHash(oldRefreshToken)).thenReturn(tokenHash);
        when(dao.findByTokenHashSecure(tokenHash)).thenReturn(Optional.of(token));

        UnauthorizedException ex =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        oldRefreshToken, "expected-client", "ua", "ip"));
        assertTrue(ex.getMessage().contains("Client mismatch"));

        verify(dao)
                .revokeFamilyWithReason(
                        eq(token.getFamilyId()), any(Instant.class), eq("client-mismatch"));
        verify(sessionService)
                .revokeSessionByFamilyId(
                        eq(token.getFamilyId()), any(Instant.class), eq("client-mismatch"));
    }

    @Test
    void rotateRefreshToken_userAgentMismatch_revokesFamilyAndThrows() {
        String oldRefreshToken = "ua.mismatch";
        String tokenHash = "thash";
        RefreshToken token = RefreshTokenProvider.singleEntity();
        token.setUserAgent("different-ua"); // token has different UA
        UUID oldFamilyId = token.getFamilyId();

        when(manager.createRefreshTokenHash(oldRefreshToken)).thenReturn(tokenHash);
        when(dao.findByTokenHashSecure(tokenHash)).thenReturn(Optional.of(token));

        UnauthorizedException ex =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        oldRefreshToken, token.getClientId(), "expected-ua", "ip"));
        assertTrue(ex.getMessage().contains("User-Agent mismatch"));

        verify(dao).revokeFamilyWithReason(eq(oldFamilyId), any(Instant.class), eq("ua-mismatch"));
        // session revoke is performed only by revokeRefreshTokensAndSessionByFamilyId
        verify(sessionService)
                .revokeSessionByFamilyId(eq(oldFamilyId), any(Instant.class), eq("ua-mismatch"));
    }

    @Test
    void rotateRefreshToken_SessionRevoked_revokesFamilyAndThrows() {
        String oldRefreshToken = "session-revoked";
        String tokenHash = "thash";
        RefreshToken token = RefreshTokenProvider.singleEntity();
        token.setUserAgent("different-ua"); // token has different UA
        UUID oldFamilyId = token.getFamilyId();
        Session session = SessionProvider.singleEntity();
        session.setRevokedAt(Instant.now().minusSeconds(1000));

        when(manager.createRefreshTokenHash(oldRefreshToken)).thenReturn(tokenHash);
        when(dao.findByTokenHashSecure(tokenHash)).thenReturn(Optional.of(token));
        when(sessionService.findSessionByFamilyId(oldFamilyId)).thenReturn(Optional.of(session));

        UnauthorizedException ex =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        oldRefreshToken,
                                        token.getClientId(),
                                        token.getUserAgent(),
                                        token.getIpAddress()));
        assertTrue(ex.getMessage().contains("Revoked Session"));
    }

    // --------------------------
    // generateJWTTokens
    // --------------------------
    @Test
    void generateJWTTokens_validInput_returnsTokens() {
        String expectedAccessToken = "access.jwt.token";
        String expectedRefreshToken = "refresh.jwt.token";
        String tokenHash = "hashed_token";
        UUID tokenJti = UUID.randomUUID();

        when(uuidv7Generator.generate()).thenReturn(tokenJti);
        when(manager.getJwtTimeExpRefresh()).thenReturn(86400000L);
        when(manager.createRefreshToken(any(RefreshToken.class))).thenReturn(expectedRefreshToken);
        when(manager.createRefreshTokenHash(expectedRefreshToken)).thenReturn(tokenHash);
        when(dao.save(any(RefreshToken.class))).thenReturn(testRefreshToken);
        when(manager.createAccessToken(userPrincipal, testSession.getLastSeenAt()))
                .thenReturn(expectedAccessToken);

        AuthResponseDTO result = service.generateJWTTokens(userPrincipal, testSession);

        assertNotNull(result);
        assertEquals(expectedAccessToken, result.accessToken());
        assertEquals(expectedRefreshToken, result.refreshToken());
        verify(dao).save(any(RefreshToken.class));
    }

    // --------------------------
    // delegations / simple dao calls
    // --------------------------
    @Test
    void revokeRefreshTokensAndSessionByFamilyId_callsDAOAndSessionService() {
        UUID familyId = UUID.randomUUID();
        Instant now = Instant.now();
        String reason = "user_logout";

        service.revokeRefreshTokensAndSessionByFamilyId(familyId, now, reason);

        verify(dao).revokeFamilyWithReason(familyId, now, reason);
        verify(sessionService).revokeSessionByFamilyId(familyId, now, reason);
    }

    @Test
    void revokeFamilyWithReason_callsDAO() {
        UUID familyId = UUID.randomUUID();
        Instant revokedAt = Instant.now();
        String reason = "admin_revoke";

        service.revokeFamilyWithReason(familyId, revokedAt, reason);

        verify(dao).revokeFamilyWithReason(familyId, revokedAt, reason);
    }

    @Test
    void findFamilyIdByTokenHash_callsDAO() {
        String tokenHash = "hashed_token";
        UUID expectedFamilyId = UUID.randomUUID();

        when(dao.findFamilyIdByTokenHash(tokenHash)).thenReturn(Optional.of(expectedFamilyId));

        Optional<UUID> result = service.findFamilyIdByTokenHash(tokenHash);

        assertTrue(result.isPresent());
        assertEquals(expectedFamilyId, result.get());
        verify(dao).findFamilyIdByTokenHash(tokenHash);
    }

    @Test
    void findByTokenHashSecure_callsDAO() {
        String tokenHash = "hashed_token";

        when(dao.findByTokenHashSecure(tokenHash)).thenReturn(Optional.of(testRefreshToken));

        Optional<RefreshToken> result = service.findByTokenHashSecure(tokenHash);

        assertTrue(result.isPresent());
        assertEquals(testRefreshToken, result.get());
        verify(dao).findByTokenHashSecure(tokenHash);
    }
}
