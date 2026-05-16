package com.alpaca.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alpaca.dto.request.AuthLoginRequestDTO;
import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.entity.RefreshToken;
import com.alpaca.entity.Role;
import com.alpaca.entity.Session;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.AuthCode;
import com.alpaca.model.UserPrincipal;
import com.alpaca.resources.SessionProvider;
import com.alpaca.resources.UserProvider;
import com.alpaca.security.manager.JJwtManager;
import com.alpaca.security.manager.TokenExchangeManager;
import com.alpaca.service.IRefreshTokenService;
import com.alpaca.service.IRoleService;
import com.alpaca.service.ISessionService;
import com.alpaca.service.IUserService;
import com.alpaca.service.impl.AuthServiceImpl;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/** Unit tests for {@link AuthServiceImpl}. */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private IRoleService roleService;
    @Mock private IUserService userService;
    @Mock private ISessionService sessionService;
    @Mock private IRefreshTokenService refreshTokenService;
    @Mock private TokenExchangeManager exchangeManager;
    @Mock private JJwtManager jJwtManager;

    @InjectMocks private AuthServiceImpl service;

    private User user;
    private UserPrincipal userPrincipal;
    private Session session;
    private AuthLoginRequestDTO loginRequest;
    private AuthResponseDTO authResponse;
    private AuthCode authCode;

    String clientId;
    String userAgent;
    String ipAddress;

    @BeforeEach
    void setUp() {
        user = UserProvider.alternativeEntity();
        userPrincipal = new UserPrincipal(user);

        session = SessionProvider.singleEntity();
        clientId = session.getClientId();
        userAgent = session.getUserAgent();
        ipAddress = session.getIpAddress();

        loginRequest =
                new AuthLoginRequestDTO(
                        user.getEmail(),
                        "Password123!",
                        session.getClientId(),
                        session.getUserAgent(),
                        session.getIpAddress());

        authResponse = new AuthResponseDTO("access-token", "refresh-token");

        authCode = new AuthCode();
        authCode.setCode("authorization-code");
        authCode.setCodeVerifier("a".repeat(43));
        authCode.setRedirectUri("https://alpaca.com/callback");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void login_WithUserPrincipal_ReturnsTokens() {
        when(sessionService.createSession(
                        userPrincipal.getUserId(),
                        loginRequest.userAgent(),
                        loginRequest.clientId(),
                        loginRequest.clientIp()))
                .thenReturn(session);

        when(refreshTokenService.generateJWTTokens(userPrincipal, session))
                .thenReturn(authResponse);

        AuthResponseDTO response = service.login(userPrincipal, loginRequest);

        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(authResponse.accessToken(), response.accessToken()),
                () -> assertEquals(authResponse.refreshToken(), response.refreshToken()));

        verify(sessionService)
                .createSession(
                        userPrincipal.getUserId(),
                        loginRequest.userAgent(),
                        loginRequest.clientId(),
                        loginRequest.clientIp());

        verify(refreshTokenService).generateJWTTokens(userPrincipal, session);
    }

    @Test
    void login_WithAuthCode_WhenCodeIsNull_ThrowsUnauthorizedException() {
        authCode.setCode(null);

        UnauthorizedException exception =
                assertThrows(UnauthorizedException.class, () -> service.login(authCode));

        assertEquals("Exchange code is required", exception.getReason());

        verifyNoInteractions(exchangeManager);
    }

    @Test
    void login_WithAuthCode_WhenCodeIsEmpty_ThrowsUnauthorizedException() {
        authCode.setCode("");

        UnauthorizedException exception =
                assertThrows(UnauthorizedException.class, () -> service.login(authCode));

        assertEquals("Exchange code is required", exception.getReason());

        verifyNoInteractions(exchangeManager);
    }

    @Test
    void login_WithAuthCode_WhenVerifierIsInvalid_ThrowsBadRequestException() {
        authCode.setCodeVerifier("invalid");

        BadRequestException exception =
                assertThrows(BadRequestException.class, () -> service.login(authCode));

        assertEquals("Invalid code-verifier format", exception.getReason());

        verifyNoInteractions(exchangeManager);
    }

    @Test
    void login_WithAuthCode_WhenCodeDoesNotExist_ThrowsUnauthorizedException() {
        when(exchangeManager.consumeCode(authCode.getCode())).thenReturn(Optional.empty());

        UnauthorizedException exception =
                assertThrows(UnauthorizedException.class, () -> service.login(authCode));

        assertEquals("Code Invalid or Expired", exception.getReason());

        verify(exchangeManager).consumeCode(authCode.getCode());
    }

    @Test
    void login_WithAuthCode_WhenChallengeDoesNotMatch_ThrowsUnauthorizedException() {
        AuthCode savedAuthCode = new AuthCode();
        savedAuthCode.setCodeChallenge("expected-challenge");
        savedAuthCode.setExpiresAt(Instant.now().plusSeconds(60));
        savedAuthCode.setRedirectUri(authCode.getRedirectUri());

        String generatedChallenge = "generated-challenge";

        when(exchangeManager.consumeCode(authCode.getCode()))
                .thenReturn(Optional.of(savedAuthCode));

        when(jJwtManager.createTokenHash(authCode.getCodeVerifier()))
                .thenReturn(generatedChallenge);

        UnauthorizedException exception =
                assertThrows(UnauthorizedException.class, () -> service.login(authCode));

        assertEquals("Code Invalid or Expired", exception.getReason());

        verify(jJwtManager).createTokenHash(authCode.getCodeVerifier());
    }

    @Test
    void login_WithAuthCode_WhenCodeIsExpired_ThrowsUnauthorizedException() {
        AuthCode savedAuthCode = new AuthCode();
        savedAuthCode.setCodeChallenge("expected-challenge");
        savedAuthCode.setExpiresAt(Instant.now().minusSeconds(1));
        savedAuthCode.setRedirectUri(authCode.getRedirectUri());

        when(exchangeManager.consumeCode(authCode.getCode()))
                .thenReturn(Optional.of(savedAuthCode));

        when(jJwtManager.createTokenHash(authCode.getCodeVerifier()))
                .thenReturn(savedAuthCode.getCodeChallenge());

        UnauthorizedException exception =
                assertThrows(UnauthorizedException.class, () -> service.login(authCode));

        assertEquals("Code Invalid or Expired", exception.getReason());
    }

    @Test
    void login_WithAuthCode_WhenRedirectUriDoesNotMatch_ThrowsUnauthorizedException() {
        AuthCode savedAuthCode = new AuthCode();
        savedAuthCode.setCodeChallenge("expected-challenge");
        savedAuthCode.setExpiresAt(Instant.now().plusSeconds(60));
        savedAuthCode.setRedirectUri("https://wrong-uri.com/callback");

        when(exchangeManager.consumeCode(authCode.getCode()))
                .thenReturn(Optional.of(savedAuthCode));

        when(jJwtManager.createTokenHash(authCode.getCodeVerifier()))
                .thenReturn(savedAuthCode.getCodeChallenge());

        UnauthorizedException exception =
                assertThrows(UnauthorizedException.class, () -> service.login(authCode));

        assertEquals("Code Invalid or Expired", exception.getReason());
    }

    @Test
    void login_WithAuthCode_ReturnsTokens() {
        AuthCode savedAuthCode = new AuthCode();
        savedAuthCode.setCodeChallenge("expected-challenge");
        savedAuthCode.setExpiresAt(Instant.now().plusSeconds(60));
        savedAuthCode.setRedirectUri(authCode.getRedirectUri());

        when(exchangeManager.consumeCode(authCode.getCode()))
                .thenReturn(Optional.of(savedAuthCode));

        when(jJwtManager.createTokenHash(authCode.getCodeVerifier()))
                .thenReturn(savedAuthCode.getCodeChallenge());

        when(refreshTokenService.generateJWTTokens(savedAuthCode)).thenReturn(authResponse);

        AuthResponseDTO response = service.login(authCode);

        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(authResponse.accessToken(), response.accessToken()),
                () -> assertEquals(authResponse.refreshToken(), response.refreshToken()));

        verify(refreshTokenService).generateJWTTokens(savedAuthCode);
    }

    @Test
    void register_WhenEmailAlreadyExists_ThrowsBadRequestException() {
        when(userService.existsByEmail(loginRequest.email())).thenReturn(true);

        BadRequestException exception =
                assertThrows(BadRequestException.class, () -> service.register(loginRequest));

        assertEquals("Email already registered", exception.getReason());

        verify(userService).existsByEmail(loginRequest.email());
        verify(userService, never()).register(any(User.class));
    }

    @Test
    void register_ReturnsTokens() {
        Set<Role> roles = Set.of(new Role());

        when(userService.existsByEmail(loginRequest.email())).thenReturn(false);

        when(roleService.getUserRoles()).thenReturn(roles);

        when(userService.register(any(User.class))).thenReturn(user);

        when(sessionService.createSession(
                        user.getId(),
                        loginRequest.userAgent(),
                        loginRequest.clientId(),
                        loginRequest.clientIp()))
                .thenReturn(session);

        when(refreshTokenService.generateJWTTokens(any(UserPrincipal.class), eq(session)))
                .thenReturn(authResponse);

        AuthResponseDTO response = service.register(loginRequest);

        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(authResponse.accessToken(), response.accessToken()),
                () -> assertEquals(authResponse.refreshToken(), response.refreshToken()));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        verify(userService).register(userCaptor.capture());

        User capturedUser = userCaptor.getValue();

        assertAll(
                () -> assertEquals(loginRequest.email(), capturedUser.getEmail()),
                () -> assertEquals(loginRequest.password(), capturedUser.getPassword()),
                () -> assertEquals(roles.stream().toList(), capturedUser.getRoles()));
    }

    @Test
    void logout_WhenRefreshTokenIsInvalid_ThrowsBadRequestException() {
        String refreshToken = " ";
        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                service.logout(
                                        refreshToken,
                                        clientId,
                                        userAgent,
                                        ipAddress));

        assertEquals("Invalid Refresh Token", exception.getReason());

        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void logout_WhenRefreshTokenDoesNotExist_ThrowsNotFoundException() {
        String refreshToken = "refresh-token";
        String refreshTokenHash = "refresh-token-hash";

        when(jJwtManager.createTokenHash(refreshToken)).thenReturn(refreshTokenHash);

        when(refreshTokenService.findByTokenHashSecure(refreshTokenHash))
                .thenReturn(Optional.empty());

        NotFoundException exception =
                assertThrows(
                        NotFoundException.class,
                        () ->
                                service.logout(
                                        refreshToken,
                                        clientId,
                                        userAgent,
                                        ipAddress));

        assertEquals("Refresh Token Not Found", exception.getReason());
    }

    @Test
    void logout_WhenRefreshTokenAlreadyRevoked_ThrowsBadRequestException() {
        String refreshToken = "refresh-token";
        String refreshTokenHash = "refresh-token-hash";

        RefreshToken storedRefreshToken = new RefreshToken();
        storedRefreshToken.setRevoked(true);

        when(jJwtManager.createTokenHash(refreshToken)).thenReturn(refreshTokenHash);

        when(refreshTokenService.findByTokenHashSecure(refreshTokenHash))
                .thenReturn(Optional.of(storedRefreshToken));

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                service.logout(
                                        refreshToken,
                                        clientId,
                                        userAgent,
                                        ipAddress));

        assertEquals("Refresh Token already revoked", exception.getReason());
    }

    @Test
    void logout_RevokesSessionAndClearsSecurityContext() {
        String refreshToken = "refresh-token";
        String refreshTokenHash = "refresh-token-hash";

        RefreshToken storedRefreshToken = new RefreshToken();
        storedRefreshToken.setRevoked(false);
        storedRefreshToken.setFamilyId(UUID.randomUUID());

        when(jJwtManager.createTokenHash(refreshToken)).thenReturn(refreshTokenHash);

        when(refreshTokenService.findByTokenHashSecure(refreshTokenHash))
                .thenReturn(Optional.of(storedRefreshToken));

        service.logout(
                refreshToken,
                clientId,
                userAgent,
                ipAddress);

        verify(refreshTokenService)
                .revokeRefreshTokensAndSessionByFamilyId(
                        eq(storedRefreshToken.getFamilyId()),
                        any(Instant.class),
                        eq("logout-session"));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void loadUserByUsername_ReturnsUserDetails() {
        when(userService.findByEmail(user.getEmail())).thenReturn(user);

        UserDetails result = service.loadUserByUsername(user.getEmail());

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(user.getEmail(), result.getUsername()));

        verify(userService).findByEmail(user.getEmail());
    }
}
