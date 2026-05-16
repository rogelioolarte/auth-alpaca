package com.alpaca.utils;

import jakarta.servlet.http.HttpServletRequest;

public class Utils {

    private Utils() {}

    public static String extractClientIP(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
