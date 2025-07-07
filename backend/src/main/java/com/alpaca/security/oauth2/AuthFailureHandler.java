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

@Component
public class AuthFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final CookieAuthReqRepo repository;
    private final String frontendUri;
    private static final Pattern ERROR_SANITIZER = Pattern.compile("[|{}\\[\\]]+");

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

    private String resolveTargetUrl(HttpServletRequest request) {
        return Optional.ofNullable(request.getParameter("redirect_uri"))
                .or(
                        () ->
                                CookieManager.getCookie(request, RedirectCookieName)
                                        .map(Cookie::getValue))
                .orElse(frontendUri);
    }

    private String appendErrorParam(String base, String rawError) {
        return UriComponentsBuilder.fromUriString(base)
                .queryParam("error", ERROR_SANITIZER.matcher(rawError).replaceAll(" ").trim())
                .build()
                .toUriString();
    }
}
