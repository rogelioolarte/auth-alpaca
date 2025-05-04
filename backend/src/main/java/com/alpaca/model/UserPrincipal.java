package com.alpaca.model;

import com.alpaca.entity.User;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
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
  private boolean accountNoExpired = true;

  /** Indicates whether the User's entity is not locked. Defaults to {@code true}. */
  private boolean accountNoLocked = true;

  /** Indicates whether the User's credentials are not expired. Defaults to {@code true}. */
  private boolean credentialNoExpired = true;

  /** The authorities granted to the user for authorization purposes. */
  private Collection<? extends GrantedAuthority> authorities = new HashSet<>();

  /** Additional attributes provided by an OAuth2 authentication provider. */
  private Map<String, Object> attributes = new HashMap<>();

  /**
   * Constructs a new {@code UserPrincipal} instance using a {@link User} entity. This constructor
   * extracts relevant information from the {@code User} object and assigns it to the corresponding
   * fields in {@code UserPrincipal}.
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
    this.accountNoLocked = user.isAccountNoLocked();
    this.accountNoExpired = user.isAccountNoExpired();
    this.credentialNoExpired = user.isCredentialNoExpired();
    this.authorities = user.getAuthorities();
    this.attributes = attributes;
  }

  /**
   * Constructs a new {@code UserPrincipal} instance using a decoded JWT token values. This
   * constructor extracts relevant information from the {@code User} object and assigns it to the
   * corresponding fields in {@code UserPrincipal}.
   *
   * @param userId the {@link UUID} id of the User.
   * @param profileId the {@link UUID} id of the Profile.
   * @param advertiserId the {@link UUID} id of the Advertiser.
   * @param username the username of the User.
   * @param password the encoded password of the User.
   * @param authoritiesList the list of authorities of the User.
   * @param attributes additional attributes obtained from an OAuth2 authentication provider.
   */
  public UserPrincipal(
      UUID userId,
      UUID profileId,
      UUID advertiserId,
      String username,
      String password,
      Collection<? extends GrantedAuthority> authoritiesList,
      Map<String, Object> attributes) {
    this.id = userId;
    this.profileId = profileId;
    this.advertiserId = advertiserId;
    this.username = username;
    this.password = password;
    this.authorities = authoritiesList;
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

  @Override
  public boolean isAccountNonExpired() {
    return accountNoExpired;
  }

  @Override
  public boolean isAccountNonLocked() {
    return accountNoLocked;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return credentialNoExpired;
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
    result = 31 * result + Boolean.hashCode(accountNoExpired);
    result = 31 * result + Boolean.hashCode(accountNoLocked);
    result = 31 * result + Boolean.hashCode(credentialNoExpired);
    result = 31 * result + (authorities != null ? authorities.hashCode() : 0);
    result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
    return result;
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof UserPrincipal that)) return false;
    return enabled == that.enabled
        && accountNoExpired == that.accountNoExpired
        && accountNoLocked == that.accountNoLocked
        && credentialNoExpired == that.credentialNoExpired
        && username != null
        && username.equals(that.username)
        && password != null
        && password.equals(that.password)
        && (authorities == that.authorities || authorities.equals(that.authorities))
        && (profileId == that.profileId || profileId.equals(that.profileId))
        && (advertiserId == that.advertiserId || advertiserId.equals(that.advertiserId));
  }
}
