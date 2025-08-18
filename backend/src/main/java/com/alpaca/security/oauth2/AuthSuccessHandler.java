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

/**
 * Authentication success handler for OAuth2 login flows.
 *
 * <p>Upon successful authentication, this handler issues a JWT access token and redirects the
 * user's browser to a previously stored or default redirect URI. It ensures that only authorized
 * redirect URIs are used to prevent open redirect vulnerabilities.
 *
 * <p>It also cleans up the related cookies used during the OAuth flow (authorization request and
 * redirect URI cookies) via {@link CookieAuthReqRepo}.
 *
 * @see SimpleUrlAuthenticationSuccessHandler
 * @see JJwtManager
 * @see CookieAuthReqRepo
 */
@Component
public class AuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JJwtManager jwtManager;
    private final CookieAuthReqRepo repository;
    private final Set<URI> authorizedRedirectUris;

    /**
     * Constructs the handler with required dependencies and a list of authorized redirect URIs.
     *
     * @param jwtManager JWT manager used to create tokens
     * @param repository cookie-based authorization request repository
     * @param redirectUris list of allowed redirect URIs (must not be {@code null})
     */
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
            logger.debug(
                    String.format("Response already committed; cannot redirect to: %s", targetUrl));
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

    /**
     * Clears authentication-related cookies and attributes post-login.
     *
     * @param request the current HTTP request
     * @param response the current HTTP response
     */
    protected void clearAuthenticationAttributes(
            HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        repository.removeAuthorizationRequestCookies(request, response);
    }

    /**
     * Validates whether the given redirect URI is among the authorized list. Only the host is
     * matched to allow flexibility in paths.
     *
     * @param clientUri the URI requested for redirection
     * @return {@code true} if the host matches any authorized redirect URI; {@code false} otherwise
     */
    private boolean isAuthorizedRedirectURI(URI clientUri) {
        return authorizedRedirectUris.stream()
                .anyMatch(auth -> auth.getHost().equalsIgnoreCase(clientUri.getHost()));
    }
}
