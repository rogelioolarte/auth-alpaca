package com.alpaca.security.oauth2;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.StringUtils;

/**
 * Converter that transforms a raw token response map into a structured {@link OAuth2AccessTokenResponse}.
 * <p>
 * This converter provides robust parsing and handling of standard OAuth2 token response parameters:
 * <ul>
 *   <li><strong>access_token</strong>: the issued access token (String).</li>
 *   <li><strong>token_type</strong>: the token type (Bearer is assumed).</li>
 *   <li><strong>expires_in</strong>: lifetime of the access token in seconds (Number).</li>
 *   <li><strong>refresh_token</strong>: the issued refresh token (String), if present.</li>
 *   <li><strong>scope</strong>: one or more scope values, either as a space-delimited String or a Collection.</li>
 * </ul>
 * Any additional parameters returned by the provider are captured as a read-only map of {@code additionalParameters}.
 * <p>
 * Usage note:
 * <ul>
 *   <li>Numeric values for {@code expires_in} are handled generically via {@link Number#longValue()}</li>
 *   <li>Scope values are parsed flexibly from both String and Collection types</li>
 *   <li>Resulting collections are immutable to prevent accidental modification.</li>
 * </ul>
 */
public class AccessTokenResConverter
    implements Converter<Map<String, Object>, OAuth2AccessTokenResponse> {

  private static final Pattern SCOPE_DELIMITER = Pattern.compile("\\s+");
  private static final long DEFAULT_EXPIRES_IN = 7200L;
  private static final Set<String> TOKEN_RESPONSE_PARAMETER_NAMES = Set.of(
      OAuth2ParameterNames.ACCESS_TOKEN,
      OAuth2ParameterNames.TOKEN_TYPE,
      OAuth2ParameterNames.EXPIRES_IN,
      OAuth2ParameterNames.REFRESH_TOKEN,
      OAuth2ParameterNames.SCOPE
  );

  @Override
  public OAuth2AccessTokenResponse convert(@NonNull Map<String, Object> source) {
    return OAuth2AccessTokenResponse.withToken((String) source.get(OAuth2ParameterNames.ACCESS_TOKEN))
        .tokenType(OAuth2AccessToken.TokenType.BEARER)
        .expiresIn(parseExpiresIn(source))
        .scopes(parseScopes(source))
        .additionalParameters(extractAdditionalParameters(source))
        .refreshToken((String) source.get(OAuth2ParameterNames.REFRESH_TOKEN))
        .build();
  }

  private long parseExpiresIn(Map<String, Object> source) {
    var expiresInObj = source.get(OAuth2ParameterNames.EXPIRES_IN);
    if (expiresInObj instanceof Number number) {
      return number.longValue();
    }
    return DEFAULT_EXPIRES_IN;
  }

  private Set<String> parseScopes(Map<String, Object> source) {
    var scopeObj = source.get(OAuth2ParameterNames.SCOPE);
    if (scopeObj instanceof String scopeStr) {
      return Set.copyOf(Set.of(SCOPE_DELIMITER.split(scopeStr.trim())));
    }
    if (scopeObj instanceof Collection<?> scopeCol) {
      return Set.copyOf(
          scopeCol.stream()
              .map(Object::toString)
              .collect(Collectors.toSet())
      );
    }
    return Set.of();
  }

  private Map<String, Object> extractAdditionalParameters(Map<String, Object> source) {
    return Map.copyOf(
        source.entrySet().stream()
            .filter(e -> !TOKEN_RESPONSE_PARAMETER_NAMES.contains(e.getKey()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> b,
                LinkedHashMap::new
            ))
    );
  }
}