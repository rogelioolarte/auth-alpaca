package com.alpaca.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A simple CORS (Cross-Origin Resource Sharing) filter that enables broad access to the API from
 * any origin. Applied at the highest precedence to ensure CORS headers are set before all other
 * filters, including security filters.
 *
 * <p>This filter extends {@link OncePerRequestFilter} so that its logic executes only once per HTTP
 * request dispatch, avoiding redundant header additions or unexpected behavior.
 *
 * @see OncePerRequestFilter
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SimpleCORSFilter extends OncePerRequestFilter {

    /**
     * Adds standard CORS headers to each HTTP response to allow cross-origin access. This includes
     * allowed origins, credentials support, accepted methods, and headers. Then continues filter
     * chain execution.
     *
     * @param request the incoming HTTP request (never {@code null})
     * @param response the HTTP response to which CORS headers will be applied (never {@code null})
     * @param filterChain the filter chain to delegate to after adding headers (never {@code null})
     * @throws ServletException if an internal servlet error occurs
     * @throws IOException if an I/O error occurs during request processing
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, OPTIONS, DELETE");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader(
                "Access-Control-Allow-Headers",
                "Content-Type, Accept, X-Requested-With, remember-me, Authorization");

        filterChain.doFilter(request, response);
    }
}
