package com.alpaca.unit.security.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alpaca.security.manager.CookieManager;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("CookieManager Unit Tests")
class CookieManagerTest {

    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    @Test
    @DisplayName("getCookie: Should return empty Optional when getCookies() returns null")
    void getCookie_ShouldReturnEmpty_WhenCookiesNull() {
        when(request.getCookies()).thenReturn(null);
        String cookieName = "test-cookie";

        Optional<Cookie> result = CookieManager.getCookie(request, cookieName);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getCookie: Should return empty Optional when no cookie name matches")
    void getCookie_ShouldReturnEmpty_WhenNoMatch() {
        Cookie existingCookie = new Cookie("other", "value");
        when(request.getCookies()).thenReturn(new Cookie[] {existingCookie});
        String targetName = "target";

        Optional<Cookie> result = CookieManager.getCookie(request, targetName);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getCookie: Should return the correct cookie when name matches")
    void getCookie_ShouldReturnCookie_WhenMatchFound() {
        String targetName = "auth-token";
        Cookie targetCookie = new Cookie(targetName, "secret-payload");
        Cookie otherCookie = new Cookie("session", "123");
        when(request.getCookies()).thenReturn(new Cookie[] {otherCookie, targetCookie});

        Optional<Cookie> result = CookieManager.getCookie(request, targetName);

        assertTrue(result.isPresent());
        assertEquals(targetCookie.getValue(), result.get().getValue());
    }

    @Test
    @DisplayName("addCookie: Should configure and add cookie to response correctly")
    void addCookie_ShouldSetAttributesAndAdd() {
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        String name = "pref";
        String value = "dark-mode";
        int maxAge = 3600;

        CookieManager.addCookie(response, name, value, maxAge);

        verify(response, times(1)).addCookie(cookieCaptor.capture());
        Cookie captured = cookieCaptor.getValue();
        assertEquals(name, captured.getName());
        assertEquals(value, captured.getValue());
        assertEquals("/", captured.getPath());
        assertEquals(maxAge, captured.getMaxAge());
    }

    @Test
    @DisplayName("deleteCookie: Should set maxAge to 0 and empty value for matching cookie")
    void deleteCookie_ShouldExpireCookie_WhenFound() {
        String nameToDelete = "old-session";
        Cookie target = new Cookie(nameToDelete, "data");
        when(request.getCookies()).thenReturn(new Cookie[] {target});
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);

        CookieManager.deleteCookie(request, response, nameToDelete);

        verify(response, times(1)).addCookie(cookieCaptor.capture());
        Cookie result = cookieCaptor.getValue();
        assertEquals("", result.getValue());
        assertEquals(0, result.getMaxAge());
        assertEquals("/", result.getPath());
    }

    @Test
    @DisplayName(
            "deleteCookie: Should take no action if cookie is not present or request cookies are"
                    + " null")
    void deleteCookie_ShouldDoNothing_WhenNotFound() {
        when(request.getCookies()).thenReturn(null);
        CookieManager.deleteCookie(request, response, "any");

        when(request.getCookies()).thenReturn(new Cookie[] {new Cookie("different", "val")});
        CookieManager.deleteCookie(request, response, "target");

        verify(response, never()).addCookie(any(Cookie.class));
    }

    @Test
    @DisplayName("serialize: Should return Base64 URL encoded JSON")
    void serialize_ShouldReturnBase64UrlJson() {
        Map<String, String> payload = Map.of("id", "alpaca-01");
        String result = CookieManager.serialize(payload);

        assertNotNull(result);
        byte[] decoded = Base64.getUrlDecoder().decode(result);
        String json = new String(decoded, StandardCharsets.UTF_8);
        assertTrue(json.contains("alpaca-01"));
    }

    @Test
    @DisplayName("deserialize: Should convert Base64 cookie value back to object")
    void deserialize_ShouldReturnObjectFromCookie() {
        String json = "{\"id\":\"alpaca-01\"}";
        String base64Value =
                Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        Cookie cookie = new Cookie("data", base64Value);

        Map<?, ?> result = CookieManager.deserialize(cookie, Map.class);

        assertEquals("alpaca-01", result.get("id"));
    }

    @Test
    @DisplayName("justSerialize: Should return raw JSON string")
    void justSerialize_ShouldReturnRawJson() {
        Map<String, Integer> payload = Map.of("code", 200);
        String result = CookieManager.justSerialize(payload);

        assertEquals("{\"code\":200}", result);
    }

    @Test
    @DisplayName("Serialization Errors: Should wrap Jackson exceptions in RuntimeException")
    void serialization_ShouldThrowRuntimeException_OnFailure() {
        Object circularReference =
                new Object() {
                    public Object getSelf() {
                        return this;
                    }
                };

        assertThrows(RuntimeException.class, () -> CookieManager.serialize(circularReference));
        assertThrows(RuntimeException.class, () -> CookieManager.justSerialize(circularReference));
    }

    @Test
    @DisplayName("deserialize Error: Should throw RuntimeException on invalid data")
    void deserialize_ShouldThrowRuntimeException_OnInvalidData() {
        Cookie badCookie = new Cookie("bad", "not-json-at-all");
        assertThrows(RuntimeException.class, () -> CookieManager.deserialize(badCookie, Map.class));
    }
}
