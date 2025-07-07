package com.alpaca.unit.security.oauth2;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.security.manager.CookieManager;
import com.alpaca.security.manager.JJwtManager;
import com.alpaca.security.oauth2.AuthSuccessHandler;
import com.alpaca.security.oauth2.CookieAuthReqRepo;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.RedirectStrategy;

@DisplayName("AuthSuccessHandler Unit Tests")
class AuthSuccessHandlerTest {

    // Subclass to expose protected methods for testing
    private static class TestableHandler extends AuthSuccessHandler {
        TestableHandler(
                JJwtManager jwtManager, CookieAuthReqRepo repository, List<URI> redirectUris) {
            super(jwtManager, repository, redirectUris);
        }

        public String invokeDetermineTarget(
                HttpServletRequest req, HttpServletResponse res, Authentication auth) {
            return super.determineTargetUrl(req, res, auth);
        }

        public void invokeClearAttributes(HttpServletRequest req, HttpServletResponse res) {
            super.clearAuthenticationAttributes(req, res);
        }
    }

    private JJwtManager jwtManager;
    private CookieAuthReqRepo repository;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private Authentication authentication;
    private UserPrincipal principal;
    private static final String DEFAULT_TARGET = "/";

    @BeforeEach
    void setUp() {
        jwtManager = mock(JJwtManager.class);
        repository = mock(CookieAuthReqRepo.class);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        authentication = mock(Authentication.class);
        principal = mock(UserPrincipal.class);
        when(authentication.getPrincipal()).thenReturn(principal);
    }

    @Nested
    @DisplayName("determineTargetUrl() tests")
    class DetermineTargetUrlTests {

        @Test
        @DisplayName(
                "Throws UnauthorizedException if no redirect cookie and default URI unauthorized")
        void noCookie_defaultUriUnauthorized() {
            TestableHandler handler =
                    new TestableHandler(
                            jwtManager, repository, List.of(URI.create("http://example.com")));
            when(jwtManager.createToken(principal)).thenReturn("tok");
            try (MockedStatic<CookieManager> cm = mockStatic(CookieManager.class)) {
                cm.when(
                                () ->
                                        CookieManager.getCookie(
                                                request, CookieAuthReqRepo.RedirectCookieName))
                        .thenReturn(Optional.empty());
                assertThrows(
                        UnauthorizedException.class,
                        () -> handler.invokeDetermineTarget(request, response, authentication));
            }
        }

        @Test
        @DisplayName("Uses default target URL when no redirect cookie and no restrictions")
        void noCookie_authorizedWhenNoRestrictions() {
            // authorizedRedirectUris empty => skip check
            TestableHandler handler =
                    new TestableHandler(jwtManager, repository, Collections.emptyList());
            handler.setDefaultTargetUrl(DEFAULT_TARGET);
            when(jwtManager.createToken(principal)).thenReturn("tok");
            try (MockedStatic<CookieManager> cm = mockStatic(CookieManager.class)) {
                cm.when(
                                () ->
                                        CookieManager.getCookie(
                                                request, CookieAuthReqRepo.RedirectCookieName))
                        .thenReturn(Optional.empty());
                String target = handler.invokeDetermineTarget(request, response, authentication);
                assertEquals(DEFAULT_TARGET + "?token=tok", target);
            }
        }

        @Test
        @DisplayName("Valid redirect cookie and authorized URI appends token")
        void validCookie_authorizedUri() {
            TestableHandler handler =
                    new TestableHandler(
                            jwtManager, repository, List.of(URI.create("http://localhost")));
            String cookieUrl = "http://localhost/path";
            when(jwtManager.createToken(principal)).thenReturn("tok2");
            Cookie cookie = new Cookie(CookieAuthReqRepo.RedirectCookieName, cookieUrl);
            try (MockedStatic<CookieManager> cm = mockStatic(CookieManager.class)) {
                cm.when(
                                () ->
                                        CookieManager.getCookie(
                                                request, CookieAuthReqRepo.RedirectCookieName))
                        .thenReturn(Optional.of(cookie));
                String target = handler.invokeDetermineTarget(request, response, authentication);
                assertTrue(target.startsWith(cookieUrl + "?token="));
            }
        }

        @Test
        @DisplayName("Throws UnauthorizedException for invalid redirect URI")
        void invalidCookie_throws() {
            TestableHandler handler =
                    new TestableHandler(jwtManager, repository, List.of(URI.create("http://foo")));
            Cookie cookie = new Cookie(CookieAuthReqRepo.RedirectCookieName, "http://bar");
            when(jwtManager.createToken(principal)).thenReturn("tok3");
            try (MockedStatic<CookieManager> cm = mockStatic(CookieManager.class)) {
                cm.when(
                                () ->
                                        CookieManager.getCookie(
                                                request, CookieAuthReqRepo.RedirectCookieName))
                        .thenReturn(Optional.of(cookie));
                assertThrows(
                        UnauthorizedException.class,
                        () -> handler.invokeDetermineTarget(request, response, authentication));
            }
        }
    }

    @Nested
    @DisplayName("clearAuthenticationAttributes() tests")
    class ClearAuthAttributesTests {
        @Test
        @DisplayName("Clears super attributes and removes cookies via repository")
        void shouldClearSuperAndRemoveCookies() {
            TestableHandler handler = new TestableHandler(jwtManager, repository, List.of());
            TestableHandler spyHandler = spy(handler);
            spyHandler.invokeClearAttributes(request, response);
            verify(spyHandler).invokeClearAttributes(request, response);
            verify(repository).removeAuthorizationRequestCookies(request, response);
        }
    }

    @Nested
    @DisplayName("onAuthenticationSuccess() tests")
    class OnAuthSuccessTests {
        @Test
        @DisplayName("Redirects when not committed and authorized")
        void redirectWhenNotCommitted() throws IOException {
            TestableHandler handler =
                    new TestableHandler(
                            jwtManager, repository, List.of(URI.create("http://localhost")));
            RedirectStrategy strategy = mock(RedirectStrategy.class);
            handler.setRedirectStrategy(strategy);
            handler.setDefaultTargetUrl(DEFAULT_TARGET);
            String cookieUrl = "http://localhost/ok";
            when(jwtManager.createToken(principal)).thenReturn("abc");
            try (MockedStatic<CookieManager> cm = mockStatic(CookieManager.class)) {
                cm.when(
                                () ->
                                        CookieManager.getCookie(
                                                request, CookieAuthReqRepo.RedirectCookieName))
                        .thenReturn(
                                Optional.of(
                                        new Cookie(
                                                CookieAuthReqRepo.RedirectCookieName, cookieUrl)));
                handler.onAuthenticationSuccess(request, response, authentication);
                verify(strategy).sendRedirect(request, response, cookieUrl + "?token=abc");
            }
        }

        @Test
        @DisplayName("Does nothing when response is committed")
        void noRedirectWhenCommitted() throws IOException {
            TestableHandler handler = new TestableHandler(jwtManager, repository, List.of());
            RedirectStrategy strategy = mock(RedirectStrategy.class);
            handler.setRedirectStrategy(strategy);
            HttpServletResponse committed =
                    new MockHttpServletResponse() {
                        @Override
                        public boolean isCommitted() {
                            return true;
                        }
                    };
            handler.onAuthenticationSuccess(request, committed, authentication);
            verify(strategy, never()).sendRedirect(any(), any(), anyString());
        }

        @Test
        @DisplayName("Throws UnauthorizedException when target unauthorized")
        void exceptionWhenUnauthorized() throws IOException {
            TestableHandler handler =
                    new TestableHandler(jwtManager, repository, List.of(URI.create("http://foo")));
            when(jwtManager.createToken(principal)).thenReturn("t");
            RedirectStrategy strategy = mock(RedirectStrategy.class);
            handler.setRedirectStrategy(strategy);
            try (MockedStatic<CookieManager> cm = mockStatic(CookieManager.class)) {
                cm.when(
                                () ->
                                        CookieManager.getCookie(
                                                request, CookieAuthReqRepo.RedirectCookieName))
                        .thenReturn(
                                Optional.of(
                                        new Cookie(
                                                CookieAuthReqRepo.RedirectCookieName,
                                                "http://bar")));
                assertThrows(
                        UnauthorizedException.class,
                        () -> handler.onAuthenticationSuccess(request, response, authentication));
                verify(strategy, never()).sendRedirect(any(), any(), any());
            }
        }
    }
}
