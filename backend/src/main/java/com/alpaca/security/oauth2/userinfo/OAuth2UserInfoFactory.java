package com.alpaca.security.oauth2.userinfo;

import com.alpaca.exception.BadRequestException;
import java.util.Map;

/**
 * Factory for creating provider-specific OAuth2 user information handlers.
 *
 * <p>This class follows the Factory design pattern by encapsulating the logic to instantiate an
 * appropriate {@link OAuth2UserInfo} subclass based on the OAuth2 provider identified by {@code
 * registrationId}.
 *
 * <p>Current implementation supports:
 *
 * <ul>
 *   <li><strong>google</strong> — returns an instance of {@link GoogleUserInfo}.
 * </ul>
 *
 * If the {@code registrationId} does not match a supported provider, a {@link BadRequestException}
 * is thrown with a descriptive error message.
 *
 * @see OAuth2UserInfo
 * @see GoogleUserInfo
 * @see BadRequestException
 */
public class OAuth2UserInfoFactory {

    /**
     * Returns an {@link OAuth2UserInfo} instance suitable for the specified provider.
     *
     * @param registrationId the provider identifier (e.g., "google")
     * @param attributes the raw user attributes from the OAuth2 provider
     * @return a concrete {@link OAuth2UserInfo} subclass for the provider
     * @throws BadRequestException if the provider is not supported
     */
    public static OAuth2UserInfo getOAuth2UserInfo(
            String registrationId, Map<String, Object> attributes) {
        if (registrationId.equalsIgnoreCase("google")) {
            return new GoogleUserInfo(attributes);
        } else {
            throw new BadRequestException(
                    String.format("Login with %s is not supported", registrationId));
        }
    }
}
