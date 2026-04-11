package com.alpaca.model;

import com.alpaca.entity.User;
import io.jsonwebtoken.Claims;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Represents the Principal User Details in the security context. This class implements both {@link
 * OAuth2User} and {@link UserDetails} to support authentication via OAuth2 and traditional login
 * mechanisms.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal implements OAuth2User, UserDetails {

    /** Unique identifier for the user. */
    private UUID id;

    /** Unique identifier for the associated profile, if available. */
    private UUID profileId;

    /** Unique identifier for the associated advertiser account, if available. */
    private UUID advertiserId;

    /** The username of the user, typically the email address. */
    private String username;

    /** The encrypted password of the user. */
    private String password;

    /** Indicates whether the User's entity is enabled. Defaults to {@code true}. */
    private boolean enabled = true;

    /** Indicates whether the User's entity is not expired. Defaults to {@code true}. */
    private boolean accountNonExpired = true;

    /** Indicates whether the User's entity is not locked. Defaults to {@code true}. */
    private boolean accountNonLocked = true;

    /** Indicates whether the User's credentials are not expired. Defaults to {@code true}. */
    private boolean credentialsNonExpired = true;

    /** The authorities granted to the user for authorization purposes. */
    private Collection<GrantedAuthority> authorities = new HashSet<>();

    /**
     * Additional attributes provided by the OAuth2 authentication provider.
     *
     * <p>These attributes are used only during the OAuth2 authentication flow and are marked as
     * {@code transient} to prevent serialization or persistence in the security context or HTTP
     * session.
     */
    private transient Map<String, Object> attributes = new HashMap<>();

    /**
     * Constructs a new {@code UserPrincipal} instance using a {@link User} entity. This constructor
     * extracts relevant information from the {@code User} object and assigns it to the
     * corresponding fields in {@code UserPrincipal}.
     *
     * @param user the {@link User} entity containing user details.
     * @param attributes additional attributes obtained from an OAuth2 authentication provider.
     */
    public UserPrincipal(User user, Map<String, Object> attributes) {
        this.id = user.getId();
        this.profileId = user.getProfile() != null ? user.getProfile().getId() : null;
        this.advertiserId = user.getAdvertiser() != null ? user.getAdvertiser().getId() : null;
        this.username = user.getEmail();
        this.password = user.getPassword();
        this.enabled = user.isEnabled();
        this.accountNonLocked = user.isAccountNonLocked();
        this.accountNonExpired = user.isAccountNonExpired();
        this.credentialsNonExpired = user.isCredentialNonExpired();
        this.authorities = user.getAuthorities();
        this.attributes = attributes;
    }

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.profileId = user.getProfile() != null ? user.getProfile().getId() : null;
        this.advertiserId = user.getAdvertiser() != null ? user.getAdvertiser().getId() : null;
        this.username = user.getEmail();
        this.password = user.getPassword();
        this.enabled = user.isEnabled();
        this.accountNonLocked = user.isAccountNonLocked();
        this.accountNonExpired = user.isAccountNonExpired();
        this.credentialsNonExpired = user.isCredentialNonExpired();
        this.authorities = user.getAuthorities();
        this.attributes = null;
    }

    /**
     * Builds a {@link UserPrincipal} from JWT claims.
     *
     * @param claims the token claims
     */
    public UserPrincipal(Claims claims) {
        if (claims == null) {
            this.username = "anonymous";
            this.authorities = Collections.emptyList();
            return;
        }
        this.id = getUUIDFromClaim(claims, "userId");
        this.profileId = getUUIDFromClaim(claims, "profileId");
        this.advertiserId = getUUIDFromClaim(claims, "advertiserId");
        this.username = claims.getSubject();
        this.password = null;
        this.authorities =
                AuthorityUtils.commaSeparatedStringToAuthorityList(
                        claims.get("authorities", String.class));
        this.attributes = null;
    }

    private UUID getUUIDFromClaim(Claims claims, String key) {
        String value = claims.get(key, String.class);
        return (value != null && !value.isEmpty()) ? UUID.fromString(value) : null;
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

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (profileId != null ? profileId.hashCode() : 0);
        result = 31 * result + (advertiserId != null ? advertiserId.hashCode() : 0);
        result = 31 * result + username.hashCode();
        result = 31 * result + password.hashCode();
        result = 31 * result + Boolean.hashCode(enabled);
        result = 31 * result + Boolean.hashCode(accountNonExpired);
        result = 31 * result + Boolean.hashCode(accountNonLocked);
        result = 31 * result + Boolean.hashCode(credentialsNonExpired);
        result = 31 * result + (authorities != null ? authorities.hashCode() : 0);
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        return result;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserPrincipal that)) return false;
        return enabled == that.enabled
                && accountNonExpired == that.accountNonExpired
                && accountNonLocked == that.accountNonLocked
                && credentialsNonExpired == that.credentialsNonExpired
                && username != null
                && username.equals(that.username)
                && password != null
                && password.equals(that.password)
                && (authorities == that.authorities || authorities.equals(that.authorities))
                && (profileId == that.profileId || profileId.equals(that.profileId))
                && (advertiserId == that.advertiserId || advertiserId.equals(that.advertiserId));
    }
}
