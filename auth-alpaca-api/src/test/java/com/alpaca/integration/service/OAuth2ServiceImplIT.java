package com.alpaca.integration.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.alpaca.entity.Profile;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.OAuth2AuthenticationProcessingException;
import com.alpaca.resources.provider.ProfileProvider;
import com.alpaca.resources.provider.UserProvider;
import com.alpaca.resources.utility.BaseIntegrationTests;
import com.alpaca.service.impl.OAuth2ServiceImpl;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

/** Integration tests for {@link OAuth2ServiceImpl}. */
@DisplayName("OAuth2ServiceImpl Integration Tests")
class OAuth2ServiceImplIT extends BaseIntegrationTests {

    @Autowired private OAuth2ServiceImpl service;

    private Instant now;
    private OAuth2UserRequest request;

    @BeforeEach
    void setup() {
        now = Instant.now();
        request = createOAuth2UserRequest();
    }

    @ParameterizedTest(name = "[{index}] email=''{0}''")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    @DisplayName("processOAuth2User throws when email is null, empty, or blank")
    void processOAuth2User_ShouldThrowOAuth2AuthenticationProcessingException_WhenEmailIsInvalid(
            String email) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", UUID.randomUUID().toString());
        attributes.put("email", email);

        OAuth2User oauth2User = createOAuth2User(attributes);

        assertThatThrownBy(() -> service.processOAuth2User(request, oauth2User))
                .isInstanceOf(OAuth2AuthenticationProcessingException.class)
                .hasMessage("Email not found from OAuth2 Provider");
    }

    @Test
    @DisplayName("processOAuth2User throws when email attribute is missing")
    void processOAuth2User_ShouldThrowOAuth2AuthenticationProcessingException_WhenEmailIsMissing() {
        Map<String, Object> attributes = Map.of("sub", UUID.randomUUID().toString());

        OAuth2User oauth2User = createOAuth2User(attributes);

        assertThatThrownBy(() -> service.processOAuth2User(request, oauth2User))
                .isInstanceOf(OAuth2AuthenticationProcessingException.class)
                .hasMessage("Email not found from OAuth2 Provider");
    }

    @ParameterizedTest(name = "[{index}] attribute={0}")
    @ValueSource(strings = {"email", "given_name", "family_name", "picture"})
    @DisplayName("processOAuth2User throws when OAuth2 information contains text")
    void processOAuth2User_ShouldThrowBadRequestException_WhenOAuth2InformationContainsText(
            String attributeName) {
        User user = UserProvider.singleTemplate();
        Profile profile = ProfileProvider.singleTemplate();

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(
                "sub",
                user.getId() == null ? UUID.randomUUID().toString() : user.getId().toString());
        attributes.put("email", user.getEmail());
        attributes.put("given_name", "");
        attributes.put("family_name", "");
        attributes.put("picture", "");

        if (!"email".equals(attributeName)) {
            if ("given_name".equals(attributeName)) {
                attributes.put(attributeName, profile.getFirstName());
            } else if ("family_name".equals(attributeName)) {
                attributes.put(attributeName, profile.getLastName());
            } else {
                attributes.put(attributeName, profile.getAvatarUrl());
            }
        }

        OAuth2User oauth2User = createOAuth2User(attributes);

        assertThatThrownBy(() -> service.processOAuth2User(request, oauth2User))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("The account does not have enough information");
    }

    private OAuth2User createOAuth2User(Map<String, Object> attributes) {
        return new DefaultOAuth2User(Collections.emptySet(), attributes, "sub");
    }

    private OAuth2UserRequest createOAuth2UserRequest() {
        ClientRegistration clientRegistration =
                ClientRegistration.withRegistrationId("google")
                        .clientId("client-id")
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .authorizationUri("https://example.com/oauth2/authorize")
                        .tokenUri("https://example.com/oauth2/token")
                        .userInfoUri("https://example.com/oauth2/userinfo")
                        .userNameAttributeName("sub")
                        .redirectUri("https://example.com/login/oauth2/code/google")
                        .build();

        OAuth2AccessToken accessToken =
                new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        "access-token",
                        now,
                        now.plusSeconds(3600));

        return new OAuth2UserRequest(clientRegistration, accessToken);
    }
}
