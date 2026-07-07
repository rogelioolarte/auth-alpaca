package com.alpaca.security.oauth2;

import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Custom Jackson deserializer for {@link OAuth2AuthorizationRequest}, enabling reconstruction of
 * authorization requests from JSON data. This is particularly useful for restoring saved OAuth2
 * Authorization Requests that were serialized into JSON (e.g., stored in cookies or other stateless
 * media).
 *
 * <p>The deserializer reads a JSON object, extracts essential fields (clientId, authorizationUri,
 * redirectUri, state), and also handles optional arrays (scopes) and maps (attributes,
 * additionalParameters). It validates that required fields are present, and throws a {@link
 * JacksonException} if any are missing.
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

    private static final JsonMapper OBJECT_MAPPER = new JsonMapper();
    private static final JavaType MAP_JAVA_TYPE =
            OBJECT_MAPPER.getTypeFactory().constructMapType(Map.class, String.class, Object.class);
    private static final JavaType OA2R_MAPPER =
            OBJECT_MAPPER.getTypeFactory().constructType(OAuth2AuthorizationRequest.class);

    /** Default constructor, informing Jackson of the handled type. */
    public AuthRequestDeserializer() {
        super(OAuth2AuthorizationRequest.class);
    }

    @Override
    public OAuth2AuthorizationRequest deserialize(JsonParser p, DeserializationContext ct)
            throws JacksonException {

        if (p.currentToken() == null) {
            p.nextToken();
        }
        if (p.currentToken() != JsonToken.START_OBJECT) {
            return (OAuth2AuthorizationRequest)
                    ct.handleUnexpectedToken(
                            OA2R_MAPPER,
                            JsonToken.START_OBJECT,
                            p,
                            "Expected JSON object for OAuth2AuthorizationRequest");
        }

        String clientId = null;
        String authorizationUri = null;
        String redirectUri = null;
        String state = null;
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
                case "attributes" -> attributes = ct.readValue(p, MAP_JAVA_TYPE);
                case "additionalParameters" ->
                        additionalParameters = ct.readValue(p, MAP_JAVA_TYPE);
                default -> p.skipChildren();
            }
        }

        clientId = requireField(clientId, "clientId", p, ct);
        authorizationUri = requireField(authorizationUri, "authorizationUri", p, ct);
        redirectUri = requireField(redirectUri, "redirectUri", p, ct);
        state = requireField(state, "state", p, ct);

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
     * @throws JacksonException if value is null
     */
    private static String requireField(
            String value, String name, JsonParser p, DeserializationContext ct)
            throws JacksonException {
        if (value == null) {
            return (String)
                    ct.handleUnexpectedToken(
                            OA2R_MAPPER,
                            JsonToken.NOT_AVAILABLE,
                            p,
                            "Missing required field %s",
                            name);
        }
        return value;
    }

    /**
     * Parses the JSON array of scopes into a {@link Set<String>}. Skips non-string entries and
     * returns an immutable set.
     *
     * @param p current JsonParser
     * @return a Set of scope strings
     */
    private static Set<String> parseScopes(JsonParser p) {
        if (p.currentToken() != JsonToken.START_ARRAY) {
            p.skipChildren();
            return Collections.emptySet();
        }

        Set<String> temp = new LinkedHashSet<>();
        while (p.nextToken() != JsonToken.END_ARRAY) {
            if (p.currentToken() == JsonToken.VALUE_STRING) {
                temp.add(p.getString());
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
