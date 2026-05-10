package com.alpaca.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.alpaca.security.manager.PasswordManager;
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
    @Mock private PasswordManager passwordManager;
    @Mock private TokenExchangeManager exchangeManager;
    @Mock private JJwtManager manager;
    @Mock private JJwtManager jJwtManager;

    @InjectMocks private AuthServiceImpl service;

    private User user;
    private AuthLoginRequestDTO loginRequest;
    private Session session;
    private AuthResponseDTO authResponse;
    private AuthCode authCode;

    @BeforeEach
    void setup() {
        service =
                new AuthServiceImpl(
                        roleService,
                        userService,
                        sessionService,
                        refreshTokenService,
                        passwordManager,
                        manager,
                        exchangeManager,
                        jJwtManager);
        user = UserProvider.alternativeEntity();
        loginRequest =
                new AuthLoginRequestDTO(
                        user.getEmail(), "Password123!", "client-id", "user-agent", "127.0.0.1");
        session = SessionProvider.singleEntity();
        authResponse = new AuthResponseDTO("access-token", "refresh-token");
        authCode = new AuthCode();
        authCode.setCode("valid-code");
        authCode.setCodeVerifier("a".repeat(43)); // Valid PKCE verifier length
        authCode.setRedirectUri("[https://alpaca.com/callback](https://alpaca.com/callback)");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void login_WithPrincipal_Success() {
        UserPrincipal principal = new UserPrincipal(user);

        when(sessionService.createSession(
                        principal.getUserId(),
                        loginRequest.userAgent(),
                        loginRequest.clientId(),
                        loginRequest.clientIp()))
                .thenReturn(session);
        when(refreshTokenService.generateJWTTokens(principal, session)).thenReturn(authResponse);

        AuthResponseDTO result = service.login(principal, loginRequest);

        assertNotNull(result);
        assertEquals(authResponse.accessToken(), result.accessToken());
    }

    @Test
    void login_WithAuthCode_NullCode_ThrowsUnauthorized() {
        authCode.setCode(null);
        assertThrows(UnauthorizedException.class, () -> service.login(authCode));
    }

    @Test
    void login_WithAuthCode_InvalidVerifier_ThrowsBadRequest() {
        authCode.setCodeVerifier("invalid");
        assertThrows(BadRequestException.class, () -> service.login(authCode));
    }

    @Test
    void login_WithAuthCode_CodeNotFound_ThrowsUnauthorized() {
        when(exchangeManager.consumeCode(authCode.getCode())).thenReturn(Optional.empty());
        assertThrows(UnauthorizedException.class, () -> service.login(authCode));
    }

    @Test
    void login_WithAuthCode_HashMismatch_ThrowsUnauthorized() {
        AuthCode savedCode = new AuthCode();
        savedCode.setCodeChallenge("expected-challenge");
        String mismatchValue = authCode.getRedirectUri();

        when(exchangeManager.consumeCode(authCode.getCode())).thenReturn(Optional.of(savedCode));
        when(jJwtManager.createTokenHash(authCode.getCodeVerifier())).thenReturn(mismatchValue);

        assertThrows(UnauthorizedException.class, () -> service.login(authCode));
    }

    @Test
    void login_WithAuthCode_Expired_ThrowsUnauthorized() {
        AuthCode savedCode = new AuthCode();
        savedCode.setCodeChallenge("hash");
        savedCode.setExpiresAt(Instant.now().minusSeconds(10));

        when(exchangeManager.consumeCode(authCode.getCode())).thenReturn(Optional.of(savedCode));
        when(jJwtManager.createTokenHash(authCode.getCodeVerifier()))
                .thenReturn(savedCode.getCodeChallenge());

        assertThrows(UnauthorizedException.class, () -> service.login(authCode));
    }

    @Test
    void login_WithAuthCode_RedirectUriMismatch_ThrowsUnauthorized() {
        AuthCode savedCode = new AuthCode();
        savedCode.setCodeChallenge("hash");
        savedCode.setExpiresAt(Instant.now().plusSeconds(60));
        savedCode.setRedirectUri("[https://wrong.com](https://wrong.com)");

        when(exchangeManager.consumeCode(authCode.getCode())).thenReturn(Optional.of(savedCode));
        when(jJwtManager.createTokenHash(authCode.getCodeVerifier()))
                .thenReturn(savedCode.getCodeChallenge());

        assertThrows(UnauthorizedException.class, () -> service.login(authCode));
    }

    @Test
    void login_WithAuthCode_Success() {
        AuthCode savedCode = new AuthCode();
        savedCode.setCodeChallenge("hash");
        savedCode.setExpiresAt(Instant.now().plusSeconds(60));
        savedCode.setRedirectUri(authCode.getRedirectUri());

        when(exchangeManager.consumeCode(authCode.getCode())).thenReturn(Optional.of(savedCode));
        when(jJwtManager.createTokenHash(authCode.getCodeVerifier()))
                .thenReturn(savedCode.getCodeChallenge());
        when(refreshTokenService.generateJWTTokens(savedCode)).thenReturn(authResponse);

        AuthResponseDTO result = service.login(authCode);

        assertNotNull(result);
        assertEquals(authResponse.accessToken(), result.accessToken());
    }

    @Test
    void register_EmailExists_ThrowsBadRequest() {
        when(userService.existsByEmail(loginRequest.email())).thenReturn(true);
        assertThrows(BadRequestException.class, () -> service.register(loginRequest));
    }

    @Test
    void register_Success() {
        Set<Role> roles = Set.of(new Role());
        when(userService.existsByEmail(loginRequest.email())).thenReturn(false);
        when(roleService.getUserRoles()).thenReturn(roles);
        when(userService.register(any(User.class))).thenReturn(user);
        when(sessionService.createSession(
                        any(),
                        eq(loginRequest.userAgent()),
                        eq(loginRequest.clientId()),
                        eq(loginRequest.clientIp())))
                .thenReturn(session);
        when(refreshTokenService.generateJWTTokens(any(UserPrincipal.class), eq(session)))
                .thenReturn(authResponse);

        AuthResponseDTO result = service.register(loginRequest);

        assertNotNull(result);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).register(captor.capture());
        assertEquals(loginRequest.email(), captor.getValue().getEmail());
    }

    @Test
    void logout_InvalidToken_ThrowsBadRequest() {
        assertThrows(BadRequestException.class, () -> service.logout("", "c", "u", "i"));
    }

    @Test
    void logout_TokenNotFound_ThrowsNotFound() {
        String token = "token";
        String hash = "hash";
        when(manager.createTokenHash(token)).thenReturn(hash);
        when(refreshTokenService.findByTokenHashSecure(hash)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.logout(token, "c", "u", "i"));
    }

    @Test
    void logout_AlreadyRevoked_ThrowsBadRequest() {
        String token = "token";
        RefreshToken rt = new RefreshToken();
        rt.setRevoked(true);
        when(manager.createTokenHash(token)).thenReturn("hash");
        when(refreshTokenService.findByTokenHashSecure("hash")).thenReturn(Optional.of(rt));

        assertThrows(BadRequestException.class, () -> service.logout(token, "c", "u", "i"));
    }

    @Test
    void logout_Success() {
        String token = "token";
        RefreshToken rt = new RefreshToken();
        rt.setRevoked(false);
        rt.setFamilyId(UUID.randomUUID());
        when(manager.createTokenHash(token)).thenReturn("hash");
        when(refreshTokenService.findByTokenHashSecure("hash")).thenReturn(Optional.of(rt));

        service.logout(token, "c", "u", "i");

        verify(refreshTokenService)
                .revokeRefreshTokensAndSessionByFamilyId(
                        eq(rt.getFamilyId()), any(Instant.class), anyString());
    }

    @Test
    void loadUserByUsername_Success() {
        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        UserDetails result = service.loadUserByUsername(user.getEmail());
        assertEquals(user.getEmail(), result.getUsername());
    }
}
