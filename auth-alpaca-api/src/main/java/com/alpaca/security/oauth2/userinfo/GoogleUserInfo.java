package com.alpaca.security.oauth2.userinfo;

import java.util.Map;

/**
 * OAuth2UserInfo implementation for Google OAuth2 provider.
 *
 * <p>This class extracts user-specific information from the attributes map provided by Google's
 * OAuth2/OpenID Connect userinfo endpoint. It assumes the standard attribute keys returned by
 * Google, including:
 *
 * <ul>
 *   <li><strong>sub</strong>: the unique user identifier
 *   <li><strong>name</strong>: the full name
 *   <li><strong>given_name</strong>: the first (given) name
 *   <li><strong>family_name</strong>: the last (family) name
 *   <li><strong>email</strong>: the user's email address
 *   <li><strong>picture</strong>: the user's profile image URL
 *   <li><strong>email_verified</strong>: whether the email address has been verified
 * </ul>
 *
 * <p>By extending {@link OAuth2UserInfo}, this class centralizes how the application accesses
 * provider-specific user data. It supports safe, consistent access and eases integration when
 * supporting multiple OAuth2 providers.
 *
 * <p>These keys align with Google's OpenID Connect specification for its UserInfo endpoint. <a
 * href="https://developers.google.com/identity/protocols/oauth2/openid-connect">OpenID Connect spec
 * – Userinfo Endpoint</a>
 *
 * @see OAuth2UserInfo
 */
public class GoogleUserInfo extends OAuth2UserInfo {

    /**
     * Constructs a new instance from the OAuth2 attributes map.
     *
     * @param attributes a map of user attributes fetched from Google's UserInfo endpoint
     */
    public GoogleUserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getFullName() {
        return (String) attributes.getOrDefault("name", "");
    }

    @Override
    public String getFirstName() {
        String givenName = (String) attributes.get("given_name");
        if (givenName != null && !givenName.isBlank()) return givenName;
        String fullName = getFullName();
        if (fullName != null && !fullName.isBlank()) {
            int space = fullName.indexOf(' ');
            return space > 0 ? fullName.substring(0, space).trim() : fullName;
        }
        return "";
    }

    @Override
    public String getLastName() {
        String familyName = (String) attributes.get("family_name");
        if (familyName != null && !familyName.isBlank()) return familyName;
        String fullName = getFullName();
        if (fullName != null && !fullName.isBlank()) {
            int space = fullName.lastIndexOf(' ');
            return space > 0 ? fullName.substring(space + 1).trim() : "";
        }
        return "";
    }

    @Override
    public String getEmail() {
        return (String) attributes.getOrDefault("email", "");
    }

    @Override
    public String getImageUrl() {
        return (String) attributes.getOrDefault("picture", "");
    }

    @Override
    public boolean getEmailVerified() {
        return (boolean) attributes.getOrDefault("email_verified", false);
    }
}
