package com.example.security.oauth2;

import com.example.config.AppProperties;
import com.example.security.manager.CookieManager;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

import static com.example.security.oauth2.CookieAuthReqRepo.RedirectCookieName;

@Component
@RequiredArgsConstructor
public class AuthFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final CookieAuthReqRepo repository;
    private final AppProperties appProperties;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String targetUrl = null;
        if(request.getParameter("redirect_uri") != null) {
            targetUrl = request.getParameter("redirect_uri");
        } else {
            targetUrl = CookieManager.getCookie(request, RedirectCookieName)
                    .map(Cookie::getName).orElse(appProperties.frontendURI);
        }
        if(request.getParameter("error") != null) {
            targetUrl = UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("error", request.getParameter("error")
                            .replaceAll("[|{}\\[\\]]", " "))
                    .build().toUriString();
        } else {
            targetUrl = UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("error", exception.getMessage()
                            .replaceAll("[|{}\\[\\]]", " "))
                    .build().toUriString();
        }
        repository.removeAuthorizationRequestCookies(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
