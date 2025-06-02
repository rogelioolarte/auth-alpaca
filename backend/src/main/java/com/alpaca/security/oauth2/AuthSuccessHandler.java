package com.alpaca.security.oauth2;

import static com.alpaca.security.oauth2.CookieAuthReqRepo.RedirectCookieName;

import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.security.manager.CookieManager;
import com.alpaca.security.manager.JJwtManager;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class AuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final JJwtManager jwtManager;
  private final CookieAuthReqRepo repository;
  private final Set<URI> authorizedRedirectUris;

  public AuthSuccessHandler(
      JJwtManager jwtManager,
      CookieAuthReqRepo repository,
      @Value("${app.oauth2AuthorizedRedirectURI}") @NonNull List<URI> redirectUris) {
    this.jwtManager = jwtManager;
    this.repository = repository;
    this.authorizedRedirectUris = Set.copyOf(redirectUris);
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {
    String targetUrl = determineTargetUrl(request, response, authentication);
    if (response.isCommitted()) {
      logger.debug(String.format("Response already committed; cannot redirect to: %s", targetUrl));
      return;
    }
    clearAuthenticationAttributes(request, response);
    getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }

  @Override
  protected String determineTargetUrl(
      HttpServletRequest request, HttpServletResponse response, Authentication auth) {
    Optional<Cookie> redirectCookie = CookieManager.getCookie(request, RedirectCookieName);
    String target = redirectCookie.map(Cookie::getValue).orElse(getDefaultTargetUrl());

    if (!authorizedRedirectUris.isEmpty() && !isAuthorizedRedirectURI(URI.create(target))) {
      throw new UnauthorizedException("Unauthorized Redirect URI");
    }
    return UriComponentsBuilder.fromUriString(target)
        .queryParam("token", jwtManager.createToken((UserPrincipal) auth.getPrincipal()))
        .build()
        .toUriString();
  }

  protected void clearAuthenticationAttributes(
      HttpServletRequest request, HttpServletResponse response) {
    super.clearAuthenticationAttributes(request);
    repository.removeAuthorizationRequestCookies(request, response);
  }

  private boolean isAuthorizedRedirectURI(URI clientUri) {
    return authorizedRedirectUris.stream()
        .anyMatch(
            auth ->
                auth.getHost().equalsIgnoreCase(clientUri.getHost())
                    && auth.getPort() == clientUri.getPort());
  }
}
