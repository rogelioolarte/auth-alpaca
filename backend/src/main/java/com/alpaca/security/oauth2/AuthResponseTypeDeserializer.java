package com.alpaca.security.oauth2;

import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.json.JsonMapper;

/**
 * Custom Jackson deserializer for OAuth2 authorization response type values.
 *
 * <p>Spring Security's {@link OAuth2AuthorizationResponseType} represents the OAuth2 response_type
 * parameter, such as "code". Since JSON may contain this value in lowercase, this deserializer
 * ensures correct construction of the enum-like object by normalizing the input to uppercase before
 * instantiation using the string constructor.
 *
 * <p>By extending {@link ValueDeserializer}, this class can be registered with Jackson’s {@link
 * JsonMapper}, allowing Spring Boot to use it automatically when deserializing OAuth2 response
 * types from JSON.
 *
 * @see OAuth2AuthorizationResponseType
 * @see ValueDeserializer
 */
public class AuthResponseTypeDeserializer
        extends ValueDeserializer<OAuth2AuthorizationResponseType> {

    @Override
    public OAuth2AuthorizationResponseType deserialize(JsonParser p, DeserializationContext context)
            throws JacksonException {
        return new OAuth2AuthorizationResponseType(p.getString().toUpperCase());
    }
}
