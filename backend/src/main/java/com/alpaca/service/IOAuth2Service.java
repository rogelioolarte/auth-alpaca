package com.alpaca.service;

import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Service interface for OAuth2 authentication flows.
 *
 * <p>Extends Spring Security's {@link OAuth2UserService} to load user attributes from an external
 * OAuth2 provider (such as Google, GitHub, or Facebook) and map them into the application's user
 * model.
 *
 * @see org.springframework.security.oauth2.client.userinfo.OAuth2UserService
 */
public interface IOAuth2Service extends OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    /**
     * Loads or creates a user based on the attributes returned by the OAuth2 provider.
     *
     * <p>If a user with the provider's subject identifier already exists in the system, their
     * existing {@code OAuth2User} representation is returned. Otherwise, a new user is registered
     * using the profile data from the provider.
     *
     * @param userRequest the OAuth2 user request containing the access token and client
     *     registration details
     * @return an {@code OAuth2User} representing the authenticated user's attributes and
     *     authorities
     */
    OAuth2User loadUser(OAuth2UserRequest userRequest);
}
