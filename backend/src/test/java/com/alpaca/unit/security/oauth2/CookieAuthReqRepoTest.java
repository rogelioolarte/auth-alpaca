package com.alpaca.unit.security.oauth2;

import com.alpaca.security.manager.CookieManager;
import com.alpaca.security.oauth2.CookieAuthReqRepo;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Unit tests for {@link CookieAuthReqRepo} */
@DisplayName("CookieAuthReqRepo Unit Tests")
class CookieAuthReqRepoTest {

    private CookieAuthReqRepo repo;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        repo = new CookieAuthReqRepo();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Nested
    @DisplayName("loadAuthorizationRequest()")
    class LoadAuthorizationRequestTests {

        @Test
        @DisplayName(
                "Given an existing auth cookie, returns the deserialized"
                        + " OAuth2AuthorizationRequest")
        void givenAuthCookiePresent_whenLoadAuthorizationRequest_thenReturnDeserializedRequest() {
            Cookie cookie = new Cookie(CookieAuthReqRepo.AuthorizationCookieName, "cookie-value");
            OAuth2AuthorizationRequest expected = mock(OAuth2AuthorizationRequest.class);

            try (MockedStatic<CookieManager> cm = mockStatic(CookieManager.class)) {
                cm.when(
                                () ->
                                        CookieManager.getCookie(
                                                request, CookieAuthReqRepo.AuthorizationCookieName))
                        .thenReturn(Optional.of(cookie));
                cm.when(() -> CookieManager.deserialize(cookie, OAuth2AuthorizationRequest.class))
                        .thenReturn(expected);

                OAuth2AuthorizationRequest actual = repo.loadAuthorizationRequest(request);

                assertAll(
                        "loaded request",
                        () -> assertNotNull(actual, "Should not be null"),
                        () -> assertEquals(expected, actual, "Should match deserialized request"));
            }
        }

        @Test
        @DisplayName("Given no auth cookie, returns null")
        void givenAuthCookieMissing_whenLoadAuthorizationRequest_thenReturnNull() {
            try (MockedStatic<CookieManager> cm = mockStatic(CookieManager.class)) {
                cm.when(
                                () ->
                                        CookieManager.getCookie(
                                                request, CookieAuthReqRepo.AuthorizationCookieName))
                        .thenReturn(Optional.empty());

                assertNull(
                        repo.loadAuthorizationRequest(request),
                        "Should return null if no cookie present");
            }
        }
    }

    @Nested
    @DisplayName("saveAuthorizationRequest()")
    class SaveAuthorizationRequestTests {

        @Test
        @DisplayName("Given null authorizationRequest, deletes both cookies")
        void givenNullAuthRequest_whenSaveAuthorizationRequest_thenDeleteBothCookies() {
            try (MockedStatic<CookieManager> cm = mockStatic(CookieManager.class)) {
                repo.saveAuthorizationRequest(null, request, response);

                cm.verify(
                        () ->
                                CookieManager.deleteCookie(
                                        request,
                                        response,
                                        CookieAuthReqRepo.AuthorizationCookieName),
                        times(1));
                cm.verify(
                        () ->
                                CookieManager.deleteCookie(
                                        request, response, CookieAuthReqRepo.RedirectCookieName),
                        times(1));

                // Should not add any cookies when auth request is null
                cm.verify(
                        () ->
                                CookieManager.addCookie(
                                        any(HttpServletResponse.class),
                                        anyString(),
                                        anyString(),
                                        anyInt()),
                        never());
            }
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Given valid auth request and null/empty redirectUri, only adds auth cookie")
        void givenNullOrEmptyRedirectUri_whenSaveAuthorizationRequest_thenOnlyAddAuthCookie(
                String redirectUri) {

            OAuth2AuthorizationRequest authReq = mock(OAuth2AuthorizationRequest.class);
            request.addParameter(CookieAuthReqRepo.RedirectCookieName, redirectUri);

            try (MockedStatic<CookieManager> cm = mockStatic(CookieManager.class)) {
                cm.when(() -> CookieManager.serialize(authReq)).thenReturn("serialized-value");

                repo.saveAuthorizationRequest(authReq, request, response);

                cm.verify(
                        () ->
                                CookieManager.addCookie(
                                        response,
                                        CookieAuthReqRepo.AuthorizationCookieName,
                                        "serialized-value",
                                        CookieAuthReqRepo.cookieExpiredSeconds),
                        times(1));

                cm.verify(
                        () ->
                                CookieManager.addCookie(
                                        eq(response),
                                        eq(CookieAuthReqRepo.RedirectCookieName),
                                        anyString(),
                                        anyInt()),
                        never());
            }
        }

        @Test
        @DisplayName("Given valid auth request and non-blank redirectUri, adds both cookies")
        void givenValidRedirectUri_whenSaveAuthorizationRequest_thenAddAuthAndRedirectCookies() {
            OAuth2AuthorizationRequest authReq = mock(OAuth2AuthorizationRequest.class);
            String redirect = "/home";
            request.addParameter(CookieAuthReqRepo.RedirectCookieName, redirect);

            try (MockedStatic<CookieManager> cm = mockStatic(CookieManager.class)) {
                cm.when(() -> CookieManager.serialize(authReq)).thenReturn("serialized-value");

                repo.saveAuthorizationRequest(authReq, request, response);

                cm.verify(
                        () ->
                                CookieManager.addCookie(
                                        response,
                                        CookieAuthReqRepo.AuthorizationCookieName,
                                        "serialized-value",
                                        CookieAuthReqRepo.cookieExpiredSeconds),
                        times(1));
                cm.verify(
                        () ->
                                CookieManager.addCookie(
                                        response,
                                        CookieAuthReqRepo.RedirectCookieName,
                                        redirect,
                                        CookieAuthReqRepo.cookieExpiredSeconds),
                        times(1));
            }
        }
    }

    @Nested
    @DisplayName("removeAuthorizationRequest()")
    class RemoveAuthorizationRequestTests {

        @Test
        @DisplayName("Delegates to loadAuthorizationRequest()")
        void whenRemoveAuthorizationRequest_thenReturnLoadedRequest() {
            Cookie cookie = new Cookie(CookieAuthReqRepo.AuthorizationCookieName, "cookie-value");
            OAuth2AuthorizationRequest expected = mock(OAuth2AuthorizationRequest.class);

            try (MockedStatic<CookieManager> cm = mockStatic(CookieManager.class)) {
                cm.when(
                                () ->
                                        CookieManager.getCookie(
                                                request, CookieAuthReqRepo.AuthorizationCookieName))
                        .thenReturn(Optional.of(cookie));
                cm.when(() -> CookieManager.deserialize(cookie, OAuth2AuthorizationRequest.class))
                        .thenReturn(expected);

                OAuth2AuthorizationRequest actual =
                        repo.removeAuthorizationRequest(request, response);

                assertEquals(
                        expected, actual, "Should return the same as loadAuthorizationRequest");
            }
        }
    }

    @Nested
    @DisplayName("removeAuthorizationRequestCookies()")
    class RemoveAuthorizationRequestCookiesTests {

        @Test
        @DisplayName("Deletes both auth and redirect cookies")
        void whenRemoveCookies_thenDeleteBoth() {
            try (MockedStatic<CookieManager> cm = mockStatic(CookieManager.class)) {
                repo.removeAuthorizationRequestCookies(request, response);

                cm.verify(
                        () ->
                                CookieManager.deleteCookie(
                                        request,
                                        response,
                                        CookieAuthReqRepo.AuthorizationCookieName),
                        times(1));
                cm.verify(
                        () ->
                                CookieManager.deleteCookie(
                                        request, response, CookieAuthReqRepo.RedirectCookieName),
                        times(1));
            }
        }
    }
}
