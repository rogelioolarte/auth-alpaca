package com.alpaca.unit.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.alpaca.controller.AuthController;
import com.alpaca.dto.request.AuthLoginRequestDTO;
import com.alpaca.dto.request.AuthRequestDTO;
import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.model.AuthCode;
import com.alpaca.model.UserPrincipal;
import com.alpaca.service.IAuthService;
import com.alpaca.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/** Unit tests for {@link AuthController}. */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private IAuthService authService;

    @Mock private AuthenticationManager authenticationManager;

    @Mock private HttpServletRequest request;

    @Mock private Authentication authentication;

    @Mock private UserPrincipal userPrincipal;

    @InjectMocks private AuthController authController;

    private MockedStatic<Utils> utilsMock;
    private final String clientIp = "192.168.1.1";
    private final String clientId = "client-123";
    private final String userAgent = "Mozilla/5.0";
    private final AuthRequestDTO authRequest = new AuthRequestDTO("test@alpaca.com", "password123");
    private final AuthResponseDTO authResponse =
            new AuthResponseDTO("access-token", "refresh-token");

    @BeforeEach
    void setUp() {
        utilsMock = mockStatic(Utils.class);
        utilsMock
                .when(() -> Utils.extractClientIP(any(HttpServletRequest.class)))
                .thenReturn(clientIp);
    }

    @AfterEach
    void tearDown() {
        utilsMock.close();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("login: Should return OK and token when credentials are valid")
    void login_ShouldReturnOk_WhenAuthenticated() {
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(authService.login(eq(userPrincipal), any(AuthLoginRequestDTO.class)))
                .thenReturn(authResponse);

        ResponseEntity<AuthResponseDTO> response =
                authController.login(authRequest, clientId, userAgent, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(authResponse.accessToken(), response.getBody().accessToken());
        SecurityContext context = SecurityContextHolder.getContext();
        assertEquals(authentication, context.getAuthentication());
        verify(authService)
                .login(
                        eq(userPrincipal),
                        argThat(
                                dto ->
                                        dto.email().equals(authRequest.getEmail())
                                                && dto.clientIp().equals(clientIp)));
    }

    @Test
    @DisplayName("register: Should return OK and token upon successful registration")
    void register_ShouldReturnOk_WhenDataIsValid() {
        when(authService.register(any(AuthLoginRequestDTO.class))).thenReturn(authResponse);

        ResponseEntity<AuthResponseDTO> response =
                authController.register(authRequest, clientId, userAgent, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(authResponse.refreshToken(), response.getBody().refreshToken());
        verify(authService).register(argThat(dto -> dto.email().equals(authRequest.getEmail())));
    }

    @Test
    @DisplayName("logout: Should return success message and call service")
    void logout_ShouldReturnSuccessMessage() {
        String refreshToken = authResponse.refreshToken();

        ResponseEntity<String> response =
                authController.logout(refreshToken, clientId, userAgent, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Logout successful"));
        verify(authService).logout(refreshToken, clientId, userAgent, clientIp);
    }

    @Test
    @DisplayName("exchangeToken: Should return tokens when valid code and verifier provided")
    void exchangeToken_ShouldReturnTokens() {
        Map<String, String> body = new HashMap<>();
        body.put("code", "auth-code");
        body.put("code_verifier", "verifier");
        body.put("client_id", clientId);

        when(authService.login(any(AuthCode.class))).thenReturn(authResponse);

        ResponseEntity<AuthResponseDTO> response =
                authController.exchangeToken(request, userAgent, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(authResponse.accessToken(), response.getBody().accessToken());
        verify(authService)
                .login(
                        argThat(
                                authCode ->
                                        authCode.getCode().equals(body.get("code"))
                                                && authCode.getClientId().equals(clientId)));
    }

    @Test
    @DisplayName("getCurrentUser: Should return user principal when authenticated")
    void getCurrentUser_ShouldReturnUser_WhenAuthenticated() {
        ResponseEntity<UserPrincipal> response = authController.getCurrentUser(userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userPrincipal, response.getBody());
    }

    @Test
    @DisplayName("getCurrentUser: Should return Unauthorized when principal is null")
    void getCurrentUser_ShouldReturnUnauthorized_WhenPrincipalIsNull() {
        ResponseEntity<UserPrincipal> response = authController.getCurrentUser(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    @DisplayName("health: Should return online status message")
    void health_ShouldReturnApiOnline() {
        ResponseEntity<String> response = authController.health();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("API Online", response.getBody());
    }
}
