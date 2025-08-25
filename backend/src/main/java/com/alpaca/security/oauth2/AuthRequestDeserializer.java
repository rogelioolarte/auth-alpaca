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

/**
 * Custom Jackson deserializer for {@link OAuth2AuthorizationRequest}, enabling reconstruction of
 * authorization requests from JSON data. This is particularly useful for restoring saved OAuth2
 * Authorization Requests that were serialized into JSON (e.g., stored in cookies or other stateless
 * media).
 *
 * <p>The deserializer reads a JSON object, extracts essential fields (clientId, authorizationUri,
 * redirectUri, state), and also handles optional arrays (scopes) and maps (attributes,
 * additionalParameters). It validates that required fields are present, and throws a {@link
 * JsonMappingException} if any are missing.
 *
 * <p>By extending {@link StdDeserializer}, this class integrates smoothly with Jackson's
 * deserialization pipeline. It meticulously reconstructs a fully functional {@link
 * OAuth2AuthorizationRequest} by calling its builder.
 *
 * <p>Intended for use when the default storage (such as HTTP session) is replaced with a stateless
 * mechanism (for example, storing requests in cookies), and you need to deserialize requests back
 * into usable objects for the OAuth2 authorization flow.
 *
 * @see StdDeserializer
 * @see OAuth2AuthorizationRequest
 */
public class AuthRequestDeserializer extends StdDeserializer<OAuth2AuthorizationRequest> {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final ObjectReader MAP_READER = new ObjectMapper().readerFor(MAP_TYPE);

    /** Default constructor, informing Jackson of the handled type. */
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

    /**
     * Validates presence of required fields.
     *
     * @param value the value to check
     * @param name field name
     * @param p current JsonParser instance
     * @return the value if valid
     * @throws JsonMappingException if value is null
     */
    private static String requireField(String value, String name, JsonParser p)
            throws JsonMappingException {
        if (value == null) {
            throw JsonMappingException.from(p, "Missing required field '" + name + "'");
        }
        return value;
    }

    /**
     * Parses the JSON array of scopes into a {@link Set<String>}. Skips non-string entries and
     * returns an immutable set.
     *
     * @param p current JsonParser
     * @return a Set of scope strings
     * @throws IOException on parse error
     */
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
