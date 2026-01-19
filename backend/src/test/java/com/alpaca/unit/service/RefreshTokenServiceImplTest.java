package com.alpaca.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock private IRefreshTokenDAO dao;
    @Mock private ISessionService sessionService;
    @Mock private JJwtManager manager;
    @Mock private UUIDv7Generator uuidv7Generator;

    @InjectMocks private RefreshTokenServiceImpl service;

    private User testUser;
    private Session testSession;
    private RefreshToken testRefreshToken;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        testUser = UserProvider.singleEntity();
        testSession = SessionProvider.singleEntity();
        testRefreshToken = RefreshTokenProvider.singleEntity();
        userPrincipal = new UserPrincipal(testUser, null);
    }

    @Test
    void rotateRefreshToken_validToken_returnsNewTokens() {
        // Arrange
        String oldRefreshToken = "valid.jwt.token";
        String clientId = "web-client";
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
        String clientIp = "127.0.0.1";
        String tokenHash = "hashed_token";
        String newJwtToken = "new.jwt.token";
        String newTokenHash = "new_hashed_token";
        UUID newTokenJti = UUID.randomUUID();
        Instant now = Instant.now();

        when(manager.createRefreshTokenHash(oldRefreshToken)).thenReturn(tokenHash);
        when(dao.findByTokenHashSecure(tokenHash)).thenReturn(Optional.of(testRefreshToken));
        when(sessionService.findSessionByFamilyId(testRefreshToken.getFamilyId()))
                .thenReturn(Optional.of(testSession));
        when(uuidv7Generator.generate()).thenReturn(newTokenJti);
        when(manager.getJwtTimeExpRefresh()).thenReturn(86400000L); // 24 hours
        when(manager.createRefreshToken(any(RefreshToken.class))).thenReturn(newJwtToken);
        when(manager.createRefreshTokenHash(newJwtToken)).thenReturn(newTokenHash);
        when(dao.save(any(RefreshToken.class))).thenReturn(testRefreshToken);
        when(manager.createAccessToken(any(UserPrincipal.class), any(Instant.class)))
                .thenReturn("access.token");

        // Act
        AuthResponseDTO result =
                service.rotateRefreshToken(oldRefreshToken, clientId, userAgent, clientIp);

        // Assert
        assertNotNull(result);
        assertEquals("access.token", result.accessToken());
        assertEquals(newJwtToken, result.refreshToken());
        verify(dao, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void rotateRefreshToken_nullToken_throwsBadRequestException() {
        // Arrange
        String clientId = "web-client";
        String userAgent = "Mozilla/5.0";
        String clientIp = "127.0.0.1";

        // Act & Assert
        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> service.rotateRefreshToken(null, clientId, userAgent, clientIp));

        assertTrue(exception.getMessage().contains("Invalid Refresh Token"));
    }

    @Test
    void rotateRefreshToken_emptyToken_throwsBadRequestException() {
        // Arrange
        String clientId = "web-client";
        String userAgent = "Mozilla/5.0";
        String clientIp = "127.0.0.1";

        // Act & Assert
        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> service.rotateRefreshToken("", clientId, userAgent, clientIp));

        assertTrue(exception.getMessage().contains("Invalid Refresh Token"));
    }

    @Test
    void rotateRefreshToken_nullClientId_throwsBadRequestException() {
        // Arrange
        String oldRefreshToken = "valid.jwt.token";
        String userAgent = "Mozilla/5.0";
        String clientIp = "127.0.0.1";

        // Act & Assert
        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                service.rotateRefreshToken(
                                        oldRefreshToken, null, userAgent, clientIp));

        assertTrue(exception.getMessage().contains("Invalid Client ID"));
    }

    @Test
    void rotateRefreshToken_tokenNotFound_reuseDetected() {
        // Arrange
        String oldRefreshToken = "invalid.jwt.token";
        String clientId = "web-client";
        String userAgent = "Mozilla/5.0";
        String clientIp = "127.0.0.1";
        String tokenHash = "hashed_token";
        UUID familyId = UUID.randomUUID();

        when(manager.createRefreshTokenHash(oldRefreshToken)).thenReturn(tokenHash);
        when(dao.findByTokenHashSecure(tokenHash)).thenReturn(Optional.empty());
        when(dao.findFamilyIdByTokenHash(tokenHash)).thenReturn(Optional.of(familyId));

        // Act & Assert
        UnauthorizedException exception =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        oldRefreshToken, clientId, userAgent, clientIp));

        assertTrue(exception.getMessage().contains("Reuse Detected Refresh Token"));
        verify(dao).revokeFamilyWithReason(eq(familyId), any(Instant.class), eq("reuse-detected"));
    }

    @Test
    void rotateRefreshToken_revokedSession_throwsUnauthorizedException() {
        // Arrange
        String oldRefreshToken = "valid.jwt.token";
        String clientId = "web-client";
        String userAgent = "Mozilla/5.0";
        String clientIp = "127.0.0.1";
        String tokenHash = "hashed_token";
        Session revokedSession = SessionProvider.revokedEntity();

        when(manager.createRefreshTokenHash(oldRefreshToken)).thenReturn(tokenHash);
        when(dao.findByTokenHashSecure(tokenHash)).thenReturn(Optional.of(testRefreshToken));
        when(sessionService.findSessionByFamilyId(testRefreshToken.getFamilyId()))
                .thenReturn(Optional.of(revokedSession));

        // Act & Assert
        UnauthorizedException exception =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        oldRefreshToken, clientId, userAgent, clientIp));

        assertTrue(exception.getMessage().contains("Revoked Session"));
    }

    @Test
    void generateJWTTokens_validInput_returnsTokens() {
        // Arrange
        String expectedAccessToken = "access.jwt.token";
        String expectedRefreshToken = "refresh.jwt.token";
        String tokenHash = "hashed_token";
        UUID tokenJti = UUID.randomUUID();

        when(uuidv7Generator.generate()).thenReturn(tokenJti);
        when(manager.getJwtTimeExpRefresh()).thenReturn(86400000L); // 24 hours
        when(manager.createRefreshToken(any(RefreshToken.class))).thenReturn(expectedRefreshToken);
        when(manager.createRefreshTokenHash(expectedRefreshToken)).thenReturn(tokenHash);
        when(dao.save(any(RefreshToken.class))).thenReturn(testRefreshToken);
        when(manager.createAccessToken(userPrincipal, testSession.getLastSeenAt()))
                .thenReturn(expectedAccessToken);

        // Act
        AuthResponseDTO result = service.generateJWTTokens(userPrincipal, testSession);

        // Assert
        assertNotNull(result);
        assertEquals(expectedAccessToken, result.accessToken());
        assertEquals(expectedRefreshToken, result.refreshToken());
        verify(dao).save(any(RefreshToken.class));
    }

    @Test
    void revokeRefreshTokensAndSessionByFamilyId_callsDAO() {
        // Arrange
        UUID familyId = UUID.randomUUID();
        Instant now = Instant.now();
        String reason = "user_logout";

        // Act
        service.revokeRefreshTokensAndSessionByFamilyId(familyId, now, reason);

        // Assert
        verify(dao).revokeFamilyWithReason(familyId, now, reason);
    }

    @Test
    void revokeFamilyWithReason_callsDAO() {
        // Arrange
        UUID familyId = UUID.randomUUID();
        Instant revokedAt = Instant.now();
        String reason = "admin_revoke";

        // Act
        service.revokeFamilyWithReason(familyId, revokedAt, reason);

        // Assert
        verify(dao).revokeFamilyWithReason(familyId, revokedAt, reason);
    }

    @Test
    void findFamilyIdByTokenHash_callsDAO() {
        // Arrange
        String tokenHash = "hashed_token";
        UUID expectedFamilyId = UUID.randomUUID();

        when(dao.findFamilyIdByTokenHash(tokenHash)).thenReturn(Optional.of(expectedFamilyId));

        // Act
        Optional<UUID> result = service.findFamilyIdByTokenHash(tokenHash);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(expectedFamilyId, result.get());
        verify(dao).findFamilyIdByTokenHash(tokenHash);
    }

    @Test
    void findByTokenHashSecure_callsDAO() {
        // Arrange
        String tokenHash = "hashed_token";

        when(dao.findByTokenHashSecure(tokenHash)).thenReturn(Optional.of(testRefreshToken));

        // Act
        Optional<RefreshToken> result = service.findByTokenHashSecure(tokenHash);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testRefreshToken, result.get());
        verify(dao).findByTokenHashSecure(tokenHash);
    }
}
