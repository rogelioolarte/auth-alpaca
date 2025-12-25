package com.alpaca.service.impl;

import com.alpaca.entity.Profile;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.OAuth2AuthenticationProcessingException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.security.manager.PasswordManager;
import com.alpaca.security.oauth2.userinfo.OAuth2UserInfo;
import com.alpaca.security.oauth2.userinfo.OAuth2UserInfoFactory;
import com.alpaca.service.IOAuth2Service;
import com.alpaca.service.IProfileService;
import com.alpaca.service.IRoleService;
import com.alpaca.service.IUserService;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Generated;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@AllArgsConstructor
public class OAuth2ServiceImpl extends DefaultOAuth2UserService implements IOAuth2Service {

    private final IRoleService roleService;
    private final IUserService userService;
    private final IProfileService profileService;
    private final PasswordManager passwordManager;

    /**
     * Handles OAuth2 login by processing the provider's user data and delegating logic.
     *
     * @param userRequest the OAuth2 user request
     * @return an authenticated {@link OAuth2User}
     */
    @Override
    @Generated
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        try {
            return processOAuth2User(userRequest, super.loadUser(userRequest));
        } catch (ResponseStatusException e) {
            throw new InternalAuthenticationServiceException(e.getReason());
        } catch (Exception e) {
            throw new InternalAuthenticationServiceException(
                    "Error during authentication", e.getCause());
        }
    }

    /**
     * Processes OAuth2 user data: registers or connects existing user based on attributes.
     *
     * @param request the OAuth2 user request
     * @param user the authenticated OAuth2 user
     * @return an {@link OAuth2User}
     * @throws OAuth2AuthenticationProcessingException if required attributes are missing
     */
    @Generated
    public OAuth2User processOAuth2User(OAuth2UserRequest request, OAuth2User user) {
        OAuth2UserInfo userInfo =
                OAuth2UserInfoFactory.getOAuth2UserInfo(
                        request.getClientRegistration().getRegistrationId(), user.getAttributes());
        if (userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
            throw new OAuth2AuthenticationProcessingException(
                    "Email not found from OAuth2 Provider");
        }
        return registerOrLoginOAuth2(
                userInfo.getEmail(),
                userInfo.getFirstName(),
                userInfo.getLastName(),
                userInfo.getImageUrl(),
                userInfo.getEmailVerified(),
                user.getAttributes());
    }

    /**
     * Registers or logs in a user based on OAuth2 information. Sets up profile as needed.
     *
     * @param email user's email
     * @param firstName user's first name
     * @param lastName user's last name
     * @param imageURL profile image URL
     * @param emailVerified email verification status
     * @param attributes other OAuth2 attributes
     * @return a new or existing {@link UserPrincipal}
     * @throws BadRequestException if required fields are missing
     */
    public UserPrincipal registerOrLoginOAuth2(
            String email,
            String firstName,
            String lastName,
            String imageURL,
            boolean emailVerified,
            Map<String, Object> attributes) {
        if (email == null
                || email.isBlank()
                || firstName == null
                || firstName.isBlank()
                || lastName == null
                || lastName.isBlank()
                || imageURL == null
                || imageURL.isBlank()) {
            throw new BadRequestException("The account does not have enough information");
        }

        if (!userService.existsByEmail(email)) {
            return new UserPrincipal(
                    registerProfile(
                            userService.register(
                                    new User(
                                            email,
                                            passwordManager.encodePassword(
                                                    UUID.randomUUID().toString()),
                                            emailVerified,
                                            true,
                                            roleService.getUserRoles())),
                            firstName,
                            lastName,
                            imageURL),
                    attributes);
        } else {
            return new UserPrincipal(
                    checkExistingUser(userService.findByEmail(email), emailVerified), attributes);
        }
    }

    /**
     * Creates or updates a user's profile.
     *
     * @param user the user entity
     * @param firstName user’s first name
     * @param lastName user’s last name
     * @param imageURL user's avatar URL
     * @return updated {@link User} with profile
     * @throws BadRequestException if the user or mandatory fields are invalid
     */
    public User registerProfile(User user, String firstName, String lastName, String imageURL) {
        if (user == null
                || user.getId() == null
                || firstName == null
                || lastName == null
                || imageURL == null) {
            throw new BadRequestException("Invalid credentials from OAuth2 Provider");
        }
        user.setProfile(profileService.save(new Profile(firstName, lastName, "", imageURL, user)));
        return user;
    }

    /**
     * Updates an existing user's OAuth2 connection status and email verification flag.
     *
     * @param user the existing user
     * @param emailVerified OAuth2-provided email verification status
     * @return updated {@link User}
     * @throws UnauthorizedException if the user account is disabled or locked
     */
    public User checkExistingUser(User user, boolean emailVerified) {
        if (!user.isAllowUser()) {
            throw new UnauthorizedException("The account has been deactivated or blocked");
        }
        if (!user.isGoogleConnected()) {
            user.setGoogleConnected(true);
            return userService.register(user);
        }
        if (user.isEmailVerified() != emailVerified) {
            user.setEmailVerified(emailVerified);
            return userService.register(user);
        }
        return user;
    }
}
