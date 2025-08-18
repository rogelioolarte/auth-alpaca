package com.alpaca.security.filter;

import com.alpaca.security.manager.JJwtManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A servlet filter that validates JWT tokens in incoming requests and populates the Spring Security
 * context.
 *
 * <p>Extending {@link OncePerRequestFilter}, this filter ensures that it is executed only once per
 * HTTP request, providing efficient and safe token-based authentication. This filter retrieves the
 * `Authorization` header, checks if it contains a valid Bearer token via {@link #isAToken(String)},
 * and if valid, delegates authentication to {@link JJwtManager}. A new {@link
 * UsernamePasswordAuthenticationToken} is created and set in the {@link SecurityContextHolder}.
 *
 * <p>This integration aligns with standard JWT authentication flows as seen in Spring Security
 * configurations ([OncePerRequestFilter guarantees single
 * execution](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/filter/OncePerRequestFilter.html)
 * :contentReference[oaicite:0]{index=0}).
 *
 * @see JJwtManager
 * @see OncePerRequestFilter
 */
@RequiredArgsConstructor
public class JwtTokenValidatorFilter extends OncePerRequestFilter {

    private final JJwtManager manager;

    /**
     * Filters the HTTP request to extract and validate a JWT token if present. If the token is
     * valid, sets the corresponding authentication into the security context.
     *
     * @param request the incoming HTTP request (never {@code null})
     * @param response the HTTP response (never {@code null})
     * @param filterChain the remaining filter chain (never {@code null})
     * @throws ServletException if an internal servlet error occurs
     * @throws IOException if an I/O error occurs during request handling
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String jwtToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (isAToken(jwtToken)) {
            SecurityContext context = SecurityContextHolder.getContext();
            UsernamePasswordAuthenticationToken authentication =
                    manager.manageAuthentication(jwtToken.substring(7));
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Determines whether the provided string is a valid Bearer token header. It checks if the
     * string starts with "Bearer " and exceeds a minimal plausible length.
     *
     * @param token the raw {@code Authorization} header value
     * @return {@code true} if the header looks like a valid Bearer token; {@code false} otherwise
     */
    public boolean isAToken(String token) {
        return token != null && token.startsWith("Bearer ") && token.length() > 120;
    }
}
