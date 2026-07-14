package com.alpaca.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.alpaca.entity.Profile;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.OAuth2AuthenticationProcessingException;
import com.alpaca.model.UserPrincipal;
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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link OAuth2ServiceImpl}. */
@DisplayName("OAuth2ServiceImpl Integration Tests")
class OAuth2ServiceImplIT extends BaseIntegrationTests {

    @Autowired private OAuth2ServiceImpl service;
    private Instant now;
    private OAuth2UserRequest request;
    private User user;
    private Profile profile;

    @BeforeEach
    void setup() {
        now = Instant.now();
        request = createOAuth2UserRequest();
        user = UserProvider.singleTemplate();
        profile = ProfileProvider.singleTemplate();
    }

    @ParameterizedTest(name = "[{index}] email=''{0}''")
    @NullAndEmptySource
    @ValueSource(strings = {" ", " "})
    @DisplayName("processOAuth2User throws when email is null, empty, or blank")
    void processOAuth2User_ShouldThrowOAuth2AuthenticationProcessingException_WhenEmailIsInvalid(
            String email) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", UUID.randomUUID().toString());
        attributes.put("email", email);
        attributes.put("given_name", profile.getFirstName());
        attributes.put("family_name", profile.getLastName());
        OAuth2User oauth2User = createOAuth2User(attributes);
        assertThatThrownBy(() -> service.processOAuth2User(request, oauth2User))
                .isInstanceOf(OAuth2AuthenticationProcessingException.class)
                .hasMessage("The account does not have enough information");
    }

    @Test
    @DisplayName("processOAuth2User throws when email attribute is missing")
    void processOAuth2User_ShouldThrowOAuth2AuthenticationProcessingException_WhenEmailIsMissing() {
        Map<String, Object> attributes =
                Map.of(
                        "sub",
                        UUID.randomUUID().toString(),
                        "given_name",
                        profile.getFirstName(),
                        "family_name",
                        profile.getLastName());
        OAuth2User oauth2User = createOAuth2User(attributes);
        assertThatThrownBy(() -> service.processOAuth2User(request, oauth2User))
                .isInstanceOf(OAuth2AuthenticationProcessingException.class)
                .hasMessageContaining("The account does not have enough information");
    }

    @ParameterizedTest(name = "[{index}] givenName=''{0}'', lastName=''{1}''")
    @CsvSource(value = {"NULL, NULL", "NULL, EMPTY", "EMPTY, NULL", "EMPTY, EMPTY", "BLANK, BLANK"})
    @DisplayName("processOAuth2User throws when both first and last name have no text")
    void processOAuth2User_ShouldThrowBadRequestException_WhenNamesHaveNoText(
            String givenNameCase, String lastNameCase) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", UUID.randomUUID().toString());
        attributes.put("email", user.getEmail());
        putAttribute(attributes, "given_name", resolveTextCase(givenNameCase));
        putAttribute(attributes, "family_name", resolveTextCase(lastNameCase));
        OAuth2User oauth2User = createOAuth2User(attributes);
        assertThatThrownBy(() -> service.processOAuth2User(request, oauth2User))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("The account does not have enough information");
    }

    @Test
    @Transactional
    @DisplayName("processOAuth2User registers user when only first name has text")
    void processOAuth2User_ShouldRegisterUser_WhenOnlyFirstNameHasText() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", UUID.randomUUID().toString());
        attributes.put("email", user.getEmail());
        attributes.put("given_name", profile.getFirstName());
        attributes.put("family_name", "");
        OAuth2User result = service.processOAuth2User(request, createOAuth2User(attributes));
        assertThat(result)
                .isInstanceOf(UserPrincipal.class)
                .extracting(OAuth2User::getAttributes)
                .isNotNull();
    }

    @Test
    @Transactional
    @DisplayName("processOAuth2User registers user when only last name has text")
    void processOAuth2User_ShouldRegisterUser_WhenOnlyLastNameHasText() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", UUID.randomUUID().toString());
        attributes.put("email", user.getEmail());
        attributes.put("given_name", "");
        attributes.put("family_name", profile.getLastName());
        OAuth2User result = service.processOAuth2User(request, createOAuth2User(attributes));
        assertThat(result)
                .isInstanceOf(UserPrincipal.class)
                .extracting(OAuth2User::getAttributes)
                .isNotNull();
    }

    @Test
    @Transactional
    @DisplayName("processOAuth2User registers user when OAuth2 information is valid")
    void processOAuth2User_ShouldReturnUserPrincipal_WhenOAuth2InformationIsValid() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", UUID.randomUUID().toString());
        attributes.put("email", user.getEmail());
        attributes.put("given_name", profile.getFirstName());
        attributes.put("family_name", profile.getLastName());
        attributes.put("picture", profile.getAvatarUrl());
        attributes.put("email_verified", true);
        OAuth2User result = service.processOAuth2User(request, createOAuth2User(attributes));
        assertThat(result).isInstanceOf(UserPrincipal.class);
        assertThat(result.getAttributes()).isNotNull();
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

    private void putAttribute(
            Map<String, Object> attributes, String attributeName, String attributeValue) {
        if (attributeValue != null) {
            attributes.put(attributeName, attributeValue);
        }
    }

    private String resolveTextCase(String value) {
        return switch (value) {
            case "NULL" -> null;
            case "EMPTY" -> "";
            case "BLANK" -> " ";
            default -> throw new IllegalArgumentException("Unsupported text case: " + value);
        };
    }
}
