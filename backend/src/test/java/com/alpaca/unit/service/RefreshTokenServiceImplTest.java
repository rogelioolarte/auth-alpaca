package com.alpaca.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.entity.RefreshToken;
import com.alpaca.entity.Session;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.AuthCode;
import com.alpaca.model.UserPrincipal;
import com.alpaca.persistence.IRefreshTokenDAO;
import com.alpaca.resources.RefreshTokenProvider;
import com.alpaca.resources.SessionProvider;
import com.alpaca.resources.UserProvider;
import com.alpaca.security.manager.JJwtManager;
import com.alpaca.service.ISessionService;
import com.alpaca.service.IUserService;
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

/** Unit tests for {@link RefreshTokenServiceImpl}. */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock private IRefreshTokenDAO dao;
    @Mock private ISessionService sessionService;
    @Mock private IUserService userService;
    @Mock private JJwtManager manager;
    @Mock private UUIDv7Generator uuidv7Generator;

    @InjectMocks private RefreshTokenServiceImpl service;

    private RefreshToken testRefreshToken;
    private Session testSession;
    private User testUser;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        testUser = UserProvider.singleEntity();
        testSession = SessionProvider.singleEntity();
        testRefreshToken = RefreshTokenProvider.singleEntity();
        testRefreshToken.setUser(testUser);
        testRefreshToken.setFamilyId(testSession.getFamilyId());
        userPrincipal = new UserPrincipal(testUser);
    }

    @Test
    void rotateRefreshToken_ValidToken_ReturnsNewTokens() {
        String oldToken = "valid.old.token";
        String hashed = "hashed_old";
        String newToken = "new.token";
        String newHash = "hashed_new";
        UUID newJti = UUID.randomUUID();

        when(manager.createTokenHash(oldToken)).thenReturn(hashed);
        when(dao.findByTokenHashSecure(hashed)).thenReturn(Optional.of(testRefreshToken));
        when(sessionService.findSessionByFamilyId(testRefreshToken.getFamilyId()))
                .thenReturn(Optional.of(testSession));
        when(uuidv7Generator.generate()).thenReturn(newJti);
        when(manager.getJwtTimeExpRefresh()).thenReturn(1000L);
        when(manager.createRefreshToken(any(RefreshToken.class))).thenReturn(newToken);
        when(manager.createTokenHash(newToken)).thenReturn(newHash);
        when(dao.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
        when(manager.createAccessToken(any(UserPrincipal.class), any(Instant.class)))
                .thenReturn("access_token");

        AuthResponseDTO response =
                service.rotateRefreshToken(
                        oldToken,
                        testRefreshToken.getClientId(),
                        testRefreshToken.getUserAgent(),
                        "127.0.0.1");

        assertNotNull(response);
        assertEquals(newToken, response.refreshToken());
        verify(dao, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void rotateRefreshToken_InvalidParams_ThrowsBadRequest() {
        assertThrows(
                BadRequestException.class, () -> service.rotateRefreshToken("", "c", "u", "i"));
        assertThrows(
                BadRequestException.class, () -> service.rotateRefreshToken("t", "", "u", "i"));
        assertThrows(
                BadRequestException.class, () -> service.rotateRefreshToken("t", "c", "", "i"));
        assertThrows(
                BadRequestException.class, () -> service.rotateRefreshToken("t", "c", "u", ""));
    }

    @Test
    void rotateRefreshToken_TokenNotFound_ReuseDetection() {
        String oldToken = "ghost.token";
        String hashed = "hashed_ghost";
        UUID familyId = UUID.randomUUID();

        when(manager.createTokenHash(oldToken)).thenReturn(hashed);
        when(dao.findByTokenHashSecure(hashed)).thenReturn(Optional.empty());
        when(dao.findFamilyIdByTokenHash(hashed)).thenReturn(Optional.of(familyId));

        assertThrows(
                UnauthorizedException.class,
                () -> service.rotateRefreshToken(oldToken, "c", "u", "i"));
        verify(dao).revokeFamilyWithReason(eq(familyId), any(), eq("reuse-detected"));
        verify(sessionService).revokeSessionByFamilyId(eq(familyId), any(), eq("reuse-detected"));
    }

    @Test
    void rotateRefreshToken_NoTokenAndNoFamily_ThrowsUnauthorized() {
        when(manager.createTokenHash(anyString())).thenReturn("hash");
        when(dao.findByTokenHashSecure(anyString())).thenReturn(Optional.empty());
        when(dao.findFamilyIdByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThrows(
                UnauthorizedException.class, () -> service.rotateRefreshToken("t", "c", "u", "i"));
    }

    @Test
    void rotateRefreshToken_MissingUserOrFamily_ThrowsBadRequest() {
        String token = "token";
        testRefreshToken.setUser(null);
        when(manager.createTokenHash(token)).thenReturn("h");
        when(dao.findByTokenHashSecure("h")).thenReturn(Optional.of(testRefreshToken));

        assertThrows(
                BadRequestException.class, () -> service.rotateRefreshToken(token, "c", "u", "i"));

        testRefreshToken.setUser(testUser);
        testRefreshToken.setFamilyId(null);
        assertThrows(
                BadRequestException.class, () -> service.rotateRefreshToken(token, "c", "u", "i"));
    }

    @Test
    void rotateRefreshToken_SessionRevoked_ThrowsUnauthorized() {
        testSession.setRevoked(true);
        when(manager.createTokenHash(anyString())).thenReturn("h");
        when(dao.findByTokenHashSecure("h")).thenReturn(Optional.of(testRefreshToken));
        when(sessionService.findSessionByFamilyId(any())).thenReturn(Optional.of(testSession));

        assertThrows(
                UnauthorizedException.class, () -> service.rotateRefreshToken("t", "c", "u", "i"));
    }

    @Test
    void validateRefreshToken_RevokedWithReplacement_ReuseDetection() {
        testRefreshToken.setRevoked(true);
        testRefreshToken.setReplacedBy(new RefreshToken());
        when(manager.createTokenHash(anyString())).thenReturn("h");
        when(dao.findByTokenHashSecure("h")).thenReturn(Optional.of(testRefreshToken));
        when(sessionService.findSessionByFamilyId(any())).thenReturn(Optional.of(testSession));

        assertThrows(
                UnauthorizedException.class, () -> service.rotateRefreshToken("t", "c", "u", "i"));
        verify(dao)
                .revokeFamilyWithReason(
                        eq(testRefreshToken.getFamilyId()), any(), eq("reuse-detected"));
    }

    @Test
    void validateRefreshToken_Expired_ThrowsUnauthorized() {
        testRefreshToken.setExpiresAt(Instant.now().minusSeconds(60));
        when(manager.createTokenHash(anyString())).thenReturn("h");
        when(dao.findByTokenHashSecure("h")).thenReturn(Optional.of(testRefreshToken));
        when(sessionService.findSessionByFamilyId(any())).thenReturn(Optional.of(testSession));

        assertThrows(
                UnauthorizedException.class, () -> service.rotateRefreshToken("t", "c", "u", "i"));
    }

    @Test
    void validateRefreshToken_IssuedBeforeInvalidation_ThrowsUnauthorized() {
        testUser.setTokensInvalidBefore(Instant.now().plusSeconds(60));
        testRefreshToken.setCreatedAt(Instant.now());
        when(manager.createTokenHash(anyString())).thenReturn("h");
        when(dao.findByTokenHashSecure("h")).thenReturn(Optional.of(testRefreshToken));
        when(sessionService.findSessionByFamilyId(any())).thenReturn(Optional.of(testSession));

        assertThrows(
                UnauthorizedException.class, () -> service.rotateRefreshToken("t", "c", "u", "i"));
    }

    @Test
    void validateRefreshToken_MismatchedClientOrUA_RevokesAndThrows() {
        when(manager.createTokenHash(anyString())).thenReturn("h");
        when(dao.findByTokenHashSecure("h")).thenReturn(Optional.of(testRefreshToken));
        when(sessionService.findSessionByFamilyId(any())).thenReturn(Optional.of(testSession));

        assertThrows(
                UnauthorizedException.class,
                () ->
                        service.rotateRefreshToken(
                                "t", "wrong-client", testRefreshToken.getUserAgent(), "i"));
        verify(dao).revokeFamilyWithReason(any(), any(), eq("client-mismatch"));

        assertThrows(
                UnauthorizedException.class,
                () ->
                        service.rotateRefreshToken(
                                "t", testRefreshToken.getClientId(), "wrong-ua", "i"));
        verify(dao).revokeFamilyWithReason(any(), any(), eq("ua-mismatch"));
    }

    @Test
    void generateJWTTokens_WithUserPrincipal_Success() {
        when(uuidv7Generator.generate()).thenReturn(UUID.randomUUID());
        when(manager.getJwtTimeExpRefresh()).thenReturn(1000L);
        when(manager.createRefreshToken(any())).thenReturn("rt");
        when(manager.createTokenHash("rt")).thenReturn("h");
        when(manager.createAccessToken(any(), any())).thenReturn("at");

        AuthResponseDTO response = service.generateJWTTokens(userPrincipal, testSession);

        assertNotNull(response);
        assertEquals("at", response.accessToken());
        verify(dao).save(any(RefreshToken.class));
    }

    @Test
    void generateJWTTokens_WithAuthCode_Success() {
        AuthCode code =
                new AuthCode("code", "challenge", "cid", "ua", "ip", testUser.getId(), "uri");
        when(userService.findById(testUser.getId())).thenReturn(testUser);
        when(sessionService.createSession(any(), any(), any(), any())).thenReturn(testSession);
        when(uuidv7Generator.generate()).thenReturn(UUID.randomUUID());
        when(manager.createRefreshToken(any())).thenReturn("rt");
        when(manager.createTokenHash("rt")).thenReturn("h");
        when(manager.createAccessToken(any(), any())).thenReturn("at");

        AuthResponseDTO response = service.generateJWTTokens(code);

        assertNotNull(response);
        verify(dao).save(any(RefreshToken.class));
    }

    @Test
    void delegations_VerifyDaoCalls() {
        String hash = "hash";
        service.findFamilyIdByTokenHash(hash);
        verify(dao).findFamilyIdByTokenHash(hash);

        service.findByTokenHashSecure(hash);
        verify(dao).findByTokenHashSecure(hash);
    }
}
