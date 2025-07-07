package com.alpaca.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;

public class OAuth2ReqResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver defaultResolver;
    private final StringKeyGenerator securityKeyGenerator =
            new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 96);
    ;

    public OAuth2ReqResolver(
            ClientRegistrationRepository repository, String authorizationRequestBaseURI) {
        defaultResolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        repository, authorizationRequestBaseURI);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return customizeAuthorizationRequest(defaultResolver.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(
            HttpServletRequest request, String clientRegistrationId) {
        return customizeAuthorizationRequest(
                defaultResolver.resolve(request, clientRegistrationId));
    }

    private OAuth2AuthorizationRequest customizeAuthorizationRequest(
            OAuth2AuthorizationRequest request) {
        if (Objects.isNull(request)) {
            return null;
        }
        Map<String, Object> attributes = new HashMap<>(request.getAttributes());
        Map<String, Object> additionalParameters = new HashMap<>(request.getAdditionalParameters());
        addPKCEParameters(attributes, additionalParameters);
        return OAuth2AuthorizationRequest.from(request)
                .attributes(attributes)
                .additionalParameters(additionalParameters)
                .build();
    }

    private void addPKCEParameters(
            Map<String, Object> attributes, Map<String, Object> additionalParameters) {
        String codeVerifier = this.securityKeyGenerator.generateKey();
        attributes.put(PkceParameterNames.CODE_VERIFIER, codeVerifier);
        try {
            additionalParameters.put(PkceParameterNames.CODE_CHALLENGE, createHash(codeVerifier));
            additionalParameters.put(PkceParameterNames.CODE_CHALLENGE_METHOD, "S256");
        } catch (NoSuchAlgorithmException e) {
            additionalParameters.put(PkceParameterNames.CODE_CHALLENGE, codeVerifier);
        }
    }

    private static String createHash(String value) throws NoSuchAlgorithmException {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        MessageDigest.getInstance("SHA-256")
                                .digest(value.getBytes(StandardCharsets.US_ASCII)));
    }
}
