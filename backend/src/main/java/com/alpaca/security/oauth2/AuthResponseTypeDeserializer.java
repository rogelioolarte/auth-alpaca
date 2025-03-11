package com.alpaca.security.oauth2;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType;

import java.io.IOException;

public class AuthResponseTypeDeserializer extends JsonDeserializer<OAuth2AuthorizationResponseType> {
    @Override
    public OAuth2AuthorizationResponseType deserialize(JsonParser p, DeserializationContext ct)
            throws IOException {
        return new OAuth2AuthorizationResponseType(p.getText().toUpperCase());
    }
}
