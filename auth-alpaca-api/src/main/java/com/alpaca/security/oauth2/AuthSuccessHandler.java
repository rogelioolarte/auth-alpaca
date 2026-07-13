package com.alpaca.security.oauth2;

import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.InternalErrorException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.AuthCode;
import com.alpaca.model.UserPrincipal;
import com.alpaca.security.manager.CookieManager;
import com.alpaca.security.manager.TokenExchangeManager;
import com.alpaca.service.IAuthService;
import com.alpaca.utils.UUIDv7Generator;
import com.alpaca.utils.Utils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
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

    /** Query parameter and cookie name for the post-login redirect URI. */
    public static final String REDIRECT_PARAM_NAME = "redirect_uri";

    /** Attribute key and cookie name for the PKCE code challenge passed from the client. */
    public static final String CLIENT_CODE_CHALLENGE = "client_code_challenge";

    /** Query parameter and cookie name for the Client ID provided by the client. */
    public static final String CLIENT_ID_PARAM = "client_id";

    private final CookieAuthReqRepo repository;
    private final Set<URI> authorizedRedirectUris;
    private final TokenExchangeManager exchangeManager;
    private final UUIDv7Generator uuidGenerator;

    /**
     * Constructs an {@code AuthSuccessHandler}.
     *
     * @param repository cookie-based repository used to manage OAuth2 state cookies
     * @param redirectUri URI authorized for redirection; must not be {@code null}
     * @param exchangeManager manager for creating and consuming exchange codes
     * @param uuidGenerator generator for UUIDv7 exchange codes
     */
    public AuthSuccessHandler(
            CookieAuthReqRepo repository,
            @Value("${app.frontend.uri}") URI redirectUri,
            TokenExchangeManager exchangeManager,
            UUIDv7Generator uuidGenerator) {
        this.repository = repository;
        this.authorizedRedirectUris = Set.of(redirectUri);
        this.exchangeManager = exchangeManager;
        this.uuidGenerator = uuidGenerator;
    }

    /**
     * Called when authentication is successful.
     *
     * <p>This method builds a Redirect URL containing the code to exchange the tokens, and the
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
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Authentication authentication)
            throws IOException {
        if (response.isCommitted()) {
            logger.debug("Response already committed; cannot redirect");
            return;
        }
        String targetUrl = determineTargetUrl(request, response, authentication);
        String codeChallenge = determineCodeChallenge(request);
        if (codeChallenge.isBlank()) {
            repository.removeAuthorizationRequestCookies(request, response);
            clearAuthenticationAttributes(request, response);
            throw new BadRequestException("Invalid Code Challenge");
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        if (userPrincipal == null) {
            repository.removeAuthorizationRequestCookies(request, response);
            clearAuthenticationAttributes(request, response);
            throw new UnauthorizedException("Invalid Credentials");
        }
        String code = uuidGenerator.generate().toString();
        String clientId =
                CookieManager.getCookie(request, CLIENT_ID_PARAM).map(Cookie::getValue).orElse("");
        String userAgent = Optional.ofNullable(request.getHeader("User-Agent")).orElse("");
        String clientIp = Optional.ofNullable(Utils.extractClientIP(request)).orElse("");

        repository.removeAuthorizationRequestCookies(request, response);
        clearAuthenticationAttributes(request, response);

        AuthCode authCode =
                new AuthCode(
                        code,
                        codeChallenge,
                        clientId,
                        userAgent,
                        clientIp,
                        userPrincipal.getUserId(),
                        targetUrl);

        this.exchangeManager.createExchangeCode(code, authCode);
        String finalRedirectURL =
                UriComponentsBuilder.fromUriString(targetUrl)
                        .queryParam("code", code)
                        .build()
                        .toUriString();

        getRedirectStrategy().sendRedirect(request, response, finalRedirectURL);
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
    @NonNull
    protected String determineTargetUrl(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            Authentication auth) {

        Optional<Cookie> redirectCookie = CookieManager.getCookie(request, REDIRECT_PARAM_NAME);
        String target = redirectCookie.map(Cookie::getValue).orElse(getDefaultTargetUrl());
        if (!isAuthorizedRedirectURI(URI.create(target))) {
            throw new UnauthorizedException("Unauthorized redirect URI");
        }

        return UriComponentsBuilder.fromUriString(target).build().toUriString();
    }

    /**
     * Resolves the PKCE code challenge value associated with the current OAuth2 authorization
     * request.
     *
     * <p>Priority order:
     *
     * <ol>
     *   <li>From the stored {@link OAuth2AuthorizationRequest} attributes (set during the initial
     *       authorization request by {@link com.alpaca.security.oauth2.OAuth2ReqResolver}).
     *   <li>From the {@code client_code_challenge} cookie (persisted by {@link CookieAuthReqRepo}
     *       for stateless flows).
     * </ol>
     *
     * @param request the incoming HTTP request
     * @return the code challenge string, or empty string if not found
     */
    protected String determineCodeChallenge(HttpServletRequest request) {
        OAuth2AuthorizationRequest authReq = repository.loadAuthorizationRequest(request);
        if (Objects.isNull(authReq)) {
            return "";
        }
        String challenge = (String) authReq.getAttributes().get(CLIENT_CODE_CHALLENGE);
        if (challenge != null && !challenge.isBlank()) {
            return challenge;
        }
        Optional<Cookie> cookie = CookieManager.getCookie(request, CLIENT_CODE_CHALLENGE);
        return cookie.map(Cookie::getValue).orElse("");
    }

    /**
     * Removes temporary authentication-related cookies after login.
     *
     * <p>This clears any stored OAuth2 authorization request cookies to avoid lingering state after
     * a successful authentication flow.
     *
     * @param request the current HTTP request
     * @param ignoredResponse the current HTTP response
     */
    protected void clearAuthenticationAttributes(
            HttpServletRequest request, HttpServletResponse ignoredResponse) {
        super.clearAuthenticationAttributes(request);
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
