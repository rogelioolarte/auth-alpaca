package com.alpaca.unit.security.oauth2;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alpaca.security.manager.CookieManager;
import com.alpaca.security.oauth2.AuthRequestDeserializer;
import com.alpaca.security.oauth2.CookieAuthReqRepo;
import jakarta.servlet.http.Cookie;
import java.util.Optional;
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

/** Unit tests for {@link CookieAuthReqRepo} */
@DisplayName("Unit tests for CookieAuthReqRepo")
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
        "Given an existing auth cookie, returns the deserialized OAuth2AuthorizationRequest")
    void givenAuthCookiePresent_whenLoadAuthorizationRequest_thenReturnDeserializedRequest() {
      Cookie cookie = new Cookie(CookieAuthReqRepo.AuthorizationCookieName, "cookie-value");
      OAuth2AuthorizationRequest expected = mock(OAuth2AuthorizationRequest.class);
      try (MockedStatic<CookieManager> mock = mockStatic(CookieManager.class)) {
        mock.when(() -> CookieManager.getCookie(request, CookieAuthReqRepo.AuthorizationCookieName))
            .thenReturn(Optional.of(cookie));
        mock.when(() -> CookieManager.deserialize(cookie, OAuth2AuthorizationRequest.class))
            .thenReturn(expected);
        OAuth2AuthorizationRequest actual = repo.loadAuthorizationRequest(request);
        assertAll(
            "verify loaded request",
            () -> assertNotNull(actual, "Result should not be null"),
            () -> assertEquals(expected, actual, "Result should match the deserialized request"));
      }
    }

    @Test
    @DisplayName("Given no auth cookie, returns null")
    void givenAuthCookieMissing_whenLoadAuthorizationRequest_thenReturnNull() {
      try (MockedStatic<CookieManager> mock = mockStatic(CookieManager.class)) {
        mock.when(() -> CookieManager.getCookie(request, CookieAuthReqRepo.AuthorizationCookieName))
            .thenReturn(Optional.empty());
        assertNull(
            repo.loadAuthorizationRequest(request),
            "Result should be null when no cookie is present");
      }
    }
  }

  @Nested
  @DisplayName("saveAuthorizationRequest()")
  class SaveAuthorizationRequestTests {

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Given null or empty redirectUri, only adds the authorization cookie")
    void givenNullOrEmptyRedirectUri_whenSaveAuthorizationRequest_thenOnlyAddAuthCookie(
        String redirectUri) {
      OAuth2AuthorizationRequest authReq = mock(OAuth2AuthorizationRequest.class);
      request.addParameter(CookieAuthReqRepo.RedirectCookieName, redirectUri);
      try (MockedStatic<CookieManager> mock = mockStatic(CookieManager.class)) {
        mock.when(() -> CookieManager.serialize(authReq)).thenReturn("serialized-value");
        repo.saveAuthorizationRequest(authReq, request, response);
        mock.verify(
            () ->
                CookieManager.addCookie(
                    response,
                    CookieAuthReqRepo.AuthorizationCookieName,
                    "serialized-value",
                    CookieAuthReqRepo.cookieExpiredSeconds),
            times(1));
        mock.verify(
            () ->
                CookieManager.addCookie(
                    eq(response), eq(CookieAuthReqRepo.RedirectCookieName), anyString(), anyInt()),
            never());
      }
    }
  }
}
