package com.alpaca.unit.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alpaca.controller.RefreshTokenController;
import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.dto.response.RateLimitResult;
import com.alpaca.exception.RateLimitExceededException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.security.ratelimit.IPRateLimit;
import com.alpaca.service.IRefreshTokenService;
import com.alpaca.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Unit tests for {@link RefreshTokenController} */
@ExtendWith(MockitoExtension.class)
class RefreshTokenControllerTest {

    @Mock private IRefreshTokenService refreshTokenService;

    @Mock private IPRateLimit rateLimit;

    @Mock private HttpServletRequest request;

    @Mock private UserPrincipal userPrincipal;

    @InjectMocks private RefreshTokenController controller;

    private MockedStatic<Utils> utilsMock;
    private final String clientIp = "192.168.1.100";
    private final String refreshToken = "valid-refresh-token";
    private final String clientId = "alpaca-web-client";
    private final String userAgent = "Mozilla/5.0-Test";
    private final AuthResponseDTO authResponse = new AuthResponseDTO("new-access", "new-refresh");

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
    }

    @Test
    @DisplayName("rotateRefreshToken: Should return OK and new tokens when request is valid")
    void rotateRefreshToken_ShouldReturnOk_WhenSuccessful() {
        RateLimitResult allowedResult = new RateLimitResult(true, 0);
        when(rateLimit.check(clientIp)).thenReturn(allowedResult);
        when(refreshTokenService.rotateRefreshToken(refreshToken, clientId, userAgent, clientIp))
                .thenReturn(authResponse);

        ResponseEntity<AuthResponseDTO> response =
                controller.rotateRefreshToken(
                        refreshToken, clientId, userAgent, userPrincipal, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(authResponse.accessToken(), response.getBody().accessToken());
        verify(rateLimit).check(clientIp);
        verify(refreshTokenService).rotateRefreshToken(refreshToken, clientId, userAgent, clientIp);
    }

    @Test
    @DisplayName(
            "rotateRefreshToken: Should throw RateLimitExceededException when rate limit is"
                    + " reached")
    void rotateRefreshToken_ShouldThrowException_WhenRateLimited() {
        RateLimitResult deniedResult = new RateLimitResult(false, 60);
        when(rateLimit.check(clientIp)).thenReturn(deniedResult);

        RateLimitExceededException exception =
                assertThrows(
                        RateLimitExceededException.class,
                        () ->
                                controller.rotateRefreshToken(
                                        refreshToken, clientId, userAgent, userPrincipal, request));

        assertEquals(deniedResult.retryAfterSeconds(), exception.getRetryAfterSeconds());
        verify(rateLimit).check(clientIp);
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    @DisplayName("rotateRefreshToken: Should return Unauthorized when user principal is missing")
    void rotateRefreshToken_ShouldReturnUnauthorized_WhenUserIsNull() {
        RateLimitResult allowedResult = new RateLimitResult(true, 0);
        when(rateLimit.check(clientIp)).thenReturn(allowedResult);

        ResponseEntity<AuthResponseDTO> response =
                controller.rotateRefreshToken(refreshToken, clientId, userAgent, null, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
        verify(rateLimit).check(clientIp);
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    @DisplayName("rotateRefreshToken: Should use extracted IP for rate limit check")
    void rotateRefreshToken_ShouldUseExtractedIp() {
        RateLimitResult allowedResult = new RateLimitResult(true, 0);
        when(rateLimit.check(clientIp)).thenReturn(allowedResult);
        when(refreshTokenService.rotateRefreshToken(any(), any(), any(), any()))
                .thenReturn(authResponse);

        controller.rotateRefreshToken(refreshToken, clientId, userAgent, userPrincipal, request);

        utilsMock.verify(() -> Utils.extractClientIP(request));
        verify(rateLimit).check(clientIp);
    }
}
