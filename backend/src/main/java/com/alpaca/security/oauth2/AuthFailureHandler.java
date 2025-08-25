package com.alpaca.security.oauth2;

import static com.alpaca.security.oauth2.CookieAuthReqRepo.RedirectCookieName;

import com.alpaca.security.manager.CookieManager;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Custom authentication failure handler for OAuth2 login flows.
 *
 * <p>When authentication fails, this handler:
 *
 * <ul>
 *   <li>Removes cookies used during the OAuth2 authorization request (via {@link
 *       CookieAuthReqRepo}).
 *   <li>Determines an appropriate redirect URL, falling back to a configured frontend URI as
 *       needed.
 *   <li>Appends an `error` query parameter to the redirect target—with sanitized message content—to
 *       inform the client of the failure.
 *   <li>Redirects the client to the resulting URL using Spring Security’s redirection strategy.
 * </ul>
 *
 * <p>This approach helps protect against open redirects by limiting fallback locations and
 * sanitizes error messages to mitigate injection-like risks.
 *
 * @see SimpleUrlAuthenticationFailureHandler
 * @see CookieAuthReqRepo
 */
@Component
public class AuthFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final CookieAuthReqRepo repository;
    private final String frontendUri;
    private static final Pattern ERROR_SANITIZER = Pattern.compile("[|{}\\[\\]]+");

    /**
     * Constructs the handler with required dependencies.
     *
     * @param repository the repository for handling OAuth2 auth request cookies
     * @param frontendUri the default frontend URI to redirect to upon failure
     */
    public AuthFailureHandler(
            CookieAuthReqRepo repository,
            @Value("${app.frontendURI}") @NotNull String frontendUri) {
        this.repository = repository;
        this.frontendUri = frontendUri;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException {
        if (response.isCommitted()) {
            logger.debug("Response already committed, cannot redirect");
            return;
        }
        repository.removeAuthorizationRequestCookies(request, response);
        getRedirectStrategy()
                .sendRedirect(
                        request,
                        response,
                        appendErrorParam(
                                resolveTargetUrl(request),
                                Optional.ofNullable(request.getParameter("error"))
                                        .orElse(exception.getMessage())));
    }

    /**
     * Determines the target URL for redirection after failure. Prefers the "redirect_uri" request
     * parameter or cookie, and falls back to {@code frontendUri}.
     *
     * @param request the incoming HTTP request
     * @return the redirection URL
     */
    private String resolveTargetUrl(HttpServletRequest request) {
        return Optional.ofNullable(request.getParameter("redirect_uri"))
                .or(
                        () ->
                                CookieManager.getCookie(request, RedirectCookieName)
                                        .map(Cookie::getValue))
                .orElse(frontendUri);
    }

    /**
     * Appends a sanitized "error" query parameter to the base URL. Removes potentially harmful
     * characters from the error message.
     *
     * @param base the base redirect URL
     * @param rawError the original error message
     * @return the sanitized URL
     */
    private String appendErrorParam(String base, String rawError) {
        return UriComponentsBuilder.fromUriString(base)
                .queryParam("error", ERROR_SANITIZER.matcher(rawError).replaceAll(" ").trim())
                .build()
                .toUriString();
    }
}
