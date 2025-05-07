package com.alpaca.unit.security.oauth2;

import com.alpaca.security.oauth2.OAuth2ReqResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/** Unit tests for {@link OAuth2ReqResolver} */
@ExtendWith(MockitoExtension.class)
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
  void resolveDelegatesToDefaultAndAddsPKCE() {
    when(defaultResolver.resolve(servletRequest)).thenReturn(baseRequest);
    OAuth2AuthorizationRequest custom = resolver.resolve(servletRequest);
    assertNotNull(custom, "The resolver must not return null");
    assertNotNull(
        custom.getAttributes().get(PkceParameterNames.CODE_VERIFIER), "Must contain code_verifier");
    assertNotNull(
        custom.getAdditionalParameters().get(PkceParameterNames.CODE_CHALLENGE),
        "Must contain code_challenge");
    assertEquals(
        "S256",
        custom.getAdditionalParameters().get(PkceParameterNames.CODE_CHALLENGE_METHOD),
        "The PKCE method must be S256");
  }

  @Test
  void resolveWithClientIdDelegatesToDefaultAndAddsPKCE() {
    when(defaultResolver.resolve(servletRequest, clientId)).thenReturn(baseRequest);
    OAuth2AuthorizationRequest custom = resolver.resolve(servletRequest, clientId);
    assertNotNull(custom, "The resolver must not return null");
    assertTrue(custom.getAttributes().containsKey(PkceParameterNames.CODE_VERIFIER));
    assertTrue(custom.getAdditionalParameters().containsKey(PkceParameterNames.CODE_CHALLENGE));
    assertEquals(
        "S256", custom.getAdditionalParameters().get(PkceParameterNames.CODE_CHALLENGE_METHOD));
  }

  @Test
  void resolveAddsPKCEParameters_whenDefaultReturnsRequest() {
    when(defaultResolver.resolve(servletRequest)).thenReturn(baseRequest);
    OAuth2AuthorizationRequest custom = resolver.resolve(servletRequest);
    assertNotNull(custom);
    String codeVerifier = (String) custom.getAttributes().get(PkceParameterNames.CODE_VERIFIER);
    assertNotNull(codeVerifier, "Must contain code_verifier");
    String codeChallenge =
        (String) custom.getAdditionalParameters().get(PkceParameterNames.CODE_CHALLENGE);
    assertNotNull(codeChallenge, "Must contain code_challenge");
    String method =
        (String) custom.getAdditionalParameters().get(PkceParameterNames.CODE_CHALLENGE_METHOD);
    assertEquals("S256", method, "The PKCE method must be S256");
  }

  @Test
  void resolveWithClientIdAddsPKCEParameters_whenDefaultReturnsRequest() {
    when(defaultResolver.resolve(servletRequest, clientId)).thenReturn(baseRequest);
    OAuth2AuthorizationRequest custom = resolver.resolve(servletRequest, clientId);
    assertNotNull(custom);
    assertTrue(custom.getAttributes().containsKey(PkceParameterNames.CODE_VERIFIER));
    assertTrue(custom.getAdditionalParameters().containsKey(PkceParameterNames.CODE_CHALLENGE));
    assertEquals(
        "S256", custom.getAdditionalParameters().get(PkceParameterNames.CODE_CHALLENGE_METHOD));
  }
}
