package com.alpaca.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.alpaca.dto.request.AuthLoginRequestDTO;
import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.entity.RefreshToken;
import com.alpaca.entity.Role;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.AuthCode;
import com.alpaca.model.UserPrincipal;
import com.alpaca.resources.provider.RefreshTokenProvider;
import com.alpaca.resources.provider.RoleProvider;
import com.alpaca.resources.provider.UserProvider;
import com.alpaca.resources.utility.BaseIntegrationTests;
import com.alpaca.security.manager.JJwtManager;
import com.alpaca.security.manager.TokenExchangeManager;
import com.alpaca.service.IRefreshTokenService;
import com.alpaca.service.IRoleService;
import com.alpaca.service.ISessionService;
import com.alpaca.service.IUserService;
import com.alpaca.service.impl.AuthServiceImpl;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link AuthServiceImpl} */
@DisplayName("AuthServiceImpl Integration Tests")
class AuthServiceImplIT extends BaseIntegrationTests {

    @Autowired private AuthServiceImpl authService;

    @Autowired private IUserService userService;

    @Autowired private IRoleService roleService;

    @Autowired private JJwtManager jwtManager;

    @MockitoBean private TokenExchangeManager exchangeManager;

    @MockitoBean private IRefreshTokenService refreshTokenService;

    @MockitoBean private ISessionService sessionService;

    private AuthLoginRequestDTO loginRequest;

    private Instant now;

    @BeforeEach
    void setup() {
        now = Instant.now();

        User template = UserProvider.singleTemplate();

        loginRequest =
                new AuthLoginRequestDTO(
                        template.getEmail(),
                        "Password123!",
                        "JUnit-Agent",
                        "test-client",
                        "127.0.0.1");

        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // register
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("register: should create user and return tokens")
    void register_shouldCreateUserAndReturnTokens() {
        Role role = RoleProvider.singleTemplate();
        role.setCreatedAt(now);
        role.setName("TEST_USER");

        roleService.save(role);

        AuthResponseDTO expected = new AuthResponseDTO("access-token", "refresh-token");

        when(refreshTokenService.generateJWTTokens(any(UserPrincipal.class), any()))
                .thenReturn(expected);

        AuthResponseDTO response = authService.register(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");

        assertThat(userService.existsByEmail(loginRequest.email())).isTrue();

        verify(sessionService)
                .createSession(
                        any(),
                        eq(loginRequest.userAgent()),
                        eq(loginRequest.clientId()),
                        eq(loginRequest.clientIp()));

        verify(refreshTokenService).generateJWTTokens(any(UserPrincipal.class), any());
    }

    @Test
    @Transactional
    @DisplayName("register: should throw when email already exists")
    void register_shouldThrowWhenEmailAlreadyExists() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);

        userService.save(user);

        assertThatThrownBy(() -> authService.register(loginRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email already registered");
    }

    // -------------------------------------------------------------------------
    // login(UserPrincipal)
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("login(UserPrincipal): should create session and generate tokens")
    void loginUserPrincipal_shouldCreateSessionAndGenerateTokens() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);

        User savedUser = userService.save(user);

        UserPrincipal principal = new UserPrincipal(savedUser);

        AuthResponseDTO expected = new AuthResponseDTO("access", "refresh");

        when(refreshTokenService.generateJWTTokens(any(UserPrincipal.class), any()))
                .thenReturn(expected);

        AuthResponseDTO response = authService.login(principal, loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");

        verify(sessionService)
                .createSession(
                        savedUser.getId(),
                        loginRequest.userAgent(),
                        loginRequest.clientId(),
                        loginRequest.clientIp());

        verify(refreshTokenService).generateJWTTokens(any(UserPrincipal.class), any());
    }

    // -------------------------------------------------------------------------
    // login(AuthCode)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("login(AuthCode): should throw when code is null")
    void loginAuthCode_shouldThrowWhenCodeIsNull() {
        AuthCode authCode = new AuthCode();
        authCode.setCode(null);

        assertThatThrownBy(() -> authService.login(authCode))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Exchange code is required");
    }

    @Test
    @DisplayName("login(AuthCode): should throw when code is empty")
    void loginAuthCode_shouldThrowWhenCodeIsEmpty() {
        AuthCode authCode = new AuthCode();
        authCode.setCode("");

        assertThatThrownBy(() -> authService.login(authCode))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Exchange code is required");
    }

    @Test
    @DisplayName("login(AuthCode): should throw when verifier is null")
    void loginAuthCode_shouldThrowWhenVerifierIsNull() {
        AuthCode authCode = new AuthCode();
        authCode.setCode("valid-code");
        authCode.setCodeVerifier(null);

        assertThatThrownBy(() -> authService.login(authCode))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("login(AuthCode): should throw when verifier format is invalid")
    void loginAuthCode_shouldThrowWhenVerifierIsInvalid() {
        AuthCode authCode = new AuthCode();
        authCode.setCode("valid-code");
        authCode.setCodeVerifier("short");

        assertThatThrownBy(() -> authService.login(authCode))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid code-verifier format");
    }

    @Test
    @DisplayName("login(AuthCode): should throw when auth code does not exist")
    void loginAuthCode_shouldThrowWhenAuthCodeDoesNotExist() {
        String verifier = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890-._~";

        AuthCode authCode = new AuthCode();
        authCode.setCode("missing-code");
        authCode.setCodeVerifier(verifier);

        when(exchangeManager.consumeCode("missing-code")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(authCode))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Code Invalid or Expired");
    }

    @Test
    @DisplayName("login(AuthCode): should throw when challenge does not match")
    void loginAuthCode_shouldThrowWhenChallengeDoesNotMatch() {
        String verifier = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890-._~";

        AuthCode savedCode = new AuthCode();
        savedCode.setCode("valid-code");
        savedCode.setCodeChallenge("invalid-challenge");
        savedCode.setRedirectUri("http://localhost/callback");
        savedCode.setExpiresAt(now.plusSeconds(60));

        AuthCode request = new AuthCode();
        request.setCode("valid-code");
        request.setCodeVerifier(verifier);
        request.setRedirectUri("http://localhost/callback");

        when(exchangeManager.consumeCode("valid-code")).thenReturn(Optional.of(savedCode));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Code Invalid or Expired");
    }

    @Test
    @DisplayName("login(AuthCode): should throw when auth code is expired")
    void loginAuthCode_shouldThrowWhenCodeExpired() {
        String verifier = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890-._~";

        String challenge = jwtManager.createTokenHash(verifier);

        AuthCode savedCode = new AuthCode();
        savedCode.setCode("valid-code");
        savedCode.setCodeChallenge(challenge);
        savedCode.setRedirectUri("http://localhost/callback");
        savedCode.setExpiresAt(now.minusSeconds(1));

        AuthCode request = new AuthCode();
        request.setCode("valid-code");
        request.setCodeVerifier(verifier);
        request.setRedirectUri("http://localhost/callback");

        when(exchangeManager.consumeCode("valid-code")).thenReturn(Optional.of(savedCode));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Code Invalid or Expired");
    }

    @Test
    @DisplayName("login(AuthCode): should throw when redirect uri mismatches")
    void loginAuthCode_shouldThrowWhenRedirectUriMismatch() {
        String verifier = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890-._~";

        String challenge = jwtManager.createTokenHash(verifier);

        AuthCode savedCode = new AuthCode();
        savedCode.setCode("valid-code");
        savedCode.setCodeChallenge(challenge);
        savedCode.setRedirectUri("http://localhost/callback");
        savedCode.setExpiresAt(now.plusSeconds(60));

        AuthCode request = new AuthCode();
        request.setCode("valid-code");
        request.setCodeVerifier(verifier);
        request.setRedirectUri("http://localhost/other");

        when(exchangeManager.consumeCode("valid-code")).thenReturn(Optional.of(savedCode));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Code Invalid or Expired");
    }

    @Test
    @DisplayName("login(AuthCode): should generate jwt tokens when auth code is valid")
    void loginAuthCode_shouldGenerateJwtTokensWhenValid() {
        String verifier = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890-._~";

        String challenge = jwtManager.createTokenHash(verifier);

        AuthCode savedCode = new AuthCode();
        savedCode.setCode("valid-code");
        savedCode.setCodeChallenge(challenge);
        savedCode.setRedirectUri("http://localhost/callback");
        savedCode.setExpiresAt(now.plusSeconds(60));
        savedCode.setUserAgent("JUnit-Agent");
        savedCode.setClientId("test-client");
        savedCode.setClientIp("127.0.0.1");

        AuthCode request = new AuthCode();
        request.setCode("valid-code");
        request.setCodeVerifier(verifier);
        request.setRedirectUri("http://localhost/callback");
        request.setUserAgent("JUnit-Agent");
        request.setClientId("test-client");
        request.setClientIp("127.0.0.1");

        AuthResponseDTO expected = new AuthResponseDTO("access", "refresh");

        when(exchangeManager.consumeCode("valid-code")).thenReturn(Optional.of(savedCode));

        when(refreshTokenService.generateJWTTokens(savedCode)).thenReturn(expected);

        AuthResponseDTO response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");

        verify(refreshTokenService).generateJWTTokens(savedCode);
    }

    @Test
    @DisplayName("login(AuthCode): should throw when client id mismatches")
    void loginAuthCode_shouldThrowWhenClientIdMismatch() {

        String verifier = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890-._~";

        String challenge = jwtManager.createTokenHash(verifier);

        AuthCode savedCode = new AuthCode();
        savedCode.setCode("valid-code");
        savedCode.setCodeChallenge(challenge);
        savedCode.setRedirectUri("http://localhost/callback");
        savedCode.setExpiresAt(now.plusSeconds(60));
        savedCode.setClientId("expected-client");
        savedCode.setUserAgent("JUnit-Agent");
        savedCode.setClientIp("127.0.0.1");

        AuthCode request = new AuthCode();
        request.setCode("valid-code");
        request.setCodeVerifier(verifier);
        request.setRedirectUri("http://localhost/callback");
        request.setClientId("another-client");
        request.setUserAgent("JUnit-Agent");
        request.setClientIp("127.0.0.1");

        when(exchangeManager.consumeCode("valid-code")).thenReturn(Optional.of(savedCode));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Code Invalid or Expired");

        verify(refreshTokenService, never()).generateJWTTokens(any(AuthCode.class));
    }

    @Test
    @DisplayName("login(AuthCode): should throw when user agent mismatches")
    void loginAuthCode_shouldThrowWhenUserAgentMismatch() {

        String verifier = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890-._~";

        String challenge = jwtManager.createTokenHash(verifier);

        AuthCode savedCode = new AuthCode();
        savedCode.setCode("valid-code");
        savedCode.setCodeChallenge(challenge);
        savedCode.setRedirectUri("http://localhost/callback");
        savedCode.setExpiresAt(now.plusSeconds(60));
        savedCode.setClientId("test-client");
        savedCode.setUserAgent("Expected-Agent");
        savedCode.setClientIp("127.0.0.1");

        AuthCode request = new AuthCode();
        request.setCode("valid-code");
        request.setCodeVerifier(verifier);
        request.setRedirectUri("http://localhost/callback");
        request.setClientId("test-client");
        request.setUserAgent("Another-Agent");
        request.setClientIp("127.0.0.1");

        when(exchangeManager.consumeCode("valid-code")).thenReturn(Optional.of(savedCode));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Code Invalid or Expired");

        verify(refreshTokenService, never()).generateJWTTokens(any(AuthCode.class));
    }

    @Test
    @DisplayName("login(AuthCode): should throw when client ip mismatches")
    void loginAuthCode_shouldThrowWhenClientIpMismatch() {

        String verifier = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890-._~";

        String challenge = jwtManager.createTokenHash(verifier);

        AuthCode savedCode = new AuthCode();
        savedCode.setCode("valid-code");
        savedCode.setCodeChallenge(challenge);
        savedCode.setRedirectUri("http://localhost/callback");
        savedCode.setExpiresAt(now.plusSeconds(60));
        savedCode.setClientId("test-client");
        savedCode.setUserAgent("JUnit-Agent");
        savedCode.setClientIp("127.0.0.1");

        AuthCode request = new AuthCode();
        request.setCode("valid-code");
        request.setCodeVerifier(verifier);
        request.setRedirectUri("http://localhost/callback");
        request.setClientId("test-client");
        request.setUserAgent("JUnit-Agent");
        request.setClientIp("10.10.10.10");

        when(exchangeManager.consumeCode("valid-code")).thenReturn(Optional.of(savedCode));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Code Invalid or Expired");

        verify(refreshTokenService, never()).generateJWTTokens(any(AuthCode.class));
    }

    @Test
    @DisplayName(
            "login(AuthCode): should consume auth code and generate jwt tokens when all validations"
                    + " succeed")
    void loginAuthCode_shouldConsumeCodeAndGenerateTokens_WhenAllValidationsSucceed() {

        String verifier = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890-._~";

        String challenge = jwtManager.createTokenHash(verifier);

        AuthCode savedCode = new AuthCode();
        savedCode.setCode("valid-code");
        savedCode.setCodeChallenge(challenge);
        savedCode.setRedirectUri("http://localhost/callback");
        savedCode.setExpiresAt(now.plusSeconds(60));
        savedCode.setClientId("test-client");
        savedCode.setUserAgent("JUnit-Agent");
        savedCode.setClientIp("127.0.0.1");

        AuthCode request = new AuthCode();
        request.setCode("valid-code");
        request.setCodeVerifier(verifier);
        request.setRedirectUri("http://localhost/callback");
        request.setClientId("test-client");
        request.setUserAgent("JUnit-Agent");
        request.setClientIp("127.0.0.1");

        AuthResponseDTO expected =
                new AuthResponseDTO("generated-access-token", "generated-refresh-token");

        when(exchangeManager.consumeCode("valid-code")).thenReturn(Optional.of(savedCode));

        when(refreshTokenService.generateJWTTokens(savedCode)).thenReturn(expected);

        AuthResponseDTO response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo(expected.accessToken());
        assertThat(response.refreshToken()).isEqualTo(expected.refreshToken());

        verify(exchangeManager).consumeCode("valid-code");
        verify(refreshTokenService).generateJWTTokens(savedCode);
    }

    // -------------------------------------------------------------------------
    // logout
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("logout: should throw when refresh token is blank")
    void logout_shouldThrowWhenRefreshTokenIsBlank() {
        assertThatThrownBy(() -> authService.logout("", "cli", "ua", "ip"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid Refresh Token");
    }

    @Test
    @DisplayName("logout: should throw when refresh token is not found")
    void logout_shouldThrowWhenRefreshTokenNotFound() {
        when(refreshTokenService.findByTokenHashSecure(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () -> authService.logout("missing-token", "client", "agent", "127.0.0.1"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Refresh Token Not Found");
    }

    @Test
    @DisplayName("logout: should throw when refresh token already revoked")
    void logout_shouldThrowWhenRefreshTokenAlreadyRevoked() {
        RefreshToken token = RefreshTokenProvider.singleTemplate();
        token.setRevoked(true);
        token.setUserAgent("agent");
        token.setClientId("client");
        token.setIpAddress("127.0.0.1");

        when(refreshTokenService.findByTokenHashSecure(anyString())).thenReturn(Optional.of(token));
        doThrow(new UnauthorizedException("Refresh Token already revoked"))
                .when(refreshTokenService)
                .validateRefreshToken(any(), any(), any(Instant.class), any(), any());

        assertThatThrownBy(
                        () -> authService.logout("refresh-token", "client", "agent", "127.0.0.1"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Refresh Token already revoked");
    }

    @Test
    @DisplayName("logout: should revoke token family and clear security context")
    void logout_shouldRevokeFamilyAndClearSecurityContext() {
        RefreshToken token = RefreshTokenProvider.singleTemplate();
        token.setRevoked(false);
        token.setFamilyId(java.util.UUID.randomUUID());

        when(refreshTokenService.findByTokenHashSecure(anyString())).thenReturn(Optional.of(token));

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("user", "password"));

        authService.logout("refresh-token", "client", "agent", "127.0.0.1");

        verify(refreshTokenService)
                .revokeRefreshTokensAndSessionByFamilyId(
                        eq(token.getFamilyId()), any(Instant.class), eq("logout-session"));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // -------------------------------------------------------------------------
    // loadUserByUsername
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("loadUserByUsername: should return user principal")
    void loadUserByUsername_shouldReturnUserPrincipal() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);

        userService.save(user);

        UserDetails result = authService.loadUserByUsername(user.getEmail());

        assertThat(result).isInstanceOf(UserPrincipal.class);
        assertThat(result.getUsername()).isEqualTo(user.getEmail());
    }

    @Test
    @DisplayName("loadUserByUsername: should throw when user does not exist")
    void loadUserByUsername_shouldThrowWhenUserDoesNotExist() {
        assertThatThrownBy(() -> authService.loadUserByUsername("missing@test.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
