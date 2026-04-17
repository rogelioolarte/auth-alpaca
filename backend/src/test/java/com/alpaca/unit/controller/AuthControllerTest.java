package com.alpaca.unit.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.alpaca.controller.AuthController;
import com.alpaca.dto.request.AuthLoginRequestDTO;
import com.alpaca.dto.request.AuthRequestDTO;
import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.model.UserPrincipal;
import com.alpaca.security.manager.TokenExchangeManager;
import com.alpaca.service.IAuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Unit tests for {@link AuthController}. */
class AuthControllerTest {

    private IAuthService authService;
    private AuthenticationManager authenticationManager;
    private TokenExchangeManager tokenExchangeManager;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        authService = mock(IAuthService.class);
        authenticationManager = mock(AuthenticationManager.class);
        tokenExchangeManager = mock(TokenExchangeManager.class);
        controller = new AuthController(authService, authenticationManager, tokenExchangeManager);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        Mockito.clearInvocations(authService, authenticationManager);
    }

    @Test
    void login_success_authenticates_setsSecurityContext_and_callsAuthService() {
        // Arrange
        AuthRequestDTO requestDTO = new AuthRequestDTO("alice@example.com", "s3cret");
        String clientId = "web-client";
        String userAgent = "Mozilla/5.0";
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(servletRequest.getRemoteAddr()).thenReturn("10.0.0.5");

        // Authentication returned by manager
        UserPrincipal principal = mock(UserPrincipal.class);
        Authentication authResult =
                new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authResult);

        AuthResponseDTO expectedDto = new AuthResponseDTO("access-token", "refresh-token");
        // capture second param passed to authService.login
        when(authService.login(eq(principal), any(AuthLoginRequestDTO.class)))
                .thenReturn(expectedDto);

        // Act
        ResponseEntity<AuthResponseDTO> resp =
                controller.login(requestDTO, clientId, userAgent, servletRequest);

        // Assert response
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(expectedDto.accessToken(), resp.getBody().accessToken());
        // SecurityContext has been set to the Authentication returned by manager
        assertSame(authResult, SecurityContextHolder.getContext().getAuthentication());

        // Verify authenticate was called with a UsernamePasswordAuthenticationToken containing the
        // email/password
        ArgumentCaptor<Authentication> authCaptor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(authCaptor.capture());
        Authentication sent = authCaptor.getValue();
        assertInstanceOf(UsernamePasswordAuthenticationToken.class, sent);
        assertEquals(requestDTO.getEmail(), sent.getPrincipal());
        assertEquals(requestDTO.getPassword(), sent.getCredentials());

        // Verify authService.login was called with the principal and an AuthLoginRequestDTO that
        // contains clientId, userAgent and IP
        ArgumentCaptor<AuthLoginRequestDTO> loginDtoCaptor =
                ArgumentCaptor.forClass(AuthLoginRequestDTO.class);
        verify(authService).login(eq(principal), loginDtoCaptor.capture());
        AuthLoginRequestDTO usedDto = loginDtoCaptor.getValue();
        assertEquals(clientId, usedDto.clientId());
        assertEquals(userAgent, usedDto.userAgent());
        assertEquals("10.0.0.5", usedDto.clientIp());
    }

    @Test
    void login_whenAuthenticationManagerThrows_exceptionPropagates() {
        // Arrange
        AuthRequestDTO requestDTO = new AuthRequestDTO("bob@example.com", "password");
        String clientId = "cid";
        String userAgent = "ua";
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new IllegalArgumentException("bad credentials"));

        // Act & Assert
        IllegalArgumentException ex =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> controller.login(requestDTO, clientId, userAgent, servletRequest));
        assertTrue(ex.getMessage().contains("bad credentials"));

        // SecurityContext should remain empty after failure
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(authService, never()).login(any(), any());
    }

    @Test
    void register_callsAuthServiceAndReturnsToken() {
        // Arrange
        AuthRequestDTO requestDTO = new AuthRequestDTO("carla@example.com", "strongpass");
        String clientId = "c";
        String userAgent = "ua";
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getHeader("X-Forwarded-For")).thenReturn("8.8.8.8");

        AuthResponseDTO expected = new AuthResponseDTO("access-register", "refresh-register");
        when(authService.register(any(AuthLoginRequestDTO.class))).thenReturn(expected);

        // Act
        ResponseEntity<AuthResponseDTO> resp =
                controller.register(requestDTO, clientId, userAgent, servletRequest);

        // Assert
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(expected, resp.getBody());

        // verify authService.register called with AuthLoginRequestDTO containing expected values
        ArgumentCaptor<AuthLoginRequestDTO> captor =
                ArgumentCaptor.forClass(AuthLoginRequestDTO.class);
        verify(authService).register(captor.capture());
        AuthLoginRequestDTO dto = captor.getValue();
        assertEquals(requestDTO.getEmail(), dto.email());
        assertEquals(requestDTO.getPassword(), dto.password());
        assertEquals(clientId, dto.clientId());
        assertEquals(userAgent, dto.userAgent());
        // IP extracted via Utils.extractClientIP(request) — ensure Utils can read provided header;
        // in our test header was X-Forwarded-For
        assertEquals("8.8.8.8", dto.clientIp());
    }

    @Test
    void logout_invokesServiceAndReturnsSuccessMessage() {
        // Arrange
        String refreshToken = "r.t.o.k";
        String clientId = "client-x";
        String userAgent = "agent";
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(servletRequest.getRemoteAddr()).thenReturn("172.16.0.1");

        // Act
        ResponseEntity<String> resp =
                controller.logout(refreshToken, clientId, userAgent, servletRequest);

        // Assert
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().contains("Logout successful"));

        verify(authService).logout(eq(refreshToken), eq(clientId), eq(userAgent), eq("172.16.0.1"));
    }

    @Test
    void getCurrentUser_returnsUnauthorized_whenPrincipalIsNull() {
        ResponseEntity<?> resp = controller.getCurrentUser(null);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertNull(resp.getBody());
    }

    @Test
    void getCurrentUser_returnsOk_whenPrincipalProvided() {
        UserPrincipal up = mock(UserPrincipal.class);
        when(up.getUsername()).thenReturn("u");
        ResponseEntity<UserPrincipal> resp = controller.getCurrentUser(up);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertSame(up, resp.getBody());
    }

    @Test
    void health_returnsApiOnline() {
        ResponseEntity<String> resp = controller.health();
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("API Online", resp.getBody());
    }
}
