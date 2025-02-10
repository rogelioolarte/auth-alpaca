package com.example.model;

import com.example.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Represents the Principal User Details in the security context.
 * This class implements both {@link OAuth2User} and {@link UserDetails}
 * to support authentication via OAuth2 and traditional login mechanisms.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal implements OAuth2User, UserDetails {

    /**
     * Unique identifier for the user.
     */
    private UUID id;

    /**
     * Unique identifier for the associated profile, if available.
     */
    private UUID profileId;

    /**
     * Unique identifier for the associated advertiser account, if available.
     */
    private UUID advertiserId;

    /**
     * The username of the user, typically the email address.
     */
    private String username;

    /**
     * The encrypted password of the user.
     */
    private String password;

    /**
     * The authorities granted to the user for authorization purposes.
     */
    private Collection<? extends GrantedAuthority> authorities;

    /**
     * Additional attributes provided by an OAuth2 authentication provider.
     */
    private Map<String, Object> attributes;

    /**
     * Constructs a new {@code UserPrincipal} instance using a {@link User} entity.
     * This constructor extracts relevant information from the {@code User} object
     * and assigns it to the corresponding fields in {@code UserPrincipal}.
     *
     * @param user      the {@link User} entity containing user details.
     * @param attributes additional attributes obtained from an OAuth2 authentication provider.
     */
    public UserPrincipal(User user, Map<String, Object> attributes) {
        this.id = user.getId();
        this.profileId = user.getProfile() != null ? user.getProfile().getId() : null;
        this.advertiserId = user.getAdvertiser() != null ? user.getAdvertiser().getId() : null;
        this.username = user.getEmail();
        this.password = user.getPassword();
        this.authorities = user.getAuthorities();
        this.attributes = attributes;
    }

    /**
     * Retrieves the attributes associated with the user from an OAuth2 provider.
     *
     * @return a map of user attributes.
     */
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * Retrieves the name of the user.
     *
     * @return the username of the user.
     */
    @Override
    public String getName() {
        return username;
    }
}
