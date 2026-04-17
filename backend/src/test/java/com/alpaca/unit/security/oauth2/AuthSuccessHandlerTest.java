package com.alpaca.unit.security.oauth2;

import static com.alpaca.security.oauth2.CookieAuthReqRepo.REDIRECT_COOKIE_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alpaca.dto.request.AuthLoginRequestDTO;
import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.exception.InternalErrorException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.security.manager.CookieManager;
import com.alpaca.security.manager.TokenExchangeManager;
import com.alpaca.security.oauth2.AuthSuccessHandler;
import com.alpaca.security.oauth2.CookieAuthReqRepo;
import com.alpaca.service.IAuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;

@DisplayName("AuthSuccessHandler Unit Tests")
class AuthSuccessHandlerTest {

    private static class TestableHandler extends AuthSuccessHandler {

        public TestableHandler(
                CookieAuthReqRepo repo,
                List<URI> uris,
                IAuthService svc,
                TokenExchangeManager exm) {
            super(repo, uris, "dev", svc, exm);
        }

        public String invokeDetermineTarget(
                HttpServletRequest r, HttpServletResponse s, Authentication a) {
            return super.determineTargetUrl(r, s, a);
        }
    }

    private CookieAuthReqRepo repository;
    private IAuthService authService;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private Authentication authentication;
    private TokenExchangeManager tokenExchangeManager;
    private UserPrincipal principal;
    private static final String DEFAULT_TARGET = "http://localhost/app";

    @BeforeEach
    void setUp() {
        repository = mock(CookieAuthReqRepo.class);
        authService = mock(IAuthService.class);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        authentication = mock(Authentication.class);
        tokenExchangeManager = mock(TokenExchangeManager.class);

        principal = mock(UserPrincipal.class);
        when(authentication.getPrincipal()).thenReturn(principal);
    }

    @Nested
    @DisplayName("determineTargetUrl() tests")
    class DetermineTargetUrl {

        @Test
        @DisplayName("throws InternalErrorException when no authorizedRedirectUris configured")
        void noAuthorizedRedirectsConfigured() {
            TestableHandler handler =
                    new TestableHandler(repository, List.of(), authService, tokenExchangeManager);
            handler.setDefaultTargetUrl(DEFAULT_TARGET);

            try (MockedStatic<CookieManager> cm = mockStatic(CookieManager.class)) {
                cm.when(() -> CookieManager.getCookie(request, REDIRECT_COOKIE_NAME))
                        .thenReturn(Optional.empty());

                assertThrows(
                        InternalErrorException.class,
                        () -> handler.invokeDetermineTarget(request, response, authentication));
            }
        }

        @Test
        @DisplayName("throws UnauthorizedException when redirect cookie host is not authorized")
        void unauthorizedRedirectCookie() {
            TestableHandler handler =
                    new TestableHandler(
                            repository,
                            List.of(URI.create("https://allowed.com")),
                            authService,
                            tokenExchangeManager);

            Cookie cookie = new Cookie(REDIRECT_COOKIE_NAME, "https://notallowed.com/path");

            try (MockedStatic<com.alpaca.security.manager.CookieManager> cm =
                    mockStatic(com.alpaca.security.manager.CookieManager.class)) {
                cm.when(
                                () ->
                                        com.alpaca.security.manager.CookieManager.getCookie(
                                                request, REDIRECT_COOKIE_NAME))
                        .thenReturn(Optional.of(cookie));

                assertThrows(
                        UnauthorizedException.class,
                        () -> handler.invokeDetermineTarget(request, response, authentication));
            }
        }

        @Test
        @DisplayName("returns default target when no cookie and host authorized")
        void defaultTargetWhenHostAuthorized() {
            TestableHandler handler =
                    new TestableHandler(
                            repository,
                            List.of(URI.create("http://localhost")),
                            authService,
                            tokenExchangeManager);

            handler.setDefaultTargetUrl(DEFAULT_TARGET);

            try (MockedStatic<com.alpaca.security.manager.CookieManager> cm =
                    mockStatic(com.alpaca.security.manager.CookieManager.class)) {

                cm.when(
                                () ->
                                        com.alpaca.security.manager.CookieManager.getCookie(
                                                request, REDIRECT_COOKIE_NAME))
                        .thenReturn(Optional.empty());

                String target = handler.invokeDetermineTarget(request, response, authentication);

                assertEquals(DEFAULT_TARGET, target);
            }
        }

        @Test
        @DisplayName("returns redirect cookie when authorized")
        void redirectCookieHostAuthorized() {
            String redirectUrl = "http://localhost/path";
            TestableHandler handler =
                    new TestableHandler(
                            repository,
                            List.of(URI.create("http://localhost")),
                            authService,
                            tokenExchangeManager);

            try (MockedStatic<com.alpaca.security.manager.CookieManager> cm =
                    mockStatic(com.alpaca.security.manager.CookieManager.class)) {

                cm.when(
                                () ->
                                        com.alpaca.security.manager.CookieManager.getCookie(
                                                request, REDIRECT_COOKIE_NAME))
                        .thenReturn(Optional.of(new Cookie(REDIRECT_COOKIE_NAME, redirectUrl)));

                String target = handler.invokeDetermineTarget(request, response, authentication);

                assertEquals(redirectUrl, target);
            }
        }
    }

    @Nested
    @DisplayName("onAuthenticationSuccess() tests")
    class OnAuthenticationSuccess {

        @Test
        @DisplayName("writes JSON body with tokens and target URL when success")
        void writesJsonBody() throws IOException {
            String redirectUrl = "http://localhost/app";
            AuthSuccessHandler handler =
                    new AuthSuccessHandler(
                            repository,
                            List.of(URI.create("http://localhost")),
                            "dev",
                            authService,
                            tokenExchangeManager);

            handler.setDefaultTargetUrl(redirectUrl);

            AuthResponseDTO authResp = new AuthResponseDTO("abc", "def");
            when(authService.login(eq(principal), any(AuthLoginRequestDTO.class)))
                    .thenReturn(authResp);

            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            handler.onAuthenticationSuccess(request, response, authentication);

            String json = response.getContentAsString();
            assertTrue(json.contains("\"accessToken\":\"abc\""));
            assertTrue(json.contains("\"refreshToken\":\"def\""));
            assertTrue(json.contains("\"targetUrl\":\"" + redirectUrl + "\""));
        }

        @Test
        @DisplayName("does nothing if response already committed")
        void doesNothingIfCommitted() throws IOException {
            AuthSuccessHandler handler =
                    new AuthSuccessHandler(
                            repository,
                            List.of(URI.create("http://localhost")),
                            "dev",
                            authService,
                            tokenExchangeManager);

            MockHttpServletResponse committed =
                    new MockHttpServletResponse() {
                        @Override
                        public boolean isCommitted() {
                            return true;
                        }
                    };

            handler.onAuthenticationSuccess(request, committed, authentication);

            assertEquals(0, committed.getContentAsString().length());
        }

        @Test
        @DisplayName("clearAuthenticationAttributes is invoked")
        void clearAuthAttributesInvoked() throws IOException {
            AuthSuccessHandler handler =
                    new AuthSuccessHandler(
                            repository,
                            List.of(URI.create("http://localhost")),
                            "dev",
                            authService,
                            tokenExchangeManager);
            handler.setDefaultTargetUrl("http://localhost/home");
            AuthResponseDTO authResp = new AuthResponseDTO("t1", "t2");
            when(authService.login(eq(principal), any(AuthLoginRequestDTO.class)))
                    .thenReturn(authResp);

            handler.onAuthenticationSuccess(request, response, authentication);
            verify(repository).removeAuthorizationRequestCookies(request, response);
        }
    }
}
