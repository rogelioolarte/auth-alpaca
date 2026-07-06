package com.alpaca.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Static utility methods shared across the application.
 *
 * <p>This class is not meant to be instantiated — it exists purely to group stateless helper
 * functions that do not fit naturally into any single domain component.
 */
public class Utils {

    private Utils() {}

    /**
     * Extracts the originating client IP address from an HTTP request.
     *
     * <p>If the request passed through one or more proxies, the {@code X-Forwarded-For} header is
     * checked first and the leftmost (original client) address is returned. Otherwise falls back to
     * {@link HttpServletRequest#getRemoteAddr()}.
     *
     * @param request the incoming HTTP request
     * @return the client IP address as a string
     */
    public static String extractClientIP(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
