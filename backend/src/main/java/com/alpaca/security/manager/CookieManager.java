package com.alpaca.security.manager;

import com.alpaca.security.oauth2.AuthRequestDeserializer;
import com.alpaca.security.oauth2.AuthResponseTypeDeserializer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

public class CookieManager {


    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final SimpleModule module = new SimpleModule();

    static {
        module.addDeserializer(OAuth2AuthorizationRequest.class,
                new AuthRequestDeserializer());
        module.addDeserializer(OAuth2AuthorizationResponseType.class,
                new AuthResponseTypeDeserializer());
        objectMapper.registerModule(module)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
    }


    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }
        return Optional.empty();
    }

    public static void addCookie(HttpServletResponse response, String name,
                                 String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    public static void deleteCookie(HttpServletRequest request,
                                    HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                }
            }
        }
    }

    public static String serialize(Object object) {
        try {
            return Base64.getUrlEncoder().encodeToString(objectMapper
                    .writeValueAsString(object).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Error serializing object", e);
        }
    }

    public static <T> T deserialize(Cookie cookie, Class<T> t) {
        try {
            return objectMapper.readValue(new String(Base64.getDecoder()
                    .decode(cookie.getValue()), StandardCharsets.UTF_8), t);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing cookie value", e);
        }
    }

    public static String justSerialize(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing object", e.getCause());
        }
    }

}
