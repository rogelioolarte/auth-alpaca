package com.alpaca.unit.security.oauth2;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.security.oauth2.AccessTokenResConverter;
import java.lang.reflect.Method;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

/** Unit tests for {@link AccessTokenResConverter} */
@DisplayName("AccessTokenResConverter Unit Tests")
class AccessTokenResConverterTest {

    private AccessTokenResConverter converter;

    private static final String accessToken = "token123";
    private static final String refreshToken = "refresh456";
    private static final Long expiresIn = 1800L;
    private static final String scope = "read write";
    private static final Set<String> scopes = Set.of("read", "write");
    private static final long DEFAULT_EXPIRES_IN = 7200L;

    @BeforeEach
    void setUp() {
        converter = new AccessTokenResConverter();
    }

    @Test
    void convert_WithAllParameters_ShouldMapCorrectly() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put(OAuth2ParameterNames.ACCESS_TOKEN, accessToken);
        source.put(OAuth2ParameterNames.REFRESH_TOKEN, refreshToken);
        source.put(OAuth2ParameterNames.EXPIRES_IN, expiresIn);
        source.put(OAuth2ParameterNames.SCOPE, scope);
        source.put("extra", "value");
        OAuth2AccessTokenResponse response = converter.convert(source);
        assertNotNull(response);
        assertEquals(accessToken, response.getAccessToken().getTokenValue());
        assertEquals(OAuth2AccessToken.TokenType.BEARER, response.getAccessToken().getTokenType());
        assertNotNull(response.getAccessToken().getIssuedAt());
        assertNotNull(response.getAccessToken().getExpiresAt());
        assertEquals(
                expiresIn,
                response.getAccessToken().getExpiresAt().getEpochSecond()
                        - response.getAccessToken().getIssuedAt().getEpochSecond());
        assertEquals(scopes, response.getAccessToken().getScopes());
        assertNotNull(response.getRefreshToken());
        assertEquals(refreshToken, response.getRefreshToken().getTokenValue());
        assertEquals(Map.of("extra", "value"), response.getAdditionalParameters());
    }

    @Test
    void convert_WhenExpiresMissing_ShouldUseDefault() {
        Map<String, Object> source = new HashMap<>();
        source.put(OAuth2ParameterNames.ACCESS_TOKEN, "tokenABC");
        source.put(OAuth2ParameterNames.SCOPE, "scope1");
        OAuth2AccessTokenResponse response = converter.convert(source);
        assertNotNull(response);
        assertNotNull(response.getAccessToken().getExpiresAt());
        assertNotNull(response.getAccessToken().getIssuedAt());
        assertEquals(
                DEFAULT_EXPIRES_IN,
                response.getAccessToken().getExpiresAt().getEpochSecond()
                        - response.getAccessToken().getIssuedAt().getEpochSecond());
    }

    @Test
    void convert_WithScopeCollection_ShouldParseEachElement() {
        Map<String, Object> source = new HashMap<>();
        source.put(OAuth2ParameterNames.ACCESS_TOKEN, "tok");
        source.put(OAuth2ParameterNames.EXPIRES_IN, 100L);
        source.put(OAuth2ParameterNames.SCOPE, List.of("a", "b", 123));
        OAuth2AccessTokenResponse response = converter.convert(source);
        assertNotNull(response);
        assertEquals(Set.of("a", "b", "123"), response.getAccessToken().getScopes());
    }

    @Test
    void convert_WhenNoAdditionalParameters_ShouldReturnEmptyMap() {
        Map<String, Object> source =
                Map.of(OAuth2ParameterNames.ACCESS_TOKEN, "t", OAuth2ParameterNames.EXPIRES_IN, 10);
        OAuth2AccessTokenResponse response = converter.convert(source);
        assertNotNull(response);
        assertTrue(response.getAdditionalParameters().isEmpty());
    }

    @Test
    @DisplayName("convert_WithMultipleAdditionalParameters")
    void convert_WithMultipleAdditionalParameters_PreservesAllInOrder() {
        // Prepare the source with LinkedHashMap to control the order
        Map<String, Object> source = new LinkedHashMap<>();
        source.put(OAuth2ParameterNames.ACCESS_TOKEN, accessToken);
        source.put("first_extra", "value1");
        source.put(OAuth2ParameterNames.SCOPE, scope); // standard, it is filtered
        source.put("second_extra", 123);
        source.put("third_extra", List.of("a", "b"));
        source.put("fourth_extra", true);
        // standard expires_in parameter: does not enter additional
        source.put(OAuth2ParameterNames.EXPIRES_IN, expiresIn);

        OAuth2AccessTokenResponse response = converter.convert(source);
        assertNotNull(response);
        Map<String, Object> actualExtras = response.getAdditionalParameters();

        Map<String, Object> expectedExtras = new LinkedHashMap<>();
        expectedExtras.put("first_extra", "value1");
        expectedExtras.put("second_extra", 123);
        expectedExtras.put("third_extra", List.of("a", "b"));
        expectedExtras.put("fourth_extra", true);

        assertEquals(expectedExtras, actualExtras);
    }

    @Test
    @DisplayName("extractAdditionalParameters merges duplicate keys using last-value-wins")
    void extractAdditionalParameters_mergeBehavior() throws Exception {
        AccessTokenResConverter converter = new AccessTokenResConverter();

        // We create a test Map where entrySet() returns duplicates
        Map<String, Object> fakeSource =
                new AbstractMap<>() {
                    private final List<Entry<String, Object>> entries =
                            List.of(
                                    Map.entry("dup", "first"),
                                    Map.entry("dup", "second"),
                                    Map.entry("other", 42));

                    @Override
                    public Set<Entry<String, Object>> entrySet() {
                        // LinkedHashSet to preserve insertion order in the test
                        return new LinkedHashSet<>(entries);
                    }

                    @Override
                    public Object get(Object key) {
                        // Just to fulfill Map contract, we don't use get() here
                        return null;
                    }
                };

        // We invoke the private method by reflection
        Method extract =
                AccessTokenResConverter.class.getDeclaredMethod(
                        "extractAdditionalParameters", Map.class);
        extract.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) extract.invoke(converter, fakeSource);

        // You should merge "dup" with the second value ("second") and keep "other"
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("dup", "second");
        expected.put("other", 42);

        assertEquals(expected, result);
    }
}
