package com.alpaca.integration.service;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.entity.Role;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.OAuth2AuthenticationProcessingException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.resources.RoleProvider;
import com.alpaca.resources.UserProvider;
import com.alpaca.security.manager.PasswordManager;
import com.alpaca.service.IProfileService;
import com.alpaca.service.IRoleService;
import com.alpaca.service.IUserService;
import com.alpaca.service.impl.OAuth2ServiceImpl;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link OAuth2ServiceImpl}. */
@SpringBootTest
@Transactional
class OAuth2ServiceImplIT {

    @Autowired private OAuth2ServiceImpl service;

    @Autowired private IUserService userService;
    @Autowired private IRoleService roleService;
    @Autowired private IProfileService profileService;
    @Autowired private PasswordManager passwordManager;

    private User userTemplate;

    @BeforeEach
    void setup() {
        userTemplate = UserProvider.singleTemplate();
    }

    // ------------------------
    // processOAuth2User
    // ------------------------

    @Test
    void processOAuth2User_whenEmailMissing_thenThrowOAuth2AuthProcessingException() {
        // Build a minimal OAuth2UserRequest (ClientRegistration + AccessToken)
        ClientRegistration clientRegistration =
                ClientRegistration.withRegistrationId("google")
                        .clientId("id")
                        .clientSecret("secret")
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .tokenUri("https://example.com/token")
                        .authorizationUri("https://example.com/auth")
                        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                        .scope("openid", "email", "profile")
                        .build();

        OAuth2AccessToken token =
                new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        "access-token",
                        Instant.now(),
                        Instant.now().plusSeconds(3600));

        OAuth2UserRequest request = new OAuth2UserRequest(clientRegistration, token);

        // OAuth2User with attributes that miss email (or blank)
        OAuth2User oauthUser =
                new OAuth2User() {
                    @Override
                    public Map<String, Object> getAttributes() {
                        return Map.of("email", ""); // blank email => should trigger exception
                    }

                    @Override
                    public Set<GrantedAuthority> getAuthorities() {
                        return Collections.emptySet();
                    }

                    @Override
                    public String getName() {
                        return "no-email";
                    }
                };

        assertThrows(
                OAuth2AuthenticationProcessingException.class,
                () -> service.processOAuth2User(request, oauthUser),
                "Expected OAuth2AuthenticationProcessingException when provider doesn't return"
                        + " email");
    }

    // ------------------------
    // registerOrLoginOAuth2 - validation errors
    // ------------------------

    @Test
    void registerOrLoginOAuth2_whenMissingFields_thenBadRequest() {
        Map<String, Object> attrs = Map.of("any", "value");
        // missing email
        assertThrows(
                BadRequestException.class,
                () -> service.registerOrLoginOAuth2(null, "first", "last", "image", false, attrs));
        // blank firstName
        assertThrows(
                BadRequestException.class,
                () ->
                        service.registerOrLoginOAuth2(
                                "email@example.com", " ", "last", "image", false, attrs));
        // blank imageURL
        assertThrows(
                BadRequestException.class,
                () ->
                        service.registerOrLoginOAuth2(
                                "email@example.com", "first", "last", " ", false, attrs));
    }

    // ------------------------
    // registerOrLoginOAuth2 - new user flow
    // ------------------------

    @Test
    @Transactional
    void registerOrLoginOAuth2_whenNewUser_thenRegisterAndReturnUserPrincipal() {
        // Ensure default role "USER" exists (getUserRoles uses findByRoleName("USER"))
        Role role = RoleProvider.singleTemplate();
        role.setName("USER");
        role.setRolePermissions(Collections.emptySet());
        roleService.save(role);

        String email = "new.user@example.com";
        String first = "First";
        String last = "Last";
        String image = "https://example.com/avatar.png";
        boolean emailVerified = true;
        Map<String, Object> attrs = Map.of("k", "v");

        UserPrincipal principal =
                service.registerOrLoginOAuth2(email, first, last, image, emailVerified, attrs);

        assertNotNull(principal);
        assertNotNull(principal.getId(), "User should be persisted and have id");
        assertEquals(email, principal.getUsername());
        // attributes forwarded to UserPrincipal
        assertEquals(attrs, principal.getAttributes());
        // persisted user should have profile created with the values provided
        User persisted = userService.findByEmail(email);
        assertNotNull(persisted);
        assertNotNull(persisted.getProfile());
        assertEquals(first, persisted.getProfile().getFirstName());
        assertEquals(last, persisted.getProfile().getLastName());
        assertEquals(image, persisted.getProfile().getAvatarUrl());
    }

    // ------------------------
    // registerProfile - validations + success
    // ------------------------

    @Test
    void registerProfile_whenInvalidArgs_thenBadRequest() {
        // null user
        assertThrows(
                BadRequestException.class, () -> service.registerProfile(null, "f", "l", "img"));

        // user without id
        User tmp = UserProvider.singleTemplate();
        tmp.setId(null); // ensure id null
        assertThrows(
                BadRequestException.class, () -> service.registerProfile(tmp, "f", "l", "img"));

        // null firstName
        User tmp2 = UserProvider.singleTemplate();
        tmp2.setId(UUID.randomUUID());
        assertThrows(
                BadRequestException.class, () -> service.registerProfile(tmp2, null, "l", "img"));
    }

    @Test
    @Transactional
    void registerProfile_whenValid_thenSaveProfileOnUser() {
        // persist a user first
        Role role = roleService.save(RoleProvider.alternativeTemplate());
        User toRegister =
                new User(
                        userTemplate.getEmail(),
                        userTemplate.getPassword(),
                        false,
                        true,
                        new HashSet<>(Set.of(role)));
        User saved = userService.register(toRegister);

        String first = "John";
        String last = "Doe";
        String img = "https://example.com/john.png";

        User result = service.registerProfile(saved, first, last, img);

        assertNotNull(result.getProfile());
        assertEquals(first, result.getProfile().getFirstName());
        assertEquals(last, result.getProfile().getLastName());
        assertEquals(img, result.getProfile().getAvatarUrl());
    }

    // ------------------------
    // checkExistingUser - unauthorized & update flows
    // ------------------------

    @Test
    void checkExistingUser_whenUserNotAllowed_thenUnauthorized() {
        User blocked = UserProvider.createUser(false, false, false, true, true, true);
        // not persisted: checkExistingUser expects a User object - it checks isAllowUser()
        assertThrows(
                UnauthorizedException.class,
                () -> service.checkExistingUser(blocked, false),
                "Expected UnauthorizedException when user.isAllowUser() == false");
    }

    @Test
    @Transactional
    void checkExistingUser_whenNotGoogleConnected_thenSetGoogleConnectedAndPersist() {
        // create and persist user with googleConnected = false
        User u = UserProvider.createUser(true, true, true, true, true, false);
        u.setEmail(userTemplate.getEmail()); // unique email
        User persisted = userService.register(u);

        User updated = service.checkExistingUser(persisted, persisted.isEmailVerified());

        assertNotNull(updated);
        assertEquals(persisted.getId(), updated.getId());
        assertTrue(
                updated.isGoogleConnected(),
                "Service should set googleConnected to true and register the user");
    }

    @Test
    @Transactional
    void checkExistingUser_whenGoogleConnectedButEmailVerifiedChanged_thenUpdateEmailVerified() {
        // create and persist user with googleConnected = true and emailVerified = false
        User u = UserProvider.createUser(true, true, true, true, true, true);
        u.setEmail("verify.change@example.com");
        User persisted = userService.register(u);

        // pass emailVerified = true -> should update and persist
        User updated = service.checkExistingUser(persisted, true);

        assertNotNull(updated);
        assertEquals(persisted.getId(), updated.getId());
        assertTrue(updated.isEmailVerified());
    }

    @Test
    @Transactional
    void checkExistingUser_whenNoChangeNeeded_thenReturnSameUser() {
        // create and persist user with googleConnected = true and emailVerified = true
        User u = UserProvider.createUser(true, true, true, true, true, true);
        u.setEmail("no.change@example.com");
        User persisted = userService.register(u);

        User result = service.checkExistingUser(persisted, true);

        assertNotNull(result);
        assertEquals(persisted.getId(), result.getId());
        assertTrue(result.isGoogleConnected());
        assertTrue(result.isEmailVerified());
    }
}
