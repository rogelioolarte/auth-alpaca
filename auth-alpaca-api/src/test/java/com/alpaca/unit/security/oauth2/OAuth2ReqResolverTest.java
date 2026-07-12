package com.alpaca.unit.security.oauth2;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.alpaca.security.oauth2.OAuth2ReqResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2ReqResolver Unit Tests")
class OAuth2ReqResolverTest {

    @Mock private ClientRegistrationRepository clientRegRepo;
    @Mock private HttpServletRequest servletRequest;
    @Mock private OAuth2AuthorizationRequestResolver defaultResolver;

    private OAuth2ReqResolver resolver;
    private final String clientId = "clientABC";
    private final OAuth2AuthorizationRequest baseRequest =
            OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri("https://auth.server/authorize")
                    .clientId(clientId)
                    .redirectUri("https://app/callback")
                    .state("xyz")
                    .build();

    @BeforeEach
    void setUp() {
        resolver = new OAuth2ReqResolver(clientRegRepo, "/oauth2/authorize-client");
        ReflectionTestUtils.setField(resolver, "defaultResolver", defaultResolver);
    }

    @Test
    @DisplayName("resolve(request): Should delegate and add PKCE + custom challenge when present")
    void resolve_ShouldAddPKCEAndCustomChallenge_WhenDefaultReturnsRequest() {
        String clientChallenge = "custom-client-challenge";
        when(defaultResolver.resolve(servletRequest)).thenReturn(baseRequest);
        when(servletRequest.getParameter("client_code_challenge")).thenReturn(clientChallenge);

        OAuth2AuthorizationRequest result = resolver.resolve(servletRequest);

        assertNotNull(result);
        assertEquals(clientChallenge, result.getAttributes().get("client_code_challenge"));
        assertNotNull(result.getAttributes().get(PkceParameterNames.CODE_VERIFIER));
        assertEquals(
                "S256",
                result.getAdditionalParameters().get(PkceParameterNames.CODE_CHALLENGE_METHOD));
    }

    @Test
    @DisplayName("resolve(request, id): Should delegate and add PKCE")
    void resolveWithId_ShouldAddPKCE_WhenDefaultReturnsRequest() {
        when(defaultResolver.resolve(servletRequest, clientId)).thenReturn(baseRequest);

        OAuth2AuthorizationRequest result = resolver.resolve(servletRequest, clientId);

        assertNotNull(result);
        assertTrue(result.getAttributes().containsKey(PkceParameterNames.CODE_VERIFIER));
        assertEquals(
                "S256",
                result.getAdditionalParameters().get(PkceParameterNames.CODE_CHALLENGE_METHOD));
    }

    @Test
    @DisplayName("resolve: Should return null when default resolver returns null")
    void resolve_ShouldReturnNull_WhenDefaultReturnsNull() {
        when(defaultResolver.resolve(servletRequest)).thenReturn(null);

        OAuth2AuthorizationRequest result = resolver.resolve(servletRequest);

        assertNull(result);
    }

    @Test
    @DisplayName("addPKCEParameters: Should handle NoSuchAlgorithmException and fallback")
    void addPKCEParameters_ShouldFallback_WhenHashAlgorithmMissing() {
        StringKeyGenerator fixedGenerator = () -> "plain-text-verifier";
        ReflectionTestUtils.setField(resolver, "securityKeyGenerator", fixedGenerator);

        Map<String, Object> attributes = new HashMap<>();
        Map<String, Object> additionalParameters = new HashMap<>();

        try (MockedStatic<MessageDigest> mockedDigest = mockStatic(MessageDigest.class)) {
            mockedDigest
                    .when(() -> MessageDigest.getInstance("SHA-256"))
                    .thenThrow(new NoSuchAlgorithmException());

            ReflectionTestUtils.invokeMethod(
                    resolver, "addPKCEParameters", attributes, additionalParameters);

            assertEquals(
                    fixedGenerator.generateKey(), attributes.get(PkceParameterNames.CODE_VERIFIER));
            assertEquals(
                    fixedGenerator.generateKey(),
                    additionalParameters.get(PkceParameterNames.CODE_CHALLENGE));
            assertNull(additionalParameters.get(PkceParameterNames.CODE_CHALLENGE_METHOD));
        }
    }

    @Test
    @DisplayName(
            "customizeAuthorizationRequest: Should skip custom challenge when parameter is blank")
    void customizeAuthorizationRequest_ShouldSkipCustomChallenge_WhenParameterIsBlank() {
        when(defaultResolver.resolve(servletRequest)).thenReturn(baseRequest);
        when(servletRequest.getParameter("client_code_challenge")).thenReturn("");

        OAuth2AuthorizationRequest result = resolver.resolve(servletRequest);

        assertNotNull(result);
        assertFalse(result.getAttributes().containsKey("client_code_challenge"));
    }
}
