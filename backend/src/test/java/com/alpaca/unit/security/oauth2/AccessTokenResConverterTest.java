package com.alpaca.unit.security.oauth2;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.security.oauth2.AccessTokenResConverter;
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
  void convert_WhenExpiresMissing_ShouldUseDefault() throws Exception {
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
}
