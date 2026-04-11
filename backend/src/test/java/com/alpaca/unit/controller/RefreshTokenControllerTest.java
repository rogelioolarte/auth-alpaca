package com.alpaca.unit.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.alpaca.controller.RefreshTokenController;
import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.dto.response.RateLimitResult;
import com.alpaca.exception.RateLimitExceededException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.security.ratelimit.IPRateLimit;
import com.alpaca.service.IRefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

/** Unit tests for {@link RefreshTokenController} */
class RefreshTokenControllerTest {

    private IRefreshTokenService refreshTokenService;
    private IPRateLimit rateLimit;
    private RefreshTokenController controller;

    @BeforeEach
    void setUp() {
        refreshTokenService = mock(IRefreshTokenService.class);
        rateLimit = mock(IPRateLimit.class);
        controller = new RefreshTokenController(refreshTokenService, rateLimit);
    }

    @AfterEach
    void tearDown() {
        Mockito.clearInvocations(refreshTokenService, rateLimit);
    }

    @Test
    void rotateRefreshToken_success_usesRemoteAddressWhenNoXForwardedFor() {
        // Arrange
        String refreshToken = "r.t.k";
        String clientId = "web-client";
        String userAgent = "UA";
        UserPrincipal userPrincipal = new UserPrincipal();
        HttpServletRequest request = mock(HttpServletRequest.class);
        // No X-Forwarded-For header
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.5");

        // rate limit allows
        RateLimitResult allowed = new RateLimitResult(true, 0);
        when(rateLimit.check("10.0.0.5")).thenReturn(allowed);

        AuthResponseDTO expected = new AuthResponseDTO("access-1", "refresh-1");
        when(refreshTokenService.rotateRefreshToken(
                        eq(refreshToken), eq(clientId), eq(userAgent), eq("10.0.0.5")))
                .thenReturn(expected);

        // Act
        ResponseEntity<AuthResponseDTO> resp =
                controller.rotateRefreshToken(
                        refreshToken, clientId, userAgent, userPrincipal, request);

        // Assert
        assertNotNull(resp);
        assertTrue(resp.getStatusCode().is2xxSuccessful());
        assertEquals(expected, resp.getBody());
        // verify rateLimit was checked with extracted IP
        verify(rateLimit).check("10.0.0.5");
        // verify service called with expected args
        verify(refreshTokenService)
                .rotateRefreshToken(eq(refreshToken), eq(clientId), eq(userAgent), eq("10.0.0.5"));
    }

    @Test
    void rotateRefreshToken_success_usesXForwardedForHeaderWhenPresent() {
        // Arrange
        String refreshToken = "rt-xyz";
        String clientId = "mobile-client";
        String userAgent = "MobileUA";
        UserPrincipal userPrincipal = new UserPrincipal();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("5.6.7.8"); // trusted header present
        // remote address should be ignored by Utils.extractClientIP in presence of header
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        RateLimitResult allowed = new RateLimitResult(true, 0);
        when(rateLimit.check("5.6.7.8")).thenReturn(allowed);

        AuthResponseDTO expected = new AuthResponseDTO("a", "r");
        when(refreshTokenService.rotateRefreshToken(
                        eq(refreshToken), eq(clientId), eq(userAgent), eq("5.6.7.8")))
                .thenReturn(expected);

        // Act
        ResponseEntity<AuthResponseDTO> resp =
                controller.rotateRefreshToken(
                        refreshToken, clientId, userAgent, userPrincipal, request);

        // Assert
        assertNotNull(resp);
        assertEquals(expected, resp.getBody());
        verify(rateLimit).check("5.6.7.8");
        verify(refreshTokenService)
                .rotateRefreshToken(eq(refreshToken), eq(clientId), eq(userAgent), eq("5.6.7.8"));
    }

    @Test
    void rotateRefreshToken_rateLimitExceeded_throwsAndDoesNotCallService() {
        // Arrange
        String refreshToken = "rt-deny";
        String clientId = "client-deny";
        String userAgent = "UA-deny";
        UserPrincipal userPrincipal = new UserPrincipal();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.0.2.1");

        RateLimitResult denied = new RateLimitResult(false, 42);
        when(rateLimit.check("192.0.2.1")).thenReturn(denied);

        // Act & Assert
        RateLimitExceededException ex =
                assertThrows(
                        RateLimitExceededException.class,
                        () ->
                                controller.rotateRefreshToken(
                                        refreshToken, clientId, userAgent, userPrincipal, request));
        assertEquals(42, ex.getRetryAfterSeconds());
        // service must NOT be invoked
        verify(refreshTokenService, never())
                .rotateRefreshToken(anyString(), anyString(), anyString(), anyString());
        verify(rateLimit).check("192.0.2.1");
    }

    @Test
    void rotateRefreshToken_capturesArguments_passesIpToService() {
        // Extra test to assert exact captured args passed to service
        String refreshToken = "rt-capture";
        String clientId = "c-capture";
        String userAgent = "UA-capture";
        UserPrincipal userPrincipal = new UserPrincipal();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("203.0.113.9");

        RateLimitResult allowed = new RateLimitResult(true, 0);
        when(rateLimit.check("203.0.113.9")).thenReturn(allowed);

        AuthResponseDTO expected = new AuthResponseDTO("acc", "ref");
        when(refreshTokenService.rotateRefreshToken(
                        anyString(), anyString(), anyString(), anyString()))
                .thenReturn(expected);

        // Act
        ResponseEntity<AuthResponseDTO> resp =
                controller.rotateRefreshToken(
                        refreshToken, clientId, userAgent, userPrincipal, request);

        // Assert
        assertEquals(expected, resp.getBody());
        ArgumentCaptor<String> refreshCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> clientCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> uaCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> ipCaptor = ArgumentCaptor.forClass(String.class);

        verify(refreshTokenService)
                .rotateRefreshToken(
                        refreshCaptor.capture(),
                        clientCaptor.capture(),
                        uaCaptor.capture(),
                        ipCaptor.capture());

        assertEquals(refreshToken, refreshCaptor.getValue());
        assertEquals(clientId, clientCaptor.getValue());
        assertEquals(userAgent, uaCaptor.getValue());
        assertEquals("203.0.113.9", ipCaptor.getValue());
    }
}
