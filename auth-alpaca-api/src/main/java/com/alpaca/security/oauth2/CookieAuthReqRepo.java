package com.alpaca.security.oauth2;

import com.alpaca.security.manager.CookieManager;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

/**
 * Cookie-based implementation of {@link AuthorizationRequestRepository} for storing and retrieving
 * {@link OAuth2AuthorizationRequest} objects during OAuth2 login flows in a stateless manner.
 *
 * <p>By default, Spring Security stores the OAuth2 authorization request in the HTTP session using
 * {@code HttpSessionOAuth2AuthorizationRequestRepository}. This implementation replaces that
 * behavior by serializing the request into a secure cookie using {@link CookieManager}, enabling
 * stateless authentication flows (e.g., REST APIs or mobile clients).
 *
 * <p>Two cookies are used:
 *
 * <ul>
 *   <li>{@code oauth2_auth_request}: holds the serialized {@code OAuth2AuthorizationRequest}.
 *   <li>{@code redirect_uri}: optionally stores a post-login redirect URI provided by the client.
 * </ul>
 *
 * Cookies are short-lived and expire after a defined duration (e.g., 180 seconds).
 *
 * @see AuthorizationRequestRepository
 * @see CookieManager
 */
@Component
public class CookieAuthReqRepo
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    /** Cookie name holding the serialized {@link OAuth2AuthorizationRequest}. */
    public static final String AUTHORIZATION_COOKIE_NAME = "oauth2_auth_request";

    /** Cookie name for the post-login redirect URI. */
    public static final String REDIRECT_PARAM_NAME = "redirect_uri";

    /** Cookie name for the PKCE code challenge provided by the client. */
    public static final String CLIENT_CODE_CHALLENGE = "client_code_challenge";

    /** Cookie name for the Client ID provided by the client. */
    public static final String CLIENT_ID_PARAM = "client_id";

    /** Lifetime in seconds after which the authorization cookies expire. */
    public static final int COOKIE_EXPIRED_SECONDS = 600;

    /**
     * Loads the {@link OAuth2AuthorizationRequest} from the cookie if present.
     *
     * @param request the incoming HTTP request
     * @return the deserialized {@link OAuth2AuthorizationRequest}, or {@code null} if not found
     */
    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(
            @NonNull HttpServletRequest request) {
        Cookie cookie = CookieManager.getCookie(request, AUTHORIZATION_COOKIE_NAME).orElse(null);
        if (cookie != null) {
            return CookieManager.deserialize(cookie, OAuth2AuthorizationRequest.class);
        }
        return null;
    }

    /**
     * Saves the {@link OAuth2AuthorizationRequest} into a cookie, and optionally captures a
     * redirect URI to be stored in a separate cookie.
     *
     * <p>If {@code authorizationRequest} is {@code null}, deletes both cookies.
     *
     * @param authorizationRequest the OAuth2 request to save (or {@code null} to clear)
     * @param request the incoming HTTP request
     * @param response the outgoing HTTP response to which cookies will be added
     */
    @Override
    public void saveAuthorizationRequest(
            @NonNull OAuth2AuthorizationRequest authorizationRequest,
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response) {
        CookieManager.addCookie(
                request,
                response,
                AUTHORIZATION_COOKIE_NAME,
                CookieManager.serialize(authorizationRequest),
                COOKIE_EXPIRED_SECONDS);

        String redirectURIAfterLogin = request.getParameter(REDIRECT_PARAM_NAME);
        if (redirectURIAfterLogin != null && !redirectURIAfterLogin.isBlank()) {
            CookieManager.addCookie(
                    request,
                    response,
                    REDIRECT_PARAM_NAME,
                    redirectURIAfterLogin,
                    COOKIE_EXPIRED_SECONDS);
        }
        String clientId = request.getParameter(CLIENT_ID_PARAM);
        if (clientId != null && !clientId.isBlank()) {
            CookieManager.addCookie(
                    request, response, CLIENT_ID_PARAM, clientId, COOKIE_EXPIRED_SECONDS);
        }
        String clientCodeChallenge =
                (String) authorizationRequest.getAttributes().get(CLIENT_CODE_CHALLENGE);
        if (clientCodeChallenge != null && !clientCodeChallenge.isBlank()) {
            CookieManager.addCookie(
                    request,
                    response,
                    CLIENT_CODE_CHALLENGE,
                    clientCodeChallenge,
                    COOKIE_EXPIRED_SECONDS);
        }
    }

    /**
     * Removes the authorization request. This implementation defers to {@link
     * #loadAuthorizationRequest}.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @return the previously stored {@link OAuth2AuthorizationRequest}, or {@code null} if not
     *     available
     */
    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            @NonNull HttpServletRequest request, @NonNull HttpServletResponse response) {
        return loadAuthorizationRequest(request);
    }

    /**
     * Deletes both the authorization request and redirect URI cookies.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     */
    public void removeAuthorizationRequestCookies(
            HttpServletRequest request, HttpServletResponse response) {
        CookieManager.deleteCookie(request, response, AUTHORIZATION_COOKIE_NAME);
        CookieManager.deleteCookie(request, response, REDIRECT_PARAM_NAME);
        CookieManager.deleteCookie(request, response, CLIENT_CODE_CHALLENGE);
        CookieManager.deleteCookie(request, response, CLIENT_ID_PARAM);
    }
}
