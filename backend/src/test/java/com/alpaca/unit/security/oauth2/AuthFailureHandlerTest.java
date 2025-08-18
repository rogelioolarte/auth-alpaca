package com.alpaca.unit.security.oauth2;

import com.alpaca.security.manager.CookieManager;
import com.alpaca.security.oauth2.AuthFailureHandler;
import com.alpaca.security.oauth2.CookieAuthReqRepo;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.RedirectStrategy;

import java.io.IOException;
import java.util.Optional;

import static org.mockito.Mockito.*;

/** Unit tests for {@link AuthFailureHandler} */
@DisplayName("AuthFailureHandler Unit Tests")
class AuthFailureHandlerTest {

    private static final String FRONTEND_URI = "http://frontend.app/";
    private AuthFailureHandler handler;
    private CookieAuthReqRepo repository;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private RedirectStrategy redirectStrategy;
    private AuthenticationException exception;

    @BeforeEach
    void setUp() {
        repository = mock(CookieAuthReqRepo.class);
        handler = new AuthFailureHandler(repository, FRONTEND_URI);
        redirectStrategy = mock(RedirectStrategy.class);
        handler.setRedirectStrategy(redirectStrategy);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        exception = new AuthenticationException("orig|error[]{}") {};
    }

    @Test
    @DisplayName("Should not call remove cookies or redirect when response is committed")
    void whenResponseCommitted_doNothing() throws IOException {
        HttpServletResponse committed =
                new MockHttpServletResponse() {
                    @Override
                    public boolean isCommitted() {
                        return true;
                    }
                };
        handler.onAuthenticationFailure(request, committed, exception);
        verify(repository, never()).removeAuthorizationRequestCookies(any(), any());
        verify(redirectStrategy, never()).sendRedirect(any(), any(), anyString());
    }

    @Nested
    @DisplayName("resolveTargetUrl and appendErrorParam flows")
    class RedirectCases {

        @Test
        @DisplayName("Given request parameter 'redirect_uri', uses it and error param from request")
        void paramRedirectUri_andParamError() throws IOException {
            request.setParameter("redirect_uri", "http://app/next");
            request.setParameter("error", "bad|input[]");

            handler.onAuthenticationFailure(request, response, exception);

            verify(repository).removeAuthorizationRequestCookies(request, response);
            String expected = "http://app/next?error=bad input";
            verify(redirectStrategy).sendRedirect(request, response, expected);
        }

        @Test
        @DisplayName("Given cookie 'redirect_uri' if param absent")
        void cookieRedirectUri_andExceptionMessage() throws IOException {
            try (MockedStatic<CookieManager> cm = mockStatic(CookieManager.class)) {
                Cookie c = new Cookie(CookieAuthReqRepo.RedirectCookieName, "http://cookie/uri");
                cm.when(
                                () ->
                                        CookieManager.getCookie(
                                                request, CookieAuthReqRepo.RedirectCookieName))
                        .thenReturn(Optional.of(c));

                handler.onAuthenticationFailure(request, response, exception);

                verify(repository).removeAuthorizationRequestCookies(request, response);
                String sanitized = "orig error"; // from exception.getMessage()
                String expected = "http://cookie/uri?error=" + sanitized;
                verify(redirectStrategy).sendRedirect(request, response, expected);
            }
        }

        @Test
        @DisplayName("Falls back to frontendUri when no param or cookie")
        void fallbackFrontendUri() throws IOException {
            // no param, no cookie -> use FRONTEND_URI
            handler.onAuthenticationFailure(request, response, exception);

            verify(repository).removeAuthorizationRequestCookies(request, response);
            String expected = FRONTEND_URI + "?error=orig error";
            verify(redirectStrategy).sendRedirect(request, response, expected);
        }
    }
}
