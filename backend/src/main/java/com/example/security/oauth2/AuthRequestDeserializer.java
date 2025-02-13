package com.example.security.oauth2;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class AuthRequestDeserializer extends
        JsonDeserializer<OAuth2AuthorizationRequest> {

    @Override
    public OAuth2AuthorizationRequest deserialize(JsonParser p, DeserializationContext ct)
            throws IOException {
        Map<String, Object> data = p.readValueAs(Map.class);
        return OAuth2AuthorizationRequest.authorizationCode()
                .clientId((String) data.get("clientId"))
                .authorizationUri((String) data.get("authorizationUri"))
                .redirectUri((String) data.get("redirectUri"))
                .scopes(safeCast(data.get("scopes"), Set.class, Set.of()))
                .state((String) data.get("state"))
                .attributes(safeCast(data.get("attributes"), Map.class, Map.of()))
                .additionalParameters(safeCast(data
                        .get("additionalParameters"), Map.class, Map.of()))
                .build();
    }

    private static <T> T safeCast(Object obj, Class<T> clazz, T other) {
        if (clazz.isInstance(obj)) {
            return (T) obj;
        }
        return other;
    }


}
