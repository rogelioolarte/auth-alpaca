package com.alpaca.security.oauth2;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

public class AuthRequestDeserializer extends StdDeserializer<OAuth2AuthorizationRequest> {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final ObjectReader MAP_READER = new ObjectMapper().readerFor(MAP_TYPE);

    public AuthRequestDeserializer() {
        super(OAuth2AuthorizationRequest.class);
    }

    @Override
    public OAuth2AuthorizationRequest deserialize(JsonParser p, DeserializationContext ct)
            throws IOException {

        if (p.currentToken() == null) {
            p.nextToken();
        }
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw JsonMappingException.from(
                    p, "Expected JSON object for OAuth2AuthorizationRequest");
        }

        String clientId = null, authorizationUri = null, redirectUri = null, state = null;
        Set<String> scopes = Collections.emptySet();
        Map<String, Object> attributes = Collections.emptyMap();
        Map<String, Object> additionalParameters = Collections.emptyMap();

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String field = p.currentName();
            p.nextToken();
            switch (field) {
                case "clientId" -> clientId = p.getValueAsString();
                case "authorizationUri" -> authorizationUri = p.getValueAsString();
                case "redirectUri" -> redirectUri = p.getValueAsString();
                case "state" -> state = p.getValueAsString();
                case "scopes" -> scopes = parseScopes(p);
                case "attributes" -> attributes = MAP_READER.readValue(p);
                case "additionalParameters" -> additionalParameters = MAP_READER.readValue(p);
                default -> p.skipChildren();
            }
        }

        clientId = requireField(clientId, "clientId", p);
        authorizationUri = requireField(authorizationUri, "authorizationUri", p);
        redirectUri = requireField(redirectUri, "redirectUri", p);
        state = requireField(state, "state", p);

        return OAuth2AuthorizationRequest.authorizationCode()
                .clientId(clientId)
                .authorizationUri(authorizationUri)
                .redirectUri(redirectUri)
                .scopes(scopes)
                .state(state)
                .attributes(attributes)
                .additionalParameters(additionalParameters)
                .build();
    }

    /** Ensures a required string field was present and not null. */
    private static String requireField(String value, String name, JsonParser p)
            throws JsonMappingException {
        if (value == null) {
            throw JsonMappingException.from(p, "Missing required field '" + name + "'");
        }
        return value;
    }

    /** Parses the 'scopes' JSON array into an immutable Set<String>. */
    private static Set<String> parseScopes(JsonParser p) throws IOException {
        if (p.currentToken() != JsonToken.START_ARRAY) {
            p.skipChildren();
            return Collections.emptySet();
        }

        Set<String> temp = new LinkedHashSet<>();
        while (p.nextToken() != JsonToken.END_ARRAY) {
            if (p.currentToken() == JsonToken.VALUE_STRING) {
                temp.add(p.getText());
            } else {
                p.skipChildren();
            }
        }
        return Set.copyOf(temp);
    }

    @Override
    public Class<OAuth2AuthorizationRequest> handledType() {
        return OAuth2AuthorizationRequest.class;
    }
}
