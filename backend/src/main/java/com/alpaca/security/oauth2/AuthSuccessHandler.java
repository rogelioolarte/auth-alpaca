package com.alpaca.security.oauth2;

import static com.alpaca.security.oauth2.CookieAuthReqRepo.REDIRECT_COOKIE_NAME;

import com.alpaca.dto.request.AuthLoginRequestDTO;
import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.exception.InternalErrorException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.security.manager.CookieManager;
import com.alpaca.service.IAuthService;
import com.alpaca.utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Handles successful authentication for OAuth2 login flows.
 *
 * <p>When a user successfully logs in using OAuth2, this handler constructs a JSON response that
 * includes an access token, a refresh token, and the target URL to which the client should navigate
 * next. The handler also ensures that only authorized redirect URIs are accepted, preventing open
 * redirect vulnerabilities.
 *
 * <p>This class extends {@code SimpleUrlAuthenticationSuccessHandler} to integrate with Spring
 * Security, but overrides behavior to produce a JSON response rather than performing a redirect
 * directly.
 *
 * <p>OAuth2 authorization request-related cookies are cleaned up after successful login using
 * {@code CookieAuthReqRepo}.
 *
 * @see SimpleUrlAuthenticationSuccessHandler
 * @see IAuthService
 * @see CookieManager
 */
@Component
public class AuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final CookieAuthReqRepo repository;
    private final Set<URI> authorizedRedirectUris;
    private final IAuthService authService;
    private final ObjectMapper objectMapper;

    /**
     * Constructs an {@code AuthSuccessHandler}.
     *
     * @param repository cookie-based repository used to manage OAuth2 state cookies
     * @param redirectUris list of URIs authorized for redirection; must not be {@code null}
     * @param authService service used to generate JWT authentication tokens
     * @param objectMapper configured {@code ObjectMapper} for serializing JSON responses
     */
    public AuthSuccessHandler(
            CookieAuthReqRepo repository,
            @Value("${app.oauth2.authorized-redirect-uri}") @NonNull List<URI> redirectUris,
            IAuthService authService,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.authorizedRedirectUris = Set.copyOf(redirectUris);
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    /**
     * Called when authentication is successful.
     *
     * <p>This method builds a JSON response containing the access token, refresh token, and the
     * target URL to navigate to after login. It first determines the target URI, clears any OAuth2
     * state cookies, and then writes the JSON body to the response.
     *
     * @param request the current HTTP request
     * @param response the current HTTP response
     * @param authentication the authentication result containing the authenticated principal
     * @throws IOException if writing the JSON response fails
     */
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        if (response.isCommitted()) {
            logger.debug("Response already committed; cannot redirect");
            return;
        }
        String targetUrl = determineTargetUrl(request, response, authentication);
        clearAuthenticationAttributes(request, response);

        AuthResponseDTO authResponse =
                authService.login(
                        (UserPrincipal) authentication.getPrincipal(),
                        new AuthLoginRequestDTO(
                                "",
                                "",
                                request.getHeader("X-Client-Id"),
                                request.getHeader("User-Agent"),
                                Utils.extractClientIP(request)));
        Map<String, Object> body =
                Map.of(
                        "accessToken", authResponse.accessToken(),
                        "refreshToken", authResponse.refreshToken(),
                        "targetUrl", targetUrl);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }

    /**
     * Determines the target URL to which the client should navigate after login.
     *
     * <p>This method retrieves a stored redirect URI from a cookie if present, or uses a configured
     * default if not. It verifies that the resulting URI's host is in the configured whitelist of
     * authorized redirect URIs.
     *
     * @param request the current HTTP request
     * @param response the current HTTP response
     * @param auth the authentication result
     * @return a valid URI string for redirecting the client
     * @throws InternalErrorException if no authorized redirect URIs are configured
     * @throws UnauthorizedException if the requested redirect URI is not authorized
     */
    @Override
    protected String determineTargetUrl(
            HttpServletRequest request, HttpServletResponse response, Authentication auth) {

        if (authorizedRedirectUris.isEmpty()) {
            throw new InternalErrorException("Bad configuration of authorized redirect URIs");
        }

        Optional<Cookie> redirectCookie = CookieManager.getCookie(request, REDIRECT_COOKIE_NAME);
        String target = redirectCookie.map(Cookie::getValue).orElse(getDefaultTargetUrl());

        if (!isAuthorizedRedirectURI(URI.create(target))) {
            throw new UnauthorizedException("Unauthorized redirect URI");
        }

        return UriComponentsBuilder.fromUriString(target).build().toUriString();
    }

    /**
     * Removes temporary authentication-related cookies after login.
     *
     * <p>This clears any stored OAuth2 authorization request cookies to avoid lingering state after
     * a successful authentication flow.
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
     * Checks if the provided redirect URI belongs to the configured set of authorized hosts.
     *
     * <p>Only the host component is compared to allow flexibility in varying paths. A match
     * indicates that the redirect is permitted.
     *
     * @param clientUri the URI that the client intends to redirect to
     * @return {@code true} if the host of the {@code clientUri} matches any authorized host
     */
    private boolean isAuthorizedRedirectURI(URI clientUri) {
        return authorizedRedirectUris.stream()
                .anyMatch(auth -> auth.getHost().equalsIgnoreCase(clientUri.getHost()));
    }
}
