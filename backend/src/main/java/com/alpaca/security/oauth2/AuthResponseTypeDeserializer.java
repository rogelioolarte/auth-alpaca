package com.alpaca.security.oauth2;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType;

/**
 * Custom Jackson deserializer for OAuth2 authorization response type values.
 *
 * <p>Spring Security's {@link OAuth2AuthorizationResponseType} represents the OAuth2 response_type
 * parameter, such as "code". Since JSON may contain this value in lowercase, this deserializer
 * ensures correct construction of the enum-like object by normalizing the input to uppercase before
 * instantiation using the string constructor.
 *
 * <p>By extending {@link JsonDeserializer}, this class can be registered with Jackson’s {@code
 * ObjectMapper}, allowing Spring Boot to use it automatically when deserializing OAuth2 response
 * types from JSON.
 *
 * @see OAuth2AuthorizationResponseType
 * @see JsonDeserializer
 */
public class AuthResponseTypeDeserializer
        extends JsonDeserializer<OAuth2AuthorizationResponseType> {

    @Override
    public OAuth2AuthorizationResponseType deserialize(JsonParser p, DeserializationContext ct)
            throws IOException {
        return new OAuth2AuthorizationResponseType(p.getText().toUpperCase());
    }
}
