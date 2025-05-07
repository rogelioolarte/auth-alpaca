package com.alpaca.unit.security.oauth2;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.security.oauth2.AuthRequestDeserializer;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

@DisplayName("AuthRequestDeserializer Unit Tests")
class AuthRequestDeserializerTest {

  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    SimpleModule module = new SimpleModule();
    module.addDeserializer(OAuth2AuthorizationRequest.class, new AuthRequestDeserializer());
    mapper = new ObjectMapper().registerModule(module);
  }

  @Nested
  @DisplayName("deserialize()")
  class DeserializeTests {

    @Test
    @DisplayName("Given valid full JSON, parses all fields correctly")
    void givenValidJson_parsesAllFields() throws Exception {
      String json =
          """
          {
            "clientId":"my-client",
            "authorizationUri":"https://auth.example.com",
            "redirectUri":"https://app.example.com/callback",
            "state":"xyz-state",
            "scopes":["read","write"],
            "attributes":{"roles": ["user","admin"]},
            "additionalParameters":{"baz":[123, 124]}
          }
          """;
      OAuth2AuthorizationRequest req = mapper.readValue(json, OAuth2AuthorizationRequest.class);
      assertAll(
          "full-object",
          () -> assertEquals("my-client", req.getClientId()),
          () -> assertEquals("https://auth.example.com", req.getAuthorizationUri()),
          () -> assertEquals("https://app.example.com/callback", req.getRedirectUri()),
          () -> assertEquals("xyz-state", req.getState()),
          () -> assertEquals(Set.of("read", "write"), req.getScopes()),
          () -> assertEquals(Map.of("roles", List.of("user", "admin")), req.getAttributes()),
          () -> assertEquals(Map.of("baz", List.of(123, 124)), req.getAdditionalParameters()));
    }

    @Test
    @DisplayName("Missing required field triggers JsonMappingException")
    void missingRequiredField_throws() {
      String json =
          """
          {
            "clientId":"c1",
            "authorizationUri":"u1",
            "redirectUri":"r1"
          }
          """;
      JsonMappingException ex =
          assertThrows(
              JsonMappingException.class,
              () -> mapper.readValue(json, OAuth2AuthorizationRequest.class));
      assertTrue(ex.getOriginalMessage().contains("Missing required field 'state'"));
    }

    @Test
    @DisplayName("Empty or missing scopes yields empty set")
    void missingScopes_producesEmptyScopes() throws Exception {
      String jsonWithoutScopes =
          """
          {
            "clientId":"c1","authorizationUri":"u1",
            "redirectUri":"r1","state":"s1"
          }
          """;
      OAuth2AuthorizationRequest r1 =
          mapper.readValue(jsonWithoutScopes, OAuth2AuthorizationRequest.class);
      assertTrue(r1.getScopes().isEmpty());
      String jsonEmptyScopes =
          """
          {
            "clientId":"c1","authorizationUri":"u1",
            "redirectUri":"r1","state":"s1",
            "scopes":[]
          }
          """;
      OAuth2AuthorizationRequest r2 =
          mapper.readValue(jsonEmptyScopes, OAuth2AuthorizationRequest.class);
      assertTrue(r2.getScopes().isEmpty());
    }

    @Test
    @DisplayName("Unknown fields are ignored")
    void unknownFields_areIgnored() throws Exception {
      String json =
          """
          {
            "clientId":"c1","authorizationUri":"u1",
            "redirectUri":"r1","state":"s1",
            "fooBar":123,
            "scopes":["a"]
          }
          """;
      OAuth2AuthorizationRequest req = mapper.readValue(json, OAuth2AuthorizationRequest.class);
      assertEquals("c1", req.getClientId());
      assertEquals(Set.of("a"), req.getScopes());
    }

    @Test
    @DisplayName("Non‑array scopes node is skipped")
    void nonArrayScopes_skipped() throws Exception {
      String json =
          """
          {
            "clientId":"c1","authorizationUri":"u1",
            "redirectUri":"r1","state":"s1",
            "scopes":12345
          }
          """;
      OAuth2AuthorizationRequest req = mapper.readValue(json, OAuth2AuthorizationRequest.class);
      assertTrue(req.getScopes().isEmpty());
    }

    @Test
    @DisplayName("Given scopes array with only non‑string elements, returns empty set")
    void arrayWithNonStringScopes_skipped() throws Exception {
      String json =
          """
          {
            "clientId":"c1",
            "authorizationUri":"u1",
            "redirectUri":"r1",
            "state":"s1",
            "scopes":[1, true, {"foo":"bar"}]
          }
          """;

      OAuth2AuthorizationRequest req = mapper.readValue(json, OAuth2AuthorizationRequest.class);
      assertTrue(req.getScopes().isEmpty(), "Non‑string elements in scopes array should be ignored");
    }

    @Test
    @DisplayName("Given advances null currentToken to START_OBJECT")
    void deserialize_directly_advancesParserFromNull() throws Exception {
      String json =
          """
          { "clientId":"c","authorizationUri":"u","redirectUri":"r","state":"s" }
          """;
      JsonFactory factory = new JsonFactory();
      try (JsonParser parser = factory.createParser(json)) {
        assertNull(parser.currentToken(), "Must start with null token");
        AuthRequestDeserializer desert = new AuthRequestDeserializer();
        OAuth2AuthorizationRequest req = desert.deserialize(parser, null);
        assertEquals("c", req.getClientId());
      }
    }

    @Test
    @DisplayName("Given a non‑object JSON, throws JsonMappingException")
    void nonObjectJson_throwsJsonMappingException() {
      String json =
          """
              [ "not", "an", "object" ]
          """;
      JsonMappingException ex =
          assertThrows(
              JsonMappingException.class,
              () -> mapper.readValue(json, OAuth2AuthorizationRequest.class));
      assertTrue(
          ex.getOriginalMessage().contains("Expected JSON object for OAuth2AuthorizationRequest"),
          "Should enforce START_OBJECT at the root");
    }
  }

  @Test
  @DisplayName("handledType() returns OAuth2AuthorizationRequest.class")
  void handledType() {
    AuthRequestDeserializer desert = new AuthRequestDeserializer();
    assertEquals(OAuth2AuthorizationRequest.class, desert.handledType());
  }
}
