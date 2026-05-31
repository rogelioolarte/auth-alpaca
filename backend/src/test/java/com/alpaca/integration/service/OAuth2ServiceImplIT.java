package com.alpaca.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.alpaca.entity.Role;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.OAuth2AuthenticationProcessingException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.resources.provider.RoleProvider;
import com.alpaca.resources.provider.UserProvider;
import com.alpaca.resources.utility.BaseIntegrationTests;
import com.alpaca.service.IRoleService;
import com.alpaca.service.IUserService;
import com.alpaca.service.impl.OAuth2ServiceImpl;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link OAuth2ServiceImpl} */
@DisplayName("OAuth2ServiceImpl Integration Tests")
class OAuth2ServiceImplIT extends BaseIntegrationTests {

    @Autowired private OAuth2ServiceImpl service;
    @Autowired private IUserService userService;
    @Autowired private IRoleService roleService;

    private Instant now;

    @BeforeEach
    void setup() {
        now = Instant.now();
    }

    // -------------------------------------------------------------------------
    // processOAuth2User Logic
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("processOAuth2User: Should throw exception if email is missing or blank")
    @Transactional
    void processOAuth2User_ShouldThrow_WhenEmailIsInvalid() {
        // Arrange
        OAuth2UserRequest request = createMockRequest();
        OAuth2User oauthUser =
                new DefaultOAuth2User(
                        Collections.emptySet(), Map.of("sub", "123", "email", ""), "sub");

        // Act & Assert
        assertThatThrownBy(() -> service.processOAuth2User(request, oauthUser))
                .isInstanceOf(OAuth2AuthenticationProcessingException.class)
                .hasMessageContaining("Email not found");
    }

    // -------------------------------------------------------------------------
    // registerOrLoginOAuth2 Logic
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("registerOrLoginOAuth2: Should register new user and profile when valid")
    @Transactional
    void registerOrLoginOAuth2_ShouldRegister_WhenUserIsNew() {
        // Arrange
        Role userRole = RoleProvider.singleTemplate();
        userRole.setName("TEST_USER");
        userRole.setCreatedAt(now); // CRITICAL: Manual audit property
        roleService.save(userRole);

        String email = "new.alpaca@example.com";
        Map<String, Object> attrs = Map.of("sub", "123");

        // Act
        UserPrincipal principal =
                service.registerOrLoginOAuth2(
                        email, "John", "Doe", "http://image.url", true, attrs);

        // Assert
        assertThat(principal).isNotNull();
        assertThat(principal.getUsername()).isEqualTo(email);

        User persisted = userService.findByEmail(email);
        assertThat(persisted.getProfile()).isNotNull();
        assertThat(persisted.getProfile().getFirstName()).isEqualTo("John");
        assertThat(persisted.isGoogleConnected()).isTrue();
    }

    @Test
    @DisplayName("registerOrLoginOAuth2: Should throw BadRequest if mandatory info is missing")
    @Transactional
    void registerOrLoginOAuth2_ShouldThrow_WhenFieldsMissing() {
        Map<String, Object> attrs = Collections.emptyMap();

        assertThatThrownBy(() -> service.registerOrLoginOAuth2(null, "F", "L", "I", true, attrs))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(
                        () -> service.registerOrLoginOAuth2("e@e.com", " ", "L", "I", true, attrs))
                .isInstanceOf(BadRequestException.class);
    }

    // -------------------------------------------------------------------------
    // registerProfile Logic
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("registerProfile: Should throw BadRequest if user has no ID or is null")
    @Transactional
    void registerProfile_ShouldThrow_WhenUserInvalid() {
        User userWithoutId = UserProvider.singleTemplate();
        // ID is null by default from provider

        assertThatThrownBy(() -> service.registerProfile(null, "F", "L", "I"))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> service.registerProfile(userWithoutId, "F", "L", "I"))
                .isInstanceOf(BadRequestException.class);
    }

    // -------------------------------------------------------------------------
    // checkExistingUser Logic
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("checkExistingUser: Should throw Unauthorized if account is disabled")
    @Transactional
    void checkExistingUser_ShouldThrow_WhenUserBlocked() {
        // Arrange
        User blocked = UserProvider.singleTemplate();
        blocked.setAllowed(false);

        // Act & Assert
        assertThatThrownBy(() -> service.checkExistingUser(blocked, true))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("checkExistingUser: Should update googleConnected flag if false")
    @Transactional
    void checkExistingUser_ShouldUpdateConnection_WhenPreviouslyFalse() {
        // Arrange
        User user = UserProvider.singleTemplate();
        user.setEmail("existing@example.com");
        user.setGoogleConnected(false);
        user.setAllowed(true);
        user.setCreatedAt(now);
        User saved = userService.save(user);

        // Act
        User updated = service.checkExistingUser(saved, true);

        // Assert
        assertThat(updated.isGoogleConnected()).isTrue();
        assertThat(userService.findByEmail("existing@example.com").isGoogleConnected()).isTrue();
    }

    @Test
    @DisplayName("checkExistingUser: Should update emailVerified if status changed")
    @Transactional
    void checkExistingUser_ShouldUpdateVerification_WhenStatusChanges() {
        // Arrange
        User user = UserProvider.alternativeTemplate();
        user.setEmail("verify@example.com");
        user.setEmailVerified(false);
        user.setGoogleConnected(true);
        user.setAllowed(true);
        user.setCreatedAt(now);
        User saved = userService.save(user);

        // Act
        User updated = service.checkExistingUser(saved, true);

        // Assert
        assertThat(updated.isEmailVerified()).isTrue();
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    private OAuth2UserRequest createMockRequest() {
        ClientRegistration clientRegistration =
                ClientRegistration.withRegistrationId("google")
                        .clientId("id")
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .tokenUri("https://example.com/token")
                        .authorizationUri("https://example.com/auth")
                        .redirectUri("https://example.com/redirect")
                        .build();

        OAuth2AccessToken token =
                new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER, "token", now, now.plusSeconds(3600));

        return new OAuth2UserRequest(clientRegistration, token);
    }
}
