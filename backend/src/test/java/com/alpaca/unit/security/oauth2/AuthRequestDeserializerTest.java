package com.alpaca.unit.security.oauth2;

import com.alpaca.security.oauth2.AuthRequestDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Unit tests for {@link AuthRequestDeserializer} */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthRequestDeserializer Unit Tests")
class AuthRequestDeserializerTest {

    private JsonMapper mapper;

    @BeforeEach
    void setUp() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(OAuth2AuthorizationRequest.class, new AuthRequestDeserializer());

        mapper = JsonMapper.builder().addModule(module).build();
    }

    @Test
    @DisplayName("deserialize should parse complete json correctly")
    void deserialize_ShouldParseCompleteJsonCorrectly() {

        String json =
                """
                {
                  "clientId":"client-id",
                  "authorizationUri":"https://auth.alpaca.com",
                  "redirectUri":"https://alpaca.com/callback",
                  "state":"oauth-state",
                  "scopes":["read","write"],
                  "attributes":{"roles":["admin","user"]},
                  "additionalParameters":{"tenant":"alpaca"}
                }
                """;

        OAuth2AuthorizationRequest request =
                mapper.readValue(json, OAuth2AuthorizationRequest.class);

        assertAll(
                () -> assertEquals("client-id", request.getClientId()),
                () -> assertEquals("https://auth.alpaca.com", request.getAuthorizationUri()),
                () -> assertEquals("https://alpaca.com/callback", request.getRedirectUri()),
                () -> assertEquals("oauth-state", request.getState()),
                () -> assertEquals(Set.of("read", "write"), request.getScopes()),
                () ->
                        assertEquals(
                                Map.of("roles", List.of("admin", "user")), request.getAttributes()),
                () -> assertEquals(Map.of("tenant", "alpaca"), request.getAdditionalParameters()));
    }

    @Test
    @DisplayName("deserialize should ignore unknown fields")
    void deserialize_ShouldIgnoreUnknownFields() {

        String json =
                """
                {
                  "clientId":"client-id",
                  "authorizationUri":"https://auth.alpaca.com",
                  "redirectUri":"https://alpaca.com/callback",
                  "state":"oauth-state",
                  "unknown":{"nested":"value"},
                  "scopes":["openid"]
                }
                """;

        OAuth2AuthorizationRequest request =
                mapper.readValue(json, OAuth2AuthorizationRequest.class);

        assertEquals("client-id", request.getClientId());
        assertEquals(Set.of("openid"), request.getScopes());
    }

    @Test
    @DisplayName("deserialize should return empty scopes when scopes field is missing")
    void deserialize_ShouldReturnEmptyScopes_WhenScopesFieldIsMissing() {

        String json =
                """
                {
                  "clientId":"client-id",
                  "authorizationUri":"https://auth.alpaca.com",
                  "redirectUri":"https://alpaca.com/callback",
                  "state":"oauth-state"
                }
                """;

        OAuth2AuthorizationRequest request =
                mapper.readValue(json, OAuth2AuthorizationRequest.class);

        assertTrue(request.getScopes().isEmpty());
    }

    @Test
    @DisplayName("deserialize should return empty scopes when scopes is not an array")
    void deserialize_ShouldReturnEmptyScopes_WhenScopesIsNotArray() {

        String json =
                """
                {
                  "clientId":"client-id",
                  "authorizationUri":"https://auth.alpaca.com",
                  "redirectUri":"https://alpaca.com/callback",
                  "state":"oauth-state",
                  "scopes":123
                }
                """;

        OAuth2AuthorizationRequest request =
                mapper.readValue(json, OAuth2AuthorizationRequest.class);

        assertTrue(request.getScopes().isEmpty());
    }

    @Test
    @DisplayName("deserialize should ignore non string scope values")
    void deserialize_ShouldIgnoreNonStringScopeValues() {

        String json =
                """
                {
                  "clientId":"client-id",
                  "authorizationUri":"https://auth.alpaca.com",
                  "redirectUri":"https://alpaca.com/callback",
                  "state":"oauth-state",
                  "scopes":["openid", 1, true, {"foo":"bar"}]
                }
                """;

        OAuth2AuthorizationRequest request =
                mapper.readValue(json, OAuth2AuthorizationRequest.class);

        assertEquals(Set.of("openid"), request.getScopes());
    }

    @Test
    @DisplayName("deserialize should remove duplicated scopes")
    void deserialize_ShouldRemoveDuplicatedScopes() {

        String json =
                """
                {
                  "clientId":"client-id",
                  "authorizationUri":"https://auth.alpaca.com",
                  "redirectUri":"https://alpaca.com/callback",
                  "state":"oauth-state",
                  "scopes":["openid","openid","profile"]
                }
                """;

        OAuth2AuthorizationRequest request =
                mapper.readValue(json, OAuth2AuthorizationRequest.class);

        assertEquals(Set.of("openid", "profile"), request.getScopes());
    }

    @ParameterizedTest
    @MethodSource("missingRequiredFieldProvider")
    @DisplayName("deserialize should throw exception when required field is missing")
    void deserialize_ShouldThrowException_WhenRequiredFieldIsMissing(
            String json, String missingField) {

        JacksonException exception =
                assertThrows(
                        JacksonException.class,
                        () -> mapper.readValue(json, OAuth2AuthorizationRequest.class));

        assertTrue(
                exception.getOriginalMessage().contains("Missing required field " + missingField));
    }

    private static Stream<Arguments> missingRequiredFieldProvider() {
        return Stream.of(
                Arguments.of(
                        """
                        {
                          "clientId":"client-id",
                          "authorizationUri":"https://auth.alpaca.com",
                          "redirectUri":"https://alpaca.com/callback"
                        }
                        """,
                        "state"),
                Arguments.of(
                        """
                        {
                          "authorizationUri":"https://auth.alpaca.com",
                          "redirectUri":"https://alpaca.com/callback",
                          "state":"oauth-state"
                        }
                        """,
                        "clientId"),
                Arguments.of(
                        """
                        {
                          "clientId":"client-id",
                          "redirectUri":"https://alpaca.com/callback",
                          "state":"oauth-state"
                        }
                        """,
                        "authorizationUri"),
                Arguments.of(
                        """
                        {
                          "clientId":"client-id",
                          "authorizationUri":"https://auth.alpaca.com",
                          "state":"oauth-state"
                        }
                        """,
                        "redirectUri"));
    }

    @Test
    @DisplayName("deserialize should throw exception when redirectUri is missing")
    void deserialize_ShouldThrowException_WhenRedirectUriIsMissing() {

        String json =
                """
                {
                  "clientId":"client-id",
                  "authorizationUri":"https://auth.alpaca.com",
                  "state":"oauth-state"
                }
                """;

        JacksonException exception =
                assertThrows(
                        JacksonException.class,
                        () -> mapper.readValue(json, OAuth2AuthorizationRequest.class));

        assertTrue(exception.getOriginalMessage().contains("Missing required field redirectUri"));
    }

    @Test
    @DisplayName("deserialize should advance parser when current token is null")
    void deserialize_ShouldAdvanceParser_WhenCurrentTokenIsNull() {

        String json =
                """
                {
                  "clientId":"client-id",
                  "authorizationUri":"https://auth.alpaca.com",
                  "redirectUri":"https://alpaca.com/callback",
                  "state":"oauth-state"
                }
                """;

        JsonFactory factory = new JsonFactory();
        ObjectReadContext context = new ObjectReadContext.Base();

        try (JsonParser parser = factory.createParser(context, json)) {

            assertNull(parser.currentToken());

            AuthRequestDeserializer deserializer = new AuthRequestDeserializer();

            OAuth2AuthorizationRequest request = deserializer.deserialize(parser, null);

            assertEquals("client-id", request.getClientId());
        }
    }

    @Test
    @DisplayName("deserialize should throw exception when root token is not object")
    void deserialize_ShouldThrowException_WhenRootTokenIsNotObject() {

        String json =
                """
                ["invalid"]
                """;

        JacksonException exception =
                assertThrows(
                        JacksonException.class,
                        () -> mapper.readValue(json, OAuth2AuthorizationRequest.class));

        assertTrue(
                exception
                        .getOriginalMessage()
                        .contains("Expected JSON object for OAuth2AuthorizationRequest"));
    }

    @Test
    @DisplayName("deserialize should delegate unexpected token handling")
    void deserialize_ShouldDelegateUnexpectedTokenHandling() {

        JsonFactory factory = new JsonFactory();
        ObjectReadContext context = new ObjectReadContext.Base();

        try (JsonParser parser = factory.createParser(context, "\"invalid\"")) {

            parser.nextToken();

            AuthRequestDeserializer deserializer = new AuthRequestDeserializer();

            DeserializationContext deserializationContext = mock(DeserializationContext.class);

            OAuth2AuthorizationRequest expected =
                    OAuth2AuthorizationRequest.authorizationCode()
                            .clientId("client")
                            .authorizationUri("uri")
                            .redirectUri("redirect")
                            .state("state")
                            .build();

            JavaType javaType =
                    mapper.getTypeFactory().constructType(OAuth2AuthorizationRequest.class);

            when(deserializationContext.handleUnexpectedToken(
                            eq(javaType), eq(JsonToken.START_OBJECT), eq(parser), anyString()))
                    .thenReturn(expected);

            OAuth2AuthorizationRequest result =
                    deserializer.deserialize(parser, deserializationContext);

            assertSame(expected, result);
        }
    }

    @Test
    @DisplayName("handledType should return OAuth2AuthorizationRequest class")
    void handledType_ShouldReturnOAuth2AuthorizationRequestClass() {

        AuthRequestDeserializer deserializer = new AuthRequestDeserializer();

        Class<OAuth2AuthorizationRequest> result = deserializer.handledType();

        assertEquals(OAuth2AuthorizationRequest.class, result);
    }
}
