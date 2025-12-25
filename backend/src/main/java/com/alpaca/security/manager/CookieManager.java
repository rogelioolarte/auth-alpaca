package com.alpaca.security.manager;

import com.alpaca.security.oauth2.AuthRequestDeserializer;
import com.alpaca.security.oauth2.AuthResponseTypeDeserializer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType;

/**
 * Utility class for managing HTTP cookies and serializing/deserializing objects in a secure manner.
 *
 * <p>Supports operations to read, add, delete cookies, as well as to serialize Java objects into
 * Base64-encoded JSON strings and to deserialize them back into objects. Custom Jackson
 * deserializers are registered to handle OAuth2-specific types like {@link
 * com.alpaca.security.oauth2.AuthRequestDeserializer}.
 *
 * @see AuthRequestDeserializer
 * @see AuthResponseTypeDeserializer
 * @since 1.0
 */
public class CookieManager {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final SimpleModule module = new SimpleModule();

    static {
        module.addDeserializer(OAuth2AuthorizationRequest.class, new AuthRequestDeserializer());
        module.addDeserializer(
                OAuth2AuthorizationResponseType.class, new AuthResponseTypeDeserializer());
        objectMapper
                .registerModule(module)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
    }

    /**
     * Retrieves a cookie by name from the HTTP request.
     *
     * @param request the incoming {@link HttpServletRequest}
     * @param name the name of the cookie to retrieve
     * @return an {@link Optional} containing the cookie if found, otherwise empty
     */
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

    /**
     * Adds a new cookie with specified attributes to the HTTP response.
     *
     * @param response the {@link HttpServletResponse} to which the cookie will be added
     * @param name the name of the cookie
     * @param value the value of the cookie
     * @param maxAge maximum age in seconds for the cookie
     */
    public static void addCookie(
            HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    /**
     * Deletes a cookie by name by setting its maximum age to zero and adding it to the response.
     *
     * @param request the {@link HttpServletRequest} containing existing cookies
     * @param response the {@link HttpServletResponse} to which the deletion command is sent
     * @param name the name of the cookie to delete
     */
    public static void deleteCookie(
            HttpServletRequest request, HttpServletResponse response, String name) {
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

    /**
     * Serializes a Java object into a Base64-encoded JSON string suitable for cookie storage.
     *
     * @param object the object to serialize
     * @return a Base64 URL-safe encoded JSON string
     * @throws RuntimeException if serialization fails
     */
    public static String serialize(Object object) {
        try {
            return Base64.getUrlEncoder()
                    .encodeToString(
                            objectMapper
                                    .writeValueAsString(object)
                                    .getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Error serializing object", e);
        }
    }

    /**
     * Deserializes a cookie’s Base64-encoded JSON value into a Java object of the specified type.
     *
     * @param cookie the cookie containing the encoded value
     * @param t the target type to deserialize into
     * @param <T> the generic type parameter
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails
     */
    public static <T> T deserialize(Cookie cookie, Class<T> t) {
        try {
            return objectMapper.readValue(
                    new String(
                            Base64.getDecoder().decode(cookie.getValue()), StandardCharsets.UTF_8),
                    t);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing cookie value", e);
        }
    }

    /**
     * Serializes a Java object to a standard JSON string without Base64 encoding.
     *
     * @param object the object to serialize
     * @return the JSON string
     * @throws RuntimeException if serialization fails
     */
    public static String justSerialize(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing object", e.getCause());
        }
    }
}
