package com.alpaca.unit.security.oauth2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.InternalErrorException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.AuthCode;
import com.alpaca.model.UserPrincipal;
import com.alpaca.security.manager.CookieManager;
import com.alpaca.security.manager.TokenExchangeManager;
import com.alpaca.security.oauth2.AuthSuccessHandler;
import com.alpaca.security.oauth2.CookieAuthReqRepo;
import com.alpaca.utils.UUIDv7Generator;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.RedirectStrategy;

@DisplayName("AuthSuccessHandler Unit Tests")
class AuthSuccessHandlerTest {

    private AuthSuccessHandler handler;
    private CookieAuthReqRepo repository;
    private TokenExchangeManager exchangeManager;
    private UUIDv7Generator uuidGenerator;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private Authentication authentication;
    private UserPrincipal principal;
    private RedirectStrategy redirectStrategy;

    private final URI authorizedUri = URI.create("https://alpaca.com/callback");

    @BeforeEach
    void setUp() {
        repository = mock(CookieAuthReqRepo.class);
        exchangeManager = mock(TokenExchangeManager.class);
        uuidGenerator = mock(UUIDv7Generator.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        authentication = mock(Authentication.class);
        principal = mock(UserPrincipal.class);
        redirectStrategy = mock(RedirectStrategy.class);

        handler =
                new AuthSuccessHandler(
                        repository, List.of(authorizedUri), exchangeManager, uuidGenerator);
        String defaultTarget = "https://alpaca.com/dashboard";
        handler.setDefaultTargetUrl(defaultTarget);
        handler.setRedirectStrategy(redirectStrategy);

        when(authentication.getPrincipal()).thenReturn(principal);
    }

    @Test
    @DisplayName("onAuthenticationSuccess: Should redirect with code when all conditions are met")
    void onAuthenticationSuccess_ShouldRedirectWithCode() throws IOException {
        UUID generatedUuid = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String challenge = "pkce-challenge-123";
        String clientId = "client-web";

        when(response.isCommitted()).thenReturn(false);
        when(uuidGenerator.generate()).thenReturn(generatedUuid);
        when(principal.getUserId()).thenReturn(userId);
        when(request.getHeader("X-Client-Id")).thenReturn(clientId);

        OAuth2AuthorizationRequest authReq = mock(OAuth2AuthorizationRequest.class);
        when(authReq.getAttributes()).thenReturn(Map.of("client_code_challenge", challenge));
        when(repository.loadAuthorizationRequest(request)).thenReturn(authReq);

        try (MockedStatic<CookieManager> cookieManager = mockStatic(CookieManager.class)) {
            cookieManager
                    .when(() -> CookieManager.getCookie(request, "redirect_uri"))
                    .thenReturn(Optional.of(new Cookie("redirect_uri", authorizedUri.toString())));

            handler.onAuthenticationSuccess(request, response, authentication);

            ArgumentCaptor<AuthCode> authCodeCaptor = ArgumentCaptor.forClass(AuthCode.class);
            verify(exchangeManager)
                    .createExchangeCode(eq(generatedUuid.toString()), authCodeCaptor.capture());

            AuthCode captured = authCodeCaptor.getValue();
            assertEquals(challenge, captured.getCodeChallenge());
            assertEquals(userId, captured.getUserId());

            verify(redirectStrategy)
                    .sendRedirect(eq(request), eq(response), contains("code=" + generatedUuid));
        }
    }

    @Test
    @DisplayName(
            "onAuthenticationSuccess: Should throw BadRequestException if code challenge is"
                    + " missing")
    void onAuthenticationSuccess_ShouldThrow_WhenChallengeMissing() {
        when(response.isCommitted()).thenReturn(false);
        OAuth2AuthorizationRequest authReq = mock(OAuth2AuthorizationRequest.class);
        when(authReq.getAttributes()).thenReturn(Map.of());
        when(repository.loadAuthorizationRequest(request)).thenReturn(authReq);

        try (MockedStatic<CookieManager> cookieManager = mockStatic(CookieManager.class)) {
            cookieManager
                    .when(() -> CookieManager.getCookie(any(), eq("client_code_challenge")))
                    .thenReturn(Optional.empty());
            cookieManager
                    .when(() -> CookieManager.getCookie(any(), eq("redirect_uri")))
                    .thenReturn(Optional.empty());

            assertThrows(
                    BadRequestException.class,
                    () -> handler.onAuthenticationSuccess(request, response, authentication));
        }
    }

    @Test
    @DisplayName("onAuthenticationSuccess: Should exit early if response is already committed")
    void onAuthenticationSuccess_ShouldExit_WhenCommitted() throws IOException {
        when(response.isCommitted()).thenReturn(true);

        handler.onAuthenticationSuccess(request, response, authentication);

        verifyNoInteractions(exchangeManager, redirectStrategy);
    }

    @Test
    @DisplayName(
            "determineTargetUrl: Should throw InternalErrorException if no URIs are configured")
    void determineTargetUrl_ShouldThrow_WhenNoUrisConfigured() {
        AuthSuccessHandler unconfiguredHandler =
                new AuthSuccessHandler(repository, List.of(), exchangeManager, uuidGenerator);

        assertThrows(
                InternalErrorException.class,
                () ->
                        unconfiguredHandler.onAuthenticationSuccess(
                                request, response, authentication));
    }

    @Test
    @DisplayName(
            "determineTargetUrl: Should throw UnauthorizedException if URI host is not whitelisted")
    void determineTargetUrl_ShouldThrow_WhenUriUnauthorized() {
        try (MockedStatic<CookieManager> cookieManager = mockStatic(CookieManager.class)) {
            cookieManager
                    .when(() -> CookieManager.getCookie(request, "redirect_uri"))
                    .thenReturn(Optional.of(new Cookie("redirect_uri", "https://malicious.com")));

            assertThrows(
                    UnauthorizedException.class,
                    () -> handler.onAuthenticationSuccess(request, response, authentication));
        }
    }

    @Test
    @DisplayName("determineCodeChallenge: Should fallback to cookie if attribute is missing")
    void determineCodeChallenge_ShouldFallbackToCookie() throws IOException {
        String cookieChallenge = "fallback-challenge";
        OAuth2AuthorizationRequest authReq = mock(OAuth2AuthorizationRequest.class);
        when(authReq.getAttributes()).thenReturn(Map.of());
        when(repository.loadAuthorizationRequest(request)).thenReturn(authReq);
        when(uuidGenerator.generate()).thenReturn(UUID.randomUUID());

        try (MockedStatic<CookieManager> cookieManager = mockStatic(CookieManager.class)) {
            cookieManager
                    .when(() -> CookieManager.getCookie(request, "client_code_challenge"))
                    .thenReturn(Optional.of(new Cookie("client_code_challenge", cookieChallenge)));
            cookieManager
                    .when(() -> CookieManager.getCookie(request, "redirect_uri"))
                    .thenReturn(Optional.empty());

            handler.onAuthenticationSuccess(request, response, authentication);

            verify(exchangeManager)
                    .createExchangeCode(
                            anyString(),
                            argThat(ac -> ac.getCodeChallenge().equals(cookieChallenge)));
        }
    }
}
