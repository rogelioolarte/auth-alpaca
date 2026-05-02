package com.alpaca.unit.security.oauth2;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alpaca.dto.request.AuthLoginRequestDTO;
import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.exception.InternalErrorException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.resources.UserProvider;
import com.alpaca.security.manager.CookieManager;
import com.alpaca.security.manager.TokenExchangeManager;
import com.alpaca.security.oauth2.AuthSuccessHandler;
import com.alpaca.security.oauth2.CookieAuthReqRepo;
import com.alpaca.service.IAuthService;
import com.alpaca.utils.UUIDv7Generator;
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
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@DisplayName("AuthSuccessHandler Unit Tests")
class AuthSuccessHandlerTest {

    private static class TestableHandler extends AuthSuccessHandler {

        public TestableHandler(
                CookieAuthReqRepo repo,
                List<URI> uris,
                TokenExchangeManager exm,
                UUIDv7Generator uug) {
            super(repo, uris, exm, uug);
        }

        public String invokeDetermineTarget(
                HttpServletRequest r, HttpServletResponse s, Authentication a) {
            return super.determineTargetUrl(r, s, a);
        }
    }

    @Mock private CookieAuthReqRepo repository;
    @Mock private IAuthService authService;
    @Mock private MockHttpServletRequest request;
    @Mock private MockHttpServletResponse response;
    private Authentication authentication;
    @Mock private TokenExchangeManager tokenExchangeManager;
    @Mock private UUIDv7Generator uuidGenerator;

    private TestableHandler handler;

    private static final String DEFAULT_TARGET = "http://localhost/app";
    public static final String REDIRECT_PARAM_NAME = "redirect_uri";
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        handler =
                new AuthSuccessHandlerTest.TestableHandler(
                        repository, List.of(), tokenExchangeManager, uuidGenerator);
        principal = new UserPrincipal(UserProvider.singleEntity());
        authentication = new UsernamePasswordAuthenticationToken(principal, null);
    }

    @Nested
    @DisplayName("determineTargetUrl() tests")
    class DetermineTargetUrl {

        @Test
        @DisplayName("throws InternalErrorException when no authorizedRedirectUris configured")
        void noAuthorizedRedirectsConfigured() {
            handler.setDefaultTargetUrl(DEFAULT_TARGET);

            try (MockedStatic<CookieManager> cm = mockStatic(CookieManager.class)) {
                cm.when(() -> CookieManager.getCookie(request, REDIRECT_PARAM_NAME))
                        .thenReturn(Optional.empty());

                assertThrows(
                        InternalErrorException.class,
                        () -> handler.invokeDetermineTarget(request, response, authentication));
            }
        }

        @Test
        @DisplayName("throws UnauthorizedException when redirect cookie host is not authorized")
        void unauthorizedRedirectCookie() {
            Cookie cookie = new Cookie(REDIRECT_PARAM_NAME, "https://notallowed.com/path");

            try (MockedStatic<com.alpaca.security.manager.CookieManager> cm =
                    mockStatic(com.alpaca.security.manager.CookieManager.class)) {
                cm.when(
                                () ->
                                        com.alpaca.security.manager.CookieManager.getCookie(
                                                request, REDIRECT_PARAM_NAME))
                        .thenReturn(Optional.of(cookie));

                assertThrows(
                        UnauthorizedException.class,
                        () -> handler.invokeDetermineTarget(request, response, authentication));
            }
        }

        @Test
        @DisplayName("returns default target when no cookie and host authorized")
        void defaultTargetWhenHostAuthorized() {
            handler.setDefaultTargetUrl(DEFAULT_TARGET);

            try (MockedStatic<com.alpaca.security.manager.CookieManager> cm =
                    mockStatic(com.alpaca.security.manager.CookieManager.class)) {

                cm.when(
                                () ->
                                        com.alpaca.security.manager.CookieManager.getCookie(
                                                request, REDIRECT_PARAM_NAME))
                        .thenReturn(Optional.empty());

                String target = handler.invokeDetermineTarget(request, response, authentication);

                assertEquals(DEFAULT_TARGET, target);
            }
        }

        @Test
        @DisplayName("returns redirect cookie when authorized")
        void redirectCookieHostAuthorized() {
            String redirectUrl = "http://localhost/path";

            try (MockedStatic<com.alpaca.security.manager.CookieManager> cm =
                    mockStatic(com.alpaca.security.manager.CookieManager.class)) {

                cm.when(
                                () ->
                                        com.alpaca.security.manager.CookieManager.getCookie(
                                                request, REDIRECT_PARAM_NAME))
                        .thenReturn(Optional.of(new Cookie(REDIRECT_PARAM_NAME, redirectUrl)));

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

            handler.setDefaultTargetUrl(redirectUrl);

            AuthResponseDTO authResp = new AuthResponseDTO("abc", "def");
            when(authService.login(eq(principal), any(AuthLoginRequestDTO.class)))
                    .thenReturn(authResp);
            String code = "TEST-CODE";
            when(uuidGenerator.generate().toString()).thenReturn(code);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            handler.onAuthenticationSuccess(request, response, authentication);

            String json = response.getRedirectedUrl();
            assertTrue(json.contains("\"code\":\"TEST-CODE\""));
            assertTrue(json.contains("\"targetUrl\":\"" + redirectUrl + "\""));
        }

        @Test
        @DisplayName("does nothing if response already committed")
        void doesNothingIfCommitted() throws IOException {
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
            handler.setDefaultTargetUrl("http://localhost/home");
            String code = "test-code";
            when(uuidGenerator.generate().toString()).thenReturn(code);
            when(request.getHeader("X-Client-Id")).thenReturn("abc123");
            when(request.getHeader("User-Agent")).thenReturn("abc123");
            when(request.getHeader("X-Forwarded-For")).thenReturn("127.0.0.1");

            handler.onAuthenticationSuccess(request, response, authentication);
            verify(repository).removeAuthorizationRequestCookies(request, response);
        }
    }
}
