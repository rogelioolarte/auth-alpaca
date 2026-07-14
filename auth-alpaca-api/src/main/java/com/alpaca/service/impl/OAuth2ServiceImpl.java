package com.alpaca.service.impl;

import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.OAuth2AuthenticationProcessingException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.security.oauth2.userinfo.OAuth2UserInfo;
import com.alpaca.security.oauth2.userinfo.OAuth2UserInfoFactory;
import com.alpaca.service.IAuthService;
import com.alpaca.service.IOAuth2Service;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Generated;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service implementation for OAuth2 login flows. Extends Spring Security's {@link
 * DefaultOAuth2UserService} to process user information from external OAuth2 providers (e.g.
 * Google).
 *
 * <p>Handles the full OAuth2 registration-or-login flow: extracting user info from the provider's
 * attributes, creating local user accounts for first-time OAuth2 users, linking existing accounts
 * to the OAuth2 provider, and provisioning initial profiles.
 *
 * @see DefaultOAuth2UserService
 * @see IOAuth2Service
 */
@Component
@AllArgsConstructor
public class OAuth2ServiceImpl extends DefaultOAuth2UserService implements IOAuth2Service {

    private final IAuthService authService;

    /**
     * Handles OAuth2 login by processing the provider's user data and delegating logic.
     *
     * @param userRequest the OAuth2 user request
     * @return an authenticated {@link OAuth2User}
     */
    @Override
    @Generated
    @NonNull
    public OAuth2User loadUser(@NonNull OAuth2UserRequest userRequest) {
        try {
            return processOAuth2User(userRequest, super.loadUser(userRequest));
        } catch (ResponseStatusException e) {
            String reason = e.getReason();
            throw new InternalAuthenticationServiceException(
                    Objects.requireNonNullElseGet(reason, e::getMessage));
        } catch (Exception e) {
            throw new InternalAuthenticationServiceException("Error during authentication", e);
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
                    "The account does not have enough information");
        }
        if (!StringUtils.hasText(userInfo.getFirstName())
                && !StringUtils.hasText(userInfo.getLastName())) {
            throw new BadRequestException("The account does not have enough information");
        }

        return new UserPrincipal(
                authService.registerOAuth2User(userInfo), userInfo.getAttributes());
    }
}
