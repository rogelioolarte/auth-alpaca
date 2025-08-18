package com.alpaca.security.oauth2.userinfo;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Abstract base class representing OAuth2 user information from a provider.
 *
 * <p>This class serves as a contract for provider-specific implementations (e.g., {@link
 * GoogleUserInfo}), defining common accessors for user attributes retrieved from OAuth2 providers.
 *
 * <p>Subclasses are expected to implement methods to extract essential user data:
 *
 * <ul>
 *   <li>{@code getId()}: unique user identifier
 *   <li>{@code getFullName()} / {@code getFirstName()} / {@code getLastName()}: name details
 *   <li>{@code getEmail()} and {@code getEmailVerified()}: email and verification status
 *   <li>{@code getImageUrl()}: profile image URL
 * </ul>
 *
 * The raw provider attributes are encapsulated in a {@link Map} and made accessible to subclasses
 * for extraction.
 *
 * @see GoogleUserInfo
 */
@Getter
@AllArgsConstructor
public abstract class OAuth2UserInfo {

    /**
     * The raw attributes received from the OAuth2 provider. Subclasses use this map to extract
     * provider-specific values.
     */
    protected Map<String, Object> attributes;

    /**
     * @return the provider-specific unique identifier for the user
     */
    public abstract String getId();

    /**
     * @return the full name of the user
     */
    public abstract String getFullName();

    /**
     * @return the user's first name
     */
    public abstract String getFirstName();

    /**
     * @return the user's last name
     */
    public abstract String getLastName();

    /**
     * @return the user's email address
     */
    public abstract String getEmail();

    /**
     * @return the URL of the user's profile image
     */
    public abstract String getImageUrl();

    /**
     * @return true if the user's email is verified; false otherwise
     */
    public abstract boolean getEmailVerified();
}
