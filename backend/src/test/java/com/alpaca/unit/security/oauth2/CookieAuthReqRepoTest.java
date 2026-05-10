package com.alpaca.unit.security.oauth2;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alpaca.security.manager.CookieManager;
import com.alpaca.security.oauth2.CookieAuthReqRepo;
import jakarta.servlet.http.Cookie;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

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

    @Test
    @DisplayName("loadAuthorizationRequest: Should return deserialized request when cookie exists")
    void loadAuthorizationRequest_ShouldReturnRequest_WhenCookieExists() {
        String cookieValue = "serialized-data";
        Cookie cookie = new Cookie(CookieAuthReqRepo.AUTHORIZATION_COOKIE_NAME, cookieValue);
        OAuth2AuthorizationRequest expected = mock(OAuth2AuthorizationRequest.class);

        try (MockedStatic<CookieManager> cm = mockStatic(CookieManager.class)) {
            cm.when(
                            () ->
                                    CookieManager.getCookie(
                                            request, CookieAuthReqRepo.AUTHORIZATION_COOKIE_NAME))
                    .thenReturn(Optional.of(cookie));
            cm.when(() -> CookieManager.deserialize(cookie, OAuth2AuthorizationRequest.class))
                    .thenReturn(expected);

            OAuth2AuthorizationRequest actual = repo.loadAuthorizationRequest(request);

            assertEquals(expected, actual);
        }
    }

    @Test
    @DisplayName("loadAuthorizationRequest: Should return null when cookie is missing")
    void loadAuthorizationRequest_ShouldReturnNull_WhenCookieMissing() {
        try (MockedStatic<CookieManager> cm = mockStatic(CookieManager.class)) {
            cm.when(
                            () ->
                                    CookieManager.getCookie(
                                            request, CookieAuthReqRepo.AUTHORIZATION_COOKIE_NAME))
                    .thenReturn(Optional.empty());

            OAuth2AuthorizationRequest actual = repo.loadAuthorizationRequest(request);

            assertNull(actual);
        }
    }

    @Test
    @DisplayName("saveAuthorizationRequest: Should delete all cookies when request is null")
    void saveAuthorizationRequest_ShouldDeleteCookies_WhenRequestIsNull() {
        try (MockedStatic<CookieManager> cm = mockStatic(CookieManager.class)) {
            repo.saveAuthorizationRequest(null, request, response);

            cm.verify(
                    () ->
                            CookieManager.deleteCookie(
                                    request,
                                    response,
                                    CookieAuthReqRepo.AUTHORIZATION_COOKIE_NAME));
            cm.verify(
                    () ->
                            CookieManager.deleteCookie(
                                    request, response, CookieAuthReqRepo.REDIRECT_PARAM_NAME));
            cm.verify(
                    () ->
                            CookieManager.deleteCookie(
                                    request, response, CookieAuthReqRepo.CLIENT_CODE_CHALLENGE));
        }
    }

    @Test
    @DisplayName(
            "saveAuthorizationRequest: Should add cookies for auth, redirect, and client challenge")
    void saveAuthorizationRequest_ShouldAddAllCookies_WhenPresent() {
        OAuth2AuthorizationRequest authReq = mock(OAuth2AuthorizationRequest.class);
        String serialized = "serialized-auth-req";
        String redirectUri = "[https://alpaca.com/dashboard](https://alpaca.com/dashboard)";
        String challenge = "pkce-challenge-v7";
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(CookieAuthReqRepo.CLIENT_CODE_CHALLENGE, challenge);

        request.addParameter(CookieAuthReqRepo.REDIRECT_PARAM_NAME, redirectUri);
        when(authReq.getAttributes()).thenReturn(attributes);

        try (MockedStatic<CookieManager> cm = mockStatic(CookieManager.class)) {
            cm.when(() -> CookieManager.serialize(authReq)).thenReturn(serialized);

            repo.saveAuthorizationRequest(authReq, request, response);

            cm.verify(
                    () ->
                            CookieManager.addCookie(
                                    response,
                                    CookieAuthReqRepo.AUTHORIZATION_COOKIE_NAME,
                                    serialized,
                                    CookieAuthReqRepo.COOKIE_EXPIRED_SECONDS));
            cm.verify(
                    () ->
                            CookieManager.addCookie(
                                    response,
                                    CookieAuthReqRepo.REDIRECT_PARAM_NAME,
                                    redirectUri,
                                    CookieAuthReqRepo.COOKIE_EXPIRED_SECONDS));
            cm.verify(
                    () ->
                            CookieManager.addCookie(
                                    response,
                                    CookieAuthReqRepo.CLIENT_CODE_CHALLENGE,
                                    challenge,
                                    CookieAuthReqRepo.COOKIE_EXPIRED_SECONDS));
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("saveAuthorizationRequest: Should skip redirect and challenge cookies if blank")
    void saveAuthorizationRequest_ShouldSkipCookies_WhenDataIsBlank(String blankValue) {
        OAuth2AuthorizationRequest authReq = mock(OAuth2AuthorizationRequest.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(CookieAuthReqRepo.CLIENT_CODE_CHALLENGE, blankValue);

        request.addParameter(CookieAuthReqRepo.REDIRECT_PARAM_NAME, blankValue);
        when(authReq.getAttributes()).thenReturn(attributes);

        try (MockedStatic<CookieManager> cm = mockStatic(CookieManager.class)) {
            cm.when(() -> CookieManager.serialize(authReq)).thenReturn("data");

            repo.saveAuthorizationRequest(authReq, request, response);

            cm.verify(
                    () ->
                            CookieManager.addCookie(
                                    eq(response),
                                    eq(CookieAuthReqRepo.REDIRECT_PARAM_NAME),
                                    anyString(),
                                    anyInt()),
                    never());
            cm.verify(
                    () ->
                            CookieManager.addCookie(
                                    eq(response),
                                    eq(CookieAuthReqRepo.CLIENT_CODE_CHALLENGE),
                                    anyString(),
                                    anyInt()),
                    never());
        }
    }

    @Test
    @DisplayName("removeAuthorizationRequest: Should delegate to loadAuthorizationRequest")
    void removeAuthorizationRequest_ShouldReturnLoadedRequest() {
        Cookie cookie = new Cookie(CookieAuthReqRepo.AUTHORIZATION_COOKIE_NAME, "val");
        OAuth2AuthorizationRequest expected = mock(OAuth2AuthorizationRequest.class);

        try (MockedStatic<CookieManager> cm = mockStatic(CookieManager.class)) {
            cm.when(
                            () ->
                                    CookieManager.getCookie(
                                            request, CookieAuthReqRepo.AUTHORIZATION_COOKIE_NAME))
                    .thenReturn(Optional.of(cookie));
            cm.when(() -> CookieManager.deserialize(cookie, OAuth2AuthorizationRequest.class))
                    .thenReturn(expected);

            OAuth2AuthorizationRequest actual = repo.removeAuthorizationRequest(request, response);

            assertEquals(expected, actual);
        }
    }

    @Test
    @DisplayName("removeAuthorizationRequestCookies: Should explicitly delete all state cookies")
    void removeAuthorizationRequestCookies_ShouldDeleteAll() {
        try (MockedStatic<CookieManager> cm = mockStatic(CookieManager.class)) {
            repo.removeAuthorizationRequestCookies(request, response);

            cm.verify(
                    () ->
                            CookieManager.deleteCookie(
                                    request,
                                    response,
                                    CookieAuthReqRepo.AUTHORIZATION_COOKIE_NAME));
            cm.verify(
                    () ->
                            CookieManager.deleteCookie(
                                    request, response, CookieAuthReqRepo.REDIRECT_PARAM_NAME));
            cm.verify(
                    () ->
                            CookieManager.deleteCookie(
                                    request, response, CookieAuthReqRepo.CLIENT_CODE_CHALLENGE));
        }
    }
}
