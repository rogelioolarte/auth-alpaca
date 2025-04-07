package com.alpaca.security.oauth2;

import com.alpaca.security.manager.CookieManager;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CookieAuthReqRepo
    implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

  public static final String AuthorizationCookieName = "oauth2_auth_request";
  public static final String RedirectCookieName = "redirect_uri";
  public static final int cookieExpiredSeconds = 180;

  @Override
  public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
    Optional<Cookie> cookie = CookieManager.getCookie(request, AuthorizationCookieName);
    if (cookie.isPresent()) {
      Cookie cookieP = cookie.get();
      return CookieManager.deserialize(cookieP, OAuth2AuthorizationRequest.class);
    }
    return null;
  }

  @Override
  public void saveAuthorizationRequest(
      OAuth2AuthorizationRequest authorizationRequest,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (authorizationRequest == null) {
      CookieManager.deleteCookie(request, response, AuthorizationCookieName);
      CookieManager.deleteCookie(request, response, RedirectCookieName);
    }
    if (authorizationRequest != null) {
      CookieManager.addCookie(
          response,
          AuthorizationCookieName,
          CookieManager.serialize(authorizationRequest),
          cookieExpiredSeconds);
    }
    String redirectURIAfterLogin = request.getParameter(RedirectCookieName);
    if (redirectURIAfterLogin != null && !redirectURIAfterLogin.isBlank()) {
      CookieManager.addCookie(
          response, RedirectCookieName, redirectURIAfterLogin, cookieExpiredSeconds);
    }
  }

  @Override
  public OAuth2AuthorizationRequest removeAuthorizationRequest(
      HttpServletRequest request, HttpServletResponse response) {
    return loadAuthorizationRequest(request);
  }

  public void removeAuthorizationRequestCookies(
      HttpServletRequest request, HttpServletResponse response) {
    CookieManager.deleteCookie(request, response, AuthorizationCookieName);
    CookieManager.deleteCookie(request, response, RedirectCookieName);
  }
}
