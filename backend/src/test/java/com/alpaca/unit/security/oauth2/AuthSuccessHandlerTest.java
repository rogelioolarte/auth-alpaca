package com.alpaca.unit.security.oauth2;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
import com.alpaca.utils.Utils;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.RedirectStrategy;

/** Unit tests for {@link AuthSuccessHandler} */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthSuccessHandler Unit Tests")
class AuthSuccessHandlerTest {

    @Mock private CookieAuthReqRepo repository;

    @Mock private TokenExchangeManager exchangeManager;

    @Mock private UUIDv7Generator uuidGenerator;

    @Mock private HttpServletRequest request;

    @Mock private HttpServletResponse response;

    @Mock private Authentication authentication;

    @Mock private UserPrincipal principal;

    @Mock private RedirectStrategy redirectStrategy;

    private AuthSuccessHandler handler;

    private URI authorizedRedirectUri;

    @BeforeEach
    void setUp() {

        authorizedRedirectUri = URI.create("https://alpaca.com/callback");

        handler =
                new AuthSuccessHandler(
                        repository, List.of(authorizedRedirectUri), exchangeManager, uuidGenerator);

        handler.setRedirectStrategy(redirectStrategy);
        handler.setDefaultTargetUrl("https://alpaca.com/default");
    }

    @Test
    @DisplayName("Should redirect successfully with generated authorization code")
    void onAuthenticationSuccess_ShouldRedirectSuccessfully() throws IOException {
        UUID generatedCode = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        OAuth2AuthorizationRequest authorizationRequest = mock(OAuth2AuthorizationRequest.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(response.isCommitted()).thenReturn(false);
        when(uuidGenerator.generate()).thenReturn(generatedCode);
        when(principal.getUserId()).thenReturn(userId);

        when(request.getHeader("X-Client-Id")).thenReturn("web-client");
        when(request.getHeader("User-Agent")).thenReturn("mozilla");

        when(repository.loadAuthorizationRequest(request)).thenReturn(authorizationRequest);

        when(authorizationRequest.getAttributes())
                .thenReturn(Map.of(AuthSuccessHandler.CLIENT_CODE_CHALLENGE, "challenge-value"));

        try (MockedStatic<CookieManager> cookieManager = mockStatic(CookieManager.class);
                MockedStatic<Utils> utils = mockStatic(Utils.class)) {

            cookieManager
                    .when(
                            () ->
                                    CookieManager.getCookie(
                                            request, AuthSuccessHandler.REDIRECT_PARAM_NAME))
                    .thenReturn(
                            Optional.of(
                                    new Cookie(
                                            AuthSuccessHandler.REDIRECT_PARAM_NAME,
                                            authorizedRedirectUri.toString())));

            utils.when(() -> Utils.extractClientIP(request)).thenReturn("127.0.0.1");

            handler.onAuthenticationSuccess(request, response, authentication);

            ArgumentCaptor<AuthCode> authCodeCaptor = ArgumentCaptor.forClass(AuthCode.class);

            verify(exchangeManager)
                    .createExchangeCode(eq(generatedCode.toString()), authCodeCaptor.capture());

            AuthCode authCode = authCodeCaptor.getValue();

            assertEquals(generatedCode.toString(), authCode.getCode());
            assertEquals("challenge-value", authCode.getCodeChallenge());
            assertEquals("web-client", authCode.getClientId());
            assertEquals("mozilla", authCode.getUserAgent());
            assertEquals("127.0.0.1", authCode.getClientIp());
            assertEquals(userId, authCode.getUserId());
            assertEquals(authorizedRedirectUri.toString(), authCode.getRedirectUri());

            verify(repository).removeAuthorizationRequestCookies(request, response);

            verify(redirectStrategy)
                    .sendRedirect(eq(request), eq(response), contains("code=" + generatedCode));
        }
    }

    @Test
    @DisplayName("Should return immediately when response is already committed")
    void onAuthenticationSuccess_ShouldReturnImmediately_WhenResponseIsCommitted()
            throws IOException {

        when(response.isCommitted()).thenReturn(true);

        handler.onAuthenticationSuccess(request, response, authentication);

        verifyNoInteractions(exchangeManager);
        verifyNoInteractions(repository);
        verifyNoInteractions(redirectStrategy);
    }

    @Test
    @DisplayName("Should throw bad request exception when code challenge is blank")
    void onAuthenticationSuccess_ShouldThrowBadRequestException_WhenCodeChallengeIsBlank()
            throws IOException {

        OAuth2AuthorizationRequest authorizationRequest = mock(OAuth2AuthorizationRequest.class);

        when(response.isCommitted()).thenReturn(false);

        when(repository.loadAuthorizationRequest(request)).thenReturn(authorizationRequest);

        when(authorizationRequest.getAttributes()).thenReturn(Map.of());

        try (MockedStatic<CookieManager> cookieManager = mockStatic(CookieManager.class)) {

            cookieManager
                    .when(
                            () ->
                                    CookieManager.getCookie(
                                            request, AuthSuccessHandler.CLIENT_CODE_CHALLENGE))
                    .thenReturn(Optional.of(new Cookie("cookie", " ")));

            cookieManager
                    .when(
                            () ->
                                    CookieManager.getCookie(
                                            request, AuthSuccessHandler.REDIRECT_PARAM_NAME))
                    .thenReturn(
                            Optional.of(
                                    new Cookie(
                                            AuthSuccessHandler.REDIRECT_PARAM_NAME,
                                            authorizedRedirectUri.toString())));

            BadRequestException exception =
                    assertThrows(
                            BadRequestException.class,
                            () ->
                                    handler.onAuthenticationSuccess(
                                            request, response, authentication));

            assertEquals("Invalid Code Challenge", exception.getReason());

            verifyNoInteractions(exchangeManager);
            verify(redirectStrategy, never()).sendRedirect(any(), any(), anyString());
        }
    }

    @Test
    @DisplayName("Should use code challenge from cookie when attribute is missing")
    void onAuthenticationSuccess_ShouldUseCodeChallengeFromCookie() throws IOException {

        UUID generatedCode = UUID.randomUUID();

        OAuth2AuthorizationRequest authorizationRequest = mock(OAuth2AuthorizationRequest.class);

        when(authentication.getPrincipal()).thenReturn(principal);
        when(response.isCommitted()).thenReturn(false);
        when(uuidGenerator.generate()).thenReturn(generatedCode);

        when(repository.loadAuthorizationRequest(request)).thenReturn(authorizationRequest);

        when(authorizationRequest.getAttributes()).thenReturn(Map.of());

        when(principal.getUserId()).thenReturn(UUID.randomUUID());

        try (MockedStatic<CookieManager> cookieManager = mockStatic(CookieManager.class);
                MockedStatic<Utils> utils = mockStatic(Utils.class)) {

            cookieManager
                    .when(
                            () ->
                                    CookieManager.getCookie(
                                            request, AuthSuccessHandler.CLIENT_CODE_CHALLENGE))
                    .thenReturn(
                            Optional.of(
                                    new Cookie(
                                            AuthSuccessHandler.CLIENT_CODE_CHALLENGE,
                                            "cookie-challenge")));

            cookieManager
                    .when(
                            () ->
                                    CookieManager.getCookie(
                                            request, AuthSuccessHandler.REDIRECT_PARAM_NAME))
                    .thenReturn(
                            Optional.of(
                                    new Cookie(
                                            AuthSuccessHandler.REDIRECT_PARAM_NAME,
                                            authorizedRedirectUri.toString())));

            utils.when(() -> Utils.extractClientIP(request)).thenReturn(null);

            handler.onAuthenticationSuccess(request, response, authentication);

            verify(exchangeManager)
                    .createExchangeCode(
                            eq(generatedCode.toString()),
                            argThat(
                                    authCode ->
                                            authCode.getCodeChallenge()
                                                    .equals("cookie-challenge")));
        }
    }

    @Test
    @DisplayName("Should throw unauthorized exception when principal is null")
    void onAuthenticationSuccess_ShouldThrowUnauthorizedException_WhenPrincipalIsNull() {

        OAuth2AuthorizationRequest authorizationRequest = mock(OAuth2AuthorizationRequest.class);

        when(response.isCommitted()).thenReturn(false);

        when(authentication.getPrincipal()).thenReturn(null);

        when(repository.loadAuthorizationRequest(request)).thenReturn(authorizationRequest);

        when(authorizationRequest.getAttributes())
                .thenReturn(Map.of(AuthSuccessHandler.CLIENT_CODE_CHALLENGE, "challenge"));

        try (MockedStatic<CookieManager> cookieManager = mockStatic(CookieManager.class)) {

            cookieManager
                    .when(
                            () ->
                                    CookieManager.getCookie(
                                            request, AuthSuccessHandler.REDIRECT_PARAM_NAME))
                    .thenReturn(
                            Optional.of(
                                    new Cookie(
                                            AuthSuccessHandler.REDIRECT_PARAM_NAME,
                                            authorizedRedirectUri.toString())));

            UnauthorizedException exception =
                    assertThrows(
                            UnauthorizedException.class,
                            () ->
                                    handler.onAuthenticationSuccess(
                                            request, response, authentication));

            assertEquals("Invalid Credentials", exception.getReason());

            verifyNoInteractions(exchangeManager);
        }
    }

    @Test
    @DisplayName("Should throw unauthorized exception when redirect URI is unauthorized")
    void onAuthenticationSuccess_ShouldThrowUnauthorizedException_WhenRedirectUriIsUnauthorized() {

        when(response.isCommitted()).thenReturn(false);

        try (MockedStatic<CookieManager> cookieManager = mockStatic(CookieManager.class)) {

            cookieManager
                    .when(
                            () ->
                                    CookieManager.getCookie(
                                            request, AuthSuccessHandler.REDIRECT_PARAM_NAME))
                    .thenReturn(
                            Optional.of(
                                    new Cookie(
                                            AuthSuccessHandler.REDIRECT_PARAM_NAME,
                                            "https://malicious.com/callback")));

            UnauthorizedException exception =
                    assertThrows(
                            UnauthorizedException.class,
                            () ->
                                    handler.onAuthenticationSuccess(
                                            request, response, authentication));

            assertEquals("Unauthorized redirect URI", exception.getReason());
        }
    }

    @Test
    @DisplayName("Should throw internal error exception when authorized redirect URIs are empty")
    void onAuthenticationSuccess_ShouldThrowInternalErrorException_WhenAuthorizedUrisAreEmpty() {

        AuthSuccessHandler invalidHandler =
                new AuthSuccessHandler(repository, List.of(), exchangeManager, uuidGenerator);

        when(response.isCommitted()).thenReturn(false);

        InternalErrorException exception =
                assertThrows(
                        InternalErrorException.class,
                        () ->
                                invalidHandler.onAuthenticationSuccess(
                                        request, response, authentication));

        assertEquals("Bad configuration of Authorized Redirect URIs", exception.getReason());
    }

    @Test
    @DisplayName("Should use default target URL when redirect cookie is absent")
    void onAuthenticationSuccess_ShouldUseDefaultTargetUrl_WhenRedirectCookieIsAbsent()
            throws IOException {

        UUID generatedCode = UUID.randomUUID();

        OAuth2AuthorizationRequest authorizationRequest = mock(OAuth2AuthorizationRequest.class);

        when(authentication.getPrincipal()).thenReturn(principal);
        when(response.isCommitted()).thenReturn(false);
        when(uuidGenerator.generate()).thenReturn(generatedCode);

        when(repository.loadAuthorizationRequest(request)).thenReturn(authorizationRequest);

        when(authorizationRequest.getAttributes())
                .thenReturn(Map.of(AuthSuccessHandler.CLIENT_CODE_CHALLENGE, "challenge"));

        when(principal.getUserId()).thenReturn(UUID.randomUUID());

        handler.setDefaultTargetUrl(authorizedRedirectUri.toString());

        try (MockedStatic<CookieManager> cookieManager = mockStatic(CookieManager.class);
                MockedStatic<Utils> utils = mockStatic(Utils.class)) {

            cookieManager
                    .when(
                            () ->
                                    CookieManager.getCookie(
                                            request, AuthSuccessHandler.REDIRECT_PARAM_NAME))
                    .thenReturn(Optional.empty());

            cookieManager
                    .when(
                            () ->
                                    CookieManager.getCookie(
                                            request, AuthSuccessHandler.CLIENT_CODE_CHALLENGE))
                    .thenReturn(Optional.empty());

            utils.when(() -> Utils.extractClientIP(request)).thenReturn("");

            handler.onAuthenticationSuccess(request, response, authentication);

            verify(exchangeManager)
                    .createExchangeCode(
                            eq(generatedCode.toString()),
                            argThat(
                                    authCode ->
                                            authCode.getRedirectUri()
                                                    .equals(authorizedRedirectUri.toString())));

            verify(redirectStrategy)
                    .sendRedirect(eq(request), eq(response), contains("code=" + generatedCode));
        }
    }
}
