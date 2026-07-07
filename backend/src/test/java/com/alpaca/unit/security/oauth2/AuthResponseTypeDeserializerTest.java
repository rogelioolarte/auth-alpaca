package com.alpaca.unit.security.oauth2;

import com.alpaca.security.oauth2.AuthResponseTypeDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/** Unit tests for {@link AuthResponseTypeDeserializer} */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthResponseTypeDeserializer Unit Tests")
class AuthResponseTypeDeserializerTest {

    @Mock private JsonParser parser;
    @Mock private DeserializationContext ct;

    private AuthResponseTypeDeserializer deserializer;

    @BeforeEach
    void setUp() {
        deserializer = new AuthResponseTypeDeserializer();
    }

    @Test
    void deserialize_lowercaseText_shouldReturnUppercaseType() {
        when(parser.getString()).thenReturn("code");
        OAuth2AuthorizationResponseType result = deserializer.deserialize(parser, ct);
        assertNotNull(result);
        assertEquals("CODE", result.getValue());
    }

    @Test
    void deserialize_mixedCaseText_shouldNormalizeToUppercase() {
        when(parser.getString()).thenReturn("CoDe");
        OAuth2AuthorizationResponseType result = deserializer.deserialize(parser, ct);
        assertEquals("CODE", result.getValue());
    }

    @Test
    void deserialize_uppercaseText_shouldRemainUppercase() {
        when(parser.getString()).thenReturn("TOKEN");
        OAuth2AuthorizationResponseType result = deserializer.deserialize(parser, ct);
        assertEquals("TOKEN", result.getValue());
    }

    @Test
    void deserialize_nullText_shouldThrowNullPointerException() {
        when(parser.getString()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> deserializer.deserialize(parser, ct));
    }

    @Test
    void deserialize_JacksonExceptionFromParser_shouldPropagate() {
        JacksonException fakeException = new JacksonException("fail", null) {};
        when(parser.getString()).thenThrow(fakeException);
        JacksonException ex =
                assertThrows(JacksonException.class, () -> deserializer.deserialize(parser, ct));

        assertEquals("fail", ex.getOriginalMessage());
    }
}
