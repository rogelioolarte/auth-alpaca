package com.alpaca.unit.security.oauth2;

import static org.mockito.Mockito.*;

import com.alpaca.security.manager.CookieManager;
import com.alpaca.security.oauth2.AuthFailureHandler;
import com.alpaca.security.oauth2.CookieAuthReqRepo;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.RedirectStrategy;

/** Unit tests for {@link AuthFailureHandler} */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthFailureHandler Unit Tests")
class AuthFailureHandlerTest {

    private static final String FRONTEND_URI = "http://test.test/";
    public static final String REDIRECT_PARAM_NAME = "redirect_uri";

    @Mock private CookieAuthReqRepo repository;
    @Mock private RedirectStrategy redirectStrategy;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private AuthFailureHandler handler;
    private AuthenticationException exception;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        handler = new AuthFailureHandler(repository, FRONTEND_URI);
        handler.setRedirectStrategy(redirectStrategy);
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
            request.setParameter("redirect_uri", "http://test/next");
            request.setParameter("error", "bad|input[]");

            handler.onAuthenticationFailure(request, response, exception);

            verify(repository).removeAuthorizationRequestCookies(request, response);
            String expected = "http://test/next?error=bad input";
            verify(redirectStrategy).sendRedirect(request, response, expected);
        }

        @Test
        @DisplayName("Given cookie 'redirect_uri' if param absent")
        void cookieRedirectUri_andExceptionMessage() throws IOException {
            try (MockedStatic<CookieManager> cm = mockStatic(CookieManager.class)) {
                Cookie c = new Cookie(REDIRECT_PARAM_NAME, "http://test/uri");
                cm.when(() -> CookieManager.getCookie(request, REDIRECT_PARAM_NAME))
                        .thenReturn(Optional.of(c));

                handler.onAuthenticationFailure(request, response, exception);

                verify(repository).removeAuthorizationRequestCookies(request, response);
                String sanitized = "orig error"; // from exception.getMessage()
                String expected = "http://test/uri?error=" + sanitized;
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
