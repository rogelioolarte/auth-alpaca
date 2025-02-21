package com.example.security.oauth2;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class AccessTokenResConverter
        implements Converter<Map<String, Object>, OAuth2AccessTokenResponse> {

    private static final Set<String> TokenResponseParameterNames = Set.of(
            OAuth2ParameterNames.ACCESS_TOKEN,
            OAuth2ParameterNames.TOKEN_TYPE,
            OAuth2ParameterNames.EXPIRES_IN,
            OAuth2ParameterNames.REFRESH_TOKEN,
            OAuth2ParameterNames.SCOPE
    );

    @Override
    public OAuth2AccessTokenResponse convert(@NonNull Map<String, Object> source) {
        String accessToken = (String) source.get(OAuth2ParameterNames.ACCESS_TOKEN);
        String refreshToken = (String) source.get(OAuth2ParameterNames.REFRESH_TOKEN);
        OAuth2AccessToken.TokenType accessTokenType = OAuth2AccessToken.TokenType.BEARER;
        long expiresIn = 0;
        if (source.containsKey(OAuth2ParameterNames.EXPIRES_IN)) {
            expiresIn = Long.valueOf((Integer) source.get(OAuth2ParameterNames.EXPIRES_IN));
        }
        Set<String> scopes = new HashSet<>();
        if (source.containsKey(OAuth2ParameterNames.SCOPE)) {
            String scope = (String) source.get(OAuth2ParameterNames.SCOPE);
            scopes = new HashSet<>(Set.of(StringUtils.delimitedListToStringArray(scope, " ")));
        }
        Map<String, Object> additionalParameters = new LinkedHashMap<>();
        source.entrySet().stream()
                .filter(e -> !TokenResponseParameterNames
                        .contains(e.getKey()))
                .forEach(e -> additionalParameters
                        .put(e.getKey(), e.getValue()));
        return OAuth2AccessTokenResponse.withToken(accessToken).tokenType(accessTokenType)
                .expiresIn(expiresIn).scopes(scopes).additionalParameters(additionalParameters)
                .refreshToken(refreshToken).build();
    }

}
