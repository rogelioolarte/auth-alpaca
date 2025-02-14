package com.example.security.oauth2;

import com.example.exception.UnauthorizedException;
import com.example.model.UserPrincipal;
import com.example.security.manager.CookieManager;
import com.example.security.manager.JJwtManager;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.Set;

import static com.example.security.oauth2.CookieAuthReqRepo.RedirectCookieName;

@Component
public class AuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JJwtManager jwtManager;
    private final CookieAuthReqRepo repository;
    private final String oauth2AuthorizedRedirectURI;

    public AuthSuccessHandler(JJwtManager jwtManager, CookieAuthReqRepo repository,
                              @Value("${app.oauth2AuthorizedRedirectURI}")
                              @NonNull String oauth2AuthorizedRedirectURI) {
        this.jwtManager = jwtManager;
        this.repository = repository;
        this.oauth2AuthorizedRedirectURI = oauth2AuthorizedRedirectURI;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String targetUrl = determineTargetUrl(request, response, authentication);
        if(response.isCommitted()) {
            logger.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }
        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {
        Optional<String> redirectURI = CookieManager.getCookie(request, RedirectCookieName)
                .map(Cookie::getValue);
        if(redirectURI.isPresent() && !isAuthorizedRedirectURI(redirectURI.get()))
            throw new UnauthorizedException("Unauthorized Redirect URI");
        return UriComponentsBuilder.fromUriString(redirectURI.orElse(getDefaultTargetUrl()))
                .queryParam("token", jwtManager.createToken(
                                (UserPrincipal) authentication.getPrincipal())).build().toUriString();
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request,
                                                 HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        repository.removeAuthorizationRequestCookies(request, response);
    }

    private boolean isAuthorizedRedirectURI(String uri) {
        URI clientRedirectURI = URI.create(uri);
        return Set.of(oauth2AuthorizedRedirectURI).stream().anyMatch(authURI -> {
            URI authorizedURI = URI. create(authURI);
            return authorizedURI.getHost().equalsIgnoreCase(clientRedirectURI.getHost()) &&
                    authorizedURI.getPort() == clientRedirectURI.getPort();
        });
    }
}
