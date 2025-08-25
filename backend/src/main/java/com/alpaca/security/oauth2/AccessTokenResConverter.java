package com.alpaca.security.oauth2;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

/**
 * Converter that transforms a raw token response {@link Map} into a structured {@link
 * OAuth2AccessTokenResponse}.
 *
 * <p>This converter handles standard OAuth2 token response entries:
 *
 * <ul>
 *   <li><strong>access_token</strong>: required access token string.
 *   <li><strong>token_type</strong>: assigned as Bearer by default.
 *   <li><strong>expires_in</strong>: duration in seconds (parsed via {@link Number#longValue()}),
 *       with default fallback when missing.
 *   <li><strong>refresh_token</strong>: optional refresh token string.
 *   <li><strong>scope</strong>: parsed robustly from String or Collection inputs.
 * </ul>
 *
 * All other entries in the source map are treated as additional parameters and included in the
 * result as an immutable read-only map. This approach offers flexibility for OAuth2 providers
 * returning non-standard fields or when additional metadata is needed alongside the access token.
 *
 * @see OAuth2AccessTokenResponse
 * @see Converter
 */
public class AccessTokenResConverter
        implements Converter<Map<String, Object>, OAuth2AccessTokenResponse> {

    private static final Pattern SCOPE_DELIMITER = Pattern.compile("\\s+");
    private static final long DEFAULT_EXPIRES_IN = 7200L;
    private static final Set<String> TOKEN_RESPONSE_PARAMETER_NAMES =
            Set.of(
                    OAuth2ParameterNames.ACCESS_TOKEN,
                    OAuth2ParameterNames.TOKEN_TYPE,
                    OAuth2ParameterNames.EXPIRES_IN,
                    OAuth2ParameterNames.REFRESH_TOKEN,
                    OAuth2ParameterNames.SCOPE);

    @Override
    public OAuth2AccessTokenResponse convert(@NonNull Map<String, Object> source) {
        return OAuth2AccessTokenResponse.withToken(
                        (String) source.get(OAuth2ParameterNames.ACCESS_TOKEN))
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
            return Set.copyOf(scopeCol.stream().map(Object::toString).collect(Collectors.toSet()));
        }
        return Set.of();
    }

    private Map<String, Object> extractAdditionalParameters(Map<String, Object> source) {
        return Map.copyOf(
                source.entrySet().stream()
                        .filter(e -> !TOKEN_RESPONSE_PARAMETER_NAMES.contains(e.getKey()))
                        .collect(
                                Collectors.toMap(
                                        Map.Entry::getKey,
                                        Map.Entry::getValue,
                                        (a, b) -> b,
                                        LinkedHashMap::new)));
    }
}
