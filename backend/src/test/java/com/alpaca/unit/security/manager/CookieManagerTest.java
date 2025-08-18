package com.alpaca.unit.security.manager;

import com.alpaca.security.manager.CookieManager;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Unit tests for {@link CookieManager} */
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
    @DisplayName("getCookie returns empty Optional when request has no cookies")
    void getCookieReturnsEmptyWhenNoCookies() {
        when(request.getCookies()).thenReturn(null);

        Optional<Cookie> result = CookieManager.getCookie(request, "anyName");
        assertTrue(result.isEmpty(), "Should return empty when getCookies() is null");
    }

    @Test
    @DisplayName("getCookie returns empty Optional when no matching cookie is found")
    void getCookieReturnsEmptyWhenNoMatch() {
        Cookie cookie1 = new Cookie("foo", "bar");
        Cookie cookie2 = new Cookie("baz", "qux");

        when(request.getCookies()).thenReturn(new Cookie[] {cookie1, cookie2});

        Optional<Cookie> result = CookieManager.getCookie(request, "doesNotExist");
        assertTrue(result.isEmpty(), "Should return empty when no cookie name matches");
    }

    @Test
    @DisplayName("getCookie returns the matching cookie if present")
    void getCookieReturnsMatchingCookie() {
        Cookie target = new Cookie("targetName", "targetValue");
        Cookie other = new Cookie("otherName", "otherValue");

        when(request.getCookies()).thenReturn(new Cookie[] {other, target});

        Optional<Cookie> result = CookieManager.getCookie(request, "targetName");
        assertTrue(result.isPresent(), "Should find the cookie with matching name");
        assertSame(target, result.get(), "Returned cookie must be the same instance");
    }

    @Test
    @DisplayName("addCookie adds a cookie with correct name, value, path, and maxAge")
    void addCookieAddsCookieWithCorrectAttributes() {
        ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);

        CookieManager.addCookie(response, "testName", "testValue", 1234);

        verify(response, times(1)).addCookie(captor.capture());
        Cookie added = captor.getValue();

        assertEquals("testName", added.getName(), "Cookie name should match");
        assertEquals("testValue", added.getValue(), "Cookie value should match");
        assertEquals("/", added.getPath(), "Cookie path should be '/'");
        assertEquals(1234, added.getMaxAge(), "Cookie maxAge should match");
    }

    @Test
    @DisplayName("deleteCookie removes the cookie by setting empty value, path '/', and maxAge 0")
    void deleteCookieRemovesCookie() {
        Cookie toDelete = new Cookie("deleteMe", "someValue");
        Cookie keep = new Cookie("keepMe", "value");

        when(request.getCookies()).thenReturn(new Cookie[] {keep, toDelete});

        ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
        CookieManager.deleteCookie(request, response, "deleteMe");

        verify(response, times(1)).addCookie(captor.capture());
        Cookie deleted = captor.getValue();

        assertEquals("deleteMe", deleted.getName(), "Deleted cookie name should match");
        assertEquals("", deleted.getValue(), "Deleted cookie value should be empty");
        assertEquals("/", deleted.getPath(), "Deleted cookie path should be '/'");
        assertEquals(0, deleted.getMaxAge(), "Deleted cookie maxAge should be 0");
    }

    @Test
    @DisplayName("deleteCookie does nothing when no matching cookie is found")
    void deleteCookieDoesNothingWhenNoMatch() {
        Cookie other = new Cookie("other", "value");

        when(request.getCookies()).thenReturn(new Cookie[] {other});

        CookieManager.deleteCookie(request, response, "nonexistent");
        verify(response, never()).addCookie(any());
    }

    @Test
    @DisplayName("deleteCookie does nothing when cookie is null")
    void deleteCookieDoesNothingWhenNull() {
        when(request.getCookies()).thenReturn(null);

        CookieManager.deleteCookie(request, response, "nonexistent");
        verify(response, never()).addCookie(any());
    }

    @Test
    @DisplayName("serialize encodes object as Base64 URL-safe JSON")
    void serializeEncodesObjectCorrectly() {
        Map<String, String> data = Map.of("key", "value");
        String encoded = CookieManager.serialize(data);

        // Decode back to JSON
        byte[] decodedBytes = Base64.getUrlDecoder().decode(encoded);
        String json = new String(decodedBytes, StandardCharsets.UTF_8);

        assertTrue(json.contains("\"key\":\"value\""), "JSON must contain key:value pair");
        assertTrue(
                json.startsWith("{") && json.endsWith("}"), "Decoded string should be valid JSON");
    }

    // Prepare a simple POJO for testing
    public static class TestPojo {
        public String foo;
        public int bar;

        // Default constructor needed for Jackson
        public TestPojo() {}
    }

    @Test
    @DisplayName("deserialize converts cookie value back to original object")
    void deserializeConvertsCookieValueToObject() {
        TestPojo original = new TestPojo();
        original.foo = "hello";
        original.bar = 42;

        // Serialize to Base64 JSON
        String json = "{\"foo\":\"hello\",\"bar\":42}";
        String base64 =
                Base64.getUrlEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

        Cookie cookie = new Cookie("myCookie", base64);
        TestPojo result = CookieManager.deserialize(cookie, TestPojo.class);

        assertNotNull(result, "Resulting object should not be null");
        assertEquals("hello", result.foo, "Field 'foo' must match original");
        assertEquals(42, result.bar, "Field 'bar' must match original");
    }

    @Test
    @DisplayName("deserialize throws RuntimeException on invalid Base64 or JSON")
    void deserializeThrowsOnInvalidValue() {
        Cookie invalid = new Cookie("bad", "not_base64!");
        assertThrows(
                RuntimeException.class, () -> CookieManager.deserialize(invalid, Object.class));
    }

    @Test
    @DisplayName("justSerialize returns raw JSON string for object")
    void justSerializeReturnsRawJson() {
        Map<String, String> data = Map.of("alpha", "beta");
        String json = CookieManager.justSerialize(data);

        assertTrue(json.contains("\"alpha\":\"beta\""), "Raw JSON should contain alpha:beta");
        assertTrue(json.startsWith("{") && json.endsWith("}"), "Output should be JSON format");
    }

    @Test
    @DisplayName("serialize throws RuntimeException when ObjectMapper fails")
    void serializeThrowsOnSerializationError() {
        Object unserializable =
                new Object() {
                    // Jackson can't serialize this
                    private final Thread thread = new Thread();
                };

        assertThrows(
                RuntimeException.class,
                () -> CookieManager.serialize(unserializable),
                "Expected RuntimeException due to serialization failure");
    }

    @Test
    @DisplayName("justSerialize throws RuntimeException when ObjectMapper fails")
    void justSerializeThrowsOnSerializationError() {
        Object unserializable =
                new Object() {
                    // Jackson can't serialize this
                    private final Thread thread = new Thread();
                };

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> CookieManager.justSerialize(unserializable),
                        "Expected RuntimeException due to serialization failure");

        assertNotNull(exception.getLocalizedMessage(), "Exception cause should not be null");
    }
}
