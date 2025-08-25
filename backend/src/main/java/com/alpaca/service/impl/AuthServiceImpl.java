package com.alpaca.service.impl;

import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.entity.Profile;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.exception.OAuth2AuthenticationProcessingException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.security.manager.JJwtManager;
import com.alpaca.security.manager.PasswordManager;
import com.alpaca.security.oauth2.userinfo.OAuth2UserInfo;
import com.alpaca.security.oauth2.userinfo.OAuth2UserInfoFactory;
import com.alpaca.service.IAuthService;
import com.alpaca.service.IProfileService;
import com.alpaca.service.IRoleService;
import com.alpaca.service.IUserService;
import java.util.Map;
import java.util.UUID;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Implementation of {@link IAuthService}, handling authentication, user registration, and OAuth2
 * login flows within a Spring Security context.
 *
 * <p>Extends {@link DefaultOAuth2UserService} to support custom OAuth2 user processing, JWT token
 * generation, and security context management.
 *
 * @see DefaultOAuth2UserService
 * @see IAuthService
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl extends DefaultOAuth2UserService implements IAuthService {

    private final IRoleService roleService;
    private final IUserService userService;
    private final IProfileService profileService;
    private final JJwtManager manager;
    private final PasswordManager passwordManager;

    /**
     * Sets the Spring Security context with the provided authentication object.
     *
     * @param authentication the authentication object; must not be {@code null}
     * @return the authenticated user's principal
     * @throws UnauthorizedException if the authentication object is {@code null}
     */
    public Object setSecurityContextBefore(Authentication authentication) {
        if (authentication == null) {
            throw new UnauthorizedException("The account has been deactivated or blocked");
        }
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        return authentication.getPrincipal();
    }

    /**
     * Authenticates a user using email and password and returns a JWT token wrapped in DTO.
     *
     * @param email user's email
     * @param password raw password
     * @return {@link AuthResponseDTO} containing the JWT token
     * @throws NotFoundException if the email is not registered
     */
    @Override
    public AuthResponseDTO login(String email, String password) {
        return new AuthResponseDTO(
                manager.createToken(
                        (UserPrincipal) setSecurityContextBefore(authenticate(email, password))));
    }

    /**
     * Registers a new user, assigns default role, and logs them in returning a JWT token.
     *
     * @param email new user's email
     * @param password raw password
     * @return {@link AuthResponseDTO} for the newly registered user
     * @throws BadRequestException if the email is already registered
     */
    @Override
    public AuthResponseDTO register(String email, String password) {
        if (userService.existsByEmail(email)) {
            throw new BadRequestException("Email already registered");
        }
        userService.register(
                new User(
                        email,
                        passwordManager.encodePassword(password),
                        roleService.getUserRoles()));
        return login(email, password);
    }

    /**
     * Loads a user by username (email), required by Spring Security.
     *
     * @param username user's email
     * @return {@link UserDetails} for authentication
     * @throws UsernameNotFoundException if user is not found
     */
    @Override
    public UserDetails loadUserByUsername(String username) {
        return new UserPrincipal(userService.findByEmail(username), null);
    }

    /**
     * Validates raw password against stored user details and enforces account status checks.
     *
     * @param rawPassword the entered password
     * @param userDetails stored user details
     * @return authenticated {@link UserDetails}
     * @throws BadRequestException if validation fails
     * @throws UnauthorizedException if account is disabled or locked
     */
    public UserDetails validateUserDetails(String rawPassword, UserDetails userDetails) {
        if (userDetails == null) {
            throw new BadRequestException("Invalid Username or Password");
        }
        if (rawPassword == null
                || rawPassword.isBlank()
                || !passwordManager.matches(rawPassword, userDetails.getPassword())) {
            throw new BadRequestException("Invalid Password");
        }
        if (!(userDetails.isEnabled()
                && userDetails.isAccountNonLocked()
                && userDetails.isAccountNonExpired()
                && userDetails.isCredentialsNonExpired())) {
            throw new UnauthorizedException("The account has been deactivated or blocked");
        }
        return userDetails;
    }

    /**
     * Performs authentication using Spring's authentication token model.
     *
     * @param username user's email
     * @param password raw password
     * @return an {@link Authentication} object
     */
    public Authentication authenticate(String username, String password) {
        return new UsernamePasswordAuthenticationToken(
                validateUserDetails(password, loadUserByUsername(username)), null);
    }

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
                                            true,
                                            true,
                                            true,
                                            true,
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
