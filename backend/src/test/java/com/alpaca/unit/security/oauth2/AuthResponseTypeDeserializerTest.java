package com.alpaca.unit.security.oauth2;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alpaca.security.oauth2.AuthResponseTypeDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType;

@ExtendWith(MockitoExtension.class)
class AuthResponseTypeDeserializerTest {

  @Mock private JsonParser parser;
  @Mock private DeserializationContext ct;

  private AuthResponseTypeDeserializer deserializer;

  @BeforeEach
  void setUp() {
    deserializer = new AuthResponseTypeDeserializer();
  }

  @Test
  void deserialize_lowercaseText_shouldReturnUppercaseType() throws IOException {
    when(parser.getText()).thenReturn("code");
    OAuth2AuthorizationResponseType result = deserializer.deserialize(parser, ct);
    assertNotNull(result);
    assertEquals("CODE", result.getValue());
  }

  @Test
  void deserialize_mixedCaseText_shouldNormalizeToUppercase() throws IOException {
    when(parser.getText()).thenReturn("CoDe");
    OAuth2AuthorizationResponseType result = deserializer.deserialize(parser, ct);
    assertEquals("CODE", result.getValue());
  }

  @Test
  void deserialize_uppercaseText_shouldRemainUppercase() throws IOException {
    when(parser.getText()).thenReturn("TOKEN");
    OAuth2AuthorizationResponseType result = deserializer.deserialize(parser, ct);
    assertEquals("TOKEN", result.getValue());
  }

  @Test
  void deserialize_nullText_shouldThrowNullPointerException() throws IOException {
    when(parser.getText()).thenReturn(null);
    assertThrows(NullPointerException.class, () -> deserializer.deserialize(parser, ct));
  }

  @Test
  void deserialize_ioExceptionFromParser_shouldPropagate() throws IOException {
    when(parser.getText()).thenThrow(new IOException("fail"));
    IOException ex = assertThrows(IOException.class, () -> deserializer.deserialize(parser, ct));
    assertEquals("fail", ex.getMessage());
  }
}
