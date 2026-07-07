package com.alpaca.entity;

import com.alpaca.utils.GeneratorUUIDv7;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.*;

/**
 * Represents a User entity in the system. This entity is mapped to the "users" table in the
 * database and stores authentication and authorization details. It includes information about
 * roles, entity status, and authentication settings.
 */
@NamedEntityGraph(
        name = "User.withAuthorities",
        attributeNodes = {
            @NamedAttributeNode("profile"),
            @NamedAttributeNode("advertiser"),
            @NamedAttributeNode(value = "userRoles", subgraph = "userRoles-subgraph")
        },
        subgraphs = {
            @NamedSubgraph(
                    name = "userRoles-subgraph",
                    attributeNodes = {
                        @NamedAttributeNode(value = "role", subgraph = "role-subgraph")
                    }),
            @NamedSubgraph(
                    name = "role-subgraph",
                    attributeNodes = {
                        @NamedAttributeNode(
                                value = "rolePermissions",
                                subgraph = "permission-subgraph")
                    }),
            @NamedSubgraph(
                    name = "permission-subgraph",
                    attributeNodes = {@NamedAttributeNode("permission")})
        })
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User extends Auditable {

    /**
     * Unique identifier for the User. This value is automatically generated using a UUID strategy.
     */
    @Id
    @GeneratorUUIDv7
    @Column(name = "id")
    private UUID id;

    /** The User's email address. This field is unique and cannot be null. */
    @Column(name = "email", unique = true, nullable = false)
    private String email;

    /** The User's encrypted password. This field can be null. */
    @Column(name = "password")
    private String password;

    /** Indicates whether the User's entity is enabled. Defaults to {@code true}. */
    @Builder.Default
    @Column(name = "enable", nullable = false)
    private boolean enabled = true;

    /** Indicates whether the User's entity is not expired. Defaults to {@code true}. */
    @Builder.Default
    @Column(name = "account_non_expired", nullable = false)
    private boolean accountNonExpired = true;

    /** Indicates whether the User's entity is not locked. Defaults to {@code true}. */
    @Builder.Default
    @Column(name = "account_non_locked", nullable = false)
    private boolean accountNonLocked = true;

    /** Indicates whether the User's credentials are not expired. Defaults to {@code true}. */
    @Builder.Default
    @Column(name = "credential_non_expired", nullable = false)
    private boolean credentialNonExpired = true;

    /** Indicates whether the User's email has been verified. Defaults to {@code false}. */
    @Builder.Default
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    /**
     * Indicates whether the User has connected their account to Google. Defaults to {@code false}.
     */
    @Builder.Default
    @Column(name = "google_connected", nullable = false)
    private boolean googleConnected = false;

    /**
     * Timestamp before which all issued tokens (access or refresh) for this user should be
     * considered invalid.
     *
     * <p>Useful for enforcing global logout or invalidating all sessions/tokens when a critical
     * change happens, such as password reset, role changes or security incident. Any token created
     * before this instant should be rejected.
     */
    @Column(name = "tokens_invalid_before")
    private Instant tokensInvalidBefore;

    /**
     * Indicates the set of Role has the User.
     *
     * <p>A User has a many-to-many relationship with an {@link Role} through {@link UserRole}
     */
    @Builder.Default
    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    private Set<UserRole> userRoles = new HashSet<>();

    /**
     * The Profile entity associated with the User.
     *
     * <p>A User has a one-to-one relationship with a {@link Profile}.
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.MERGE, orphanRemoval = true)
    private Profile profile;

    /**
     * The Advertiser entity associated with the User.
     *
     * <p>A User has a one-to-one relationship with an {@link Advertiser}.
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.MERGE, orphanRemoval = true)
    private Advertiser advertiser;

    /**
     * The set of refresh tokens associated with this user across sessions/families.
     *
     * <p>Used for managing, revoking or auditing refresh tokens when implementing rotation and
     * reuse-detection.
     */
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RefreshToken> refreshTokens = new HashSet<>();

    /**
     * The set of session / token-family records associated with this user.
     *
     * <p>Each session typically represents a logical login context (device, client, browser, etc.).
     * This allows tracking active sessions, revoking an entire session (all its tokens), and
     * auditing session metadata.
     */
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Session> sessions = new HashSet<>();

    /**
     * Prefix prepended to role names when building Spring Security {@link GrantedAuthority}
     * entries.
     */
    public static final String ROLE_KEY_AUTHORITY = "ROLE_";

    /**
     * Constructs an instance of a new User object with the specified attributes. The generated
     * object is ready to be used by default.
     *
     * @param email the User's email - must not be null
     * @param password the User's encrypted password - must not be null
     * @param roles Set of Roles assigned to the User
     */
    public User(String email, String password, Set<Role> roles) {
        this.email = email;
        this.password = password;
        this.userRoles = rolesToUserRoles(roles);
        this.enabled = true;
        this.accountNonExpired = true;
        this.accountNonLocked = true;
        this.credentialNonExpired = true;
    }

    /**
     * Constructs an instance of a new User object with the specified attributes. The generated
     * object is ready to be used and stored in the database.
     *
     * @param email the User's email - must not be null
     * @param password the User's encrypted password - must not be null
     * @param emailVerified indicates whether the email has been verified
     * @param googleConnected indicates whether the User has connected with Google
     * @param roles Set of Roles assigned to the User
     */
    public User(
            String email,
            String password,
            boolean emailVerified,
            boolean googleConnected,
            Set<Role> roles) {
        this.email = email;
        this.password = password;
        this.enabled = true;
        this.accountNonExpired = true;
        this.accountNonLocked = true;
        this.credentialNonExpired = true;
        this.emailVerified = emailVerified;
        this.googleConnected = googleConnected;
        this.userRoles = rolesToUserRoles(roles);
    }

    /**
     * Replaces all role assignments for this user with the given roles.
     *
     * <p>Clears existing {@link UserRole} associations and rebuilds them from the provided
     * collection. Has no effect if the collection is null or empty.
     *
     * @param roles the collection of roles to assign; may be empty or {@code null} (no-op)
     */
    public void setRoles(Collection<Role> roles) {
        if (roles != null && !roles.isEmpty()) {
            this.userRoles.clear();
            Set<UserRole> newUserRoles = rolesToUserRoles(roles);
            this.userRoles.addAll(newUserRoles);
        }
    }

    /**
     * Converts a set of {@link Role} objects into a set of {@link UserRole} objects associated with
     * the current User.
     *
     * @param roles the set of {@link Role} objects to be converted.
     * @return a set of {@link UserRole} objects associated with the User.
     */
    private Set<UserRole> rolesToUserRoles(Collection<Role> roles) {
        if (roles == null || roles.isEmpty()) return Collections.emptySet();
        Set<UserRole> newUserRoles = HashSet.newHashSet(roles.size());
        for (Role role : roles) {
            newUserRoles.add(new UserRole(this, role));
        }
        return newUserRoles;
    }

    /**
     * Retrieves the list of roles assigned to the User.
     *
     * @return a List of {@link Role} objects representing the User's roles.
     */
    public List<Role> getRoles() {
        if (userRoles.isEmpty()) return Collections.emptyList();
        List<Role> roles = new ArrayList<>(userRoles.size());
        if (userRoles.size() == 1) {
            roles.add(userRoles.iterator().next().getRole());
        } else {
            for (UserRole userRole : userRoles) {
                roles.add(userRole.getRole());
            }
        }
        return roles;
    }

    /**
     * Retrieves the authorities granted and Permissions to the User based on their assigned roles
     * and permissions. Each role is prefixed with "ROLE_" as per Spring Security conventions. Each
     * role is prefixed with "PERMISSION_" as per Spring Security conventions.
     *
     * @return List of {@link SimpleGrantedAuthority} representing the User's permissions and roles.
     */
    public List<GrantedAuthority> getAuthorities() {
        if (userRoles == null || userRoles.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> seen = new HashSet<>();
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (UserRole userRole : userRoles) {
            String roleKey = ROLE_KEY_AUTHORITY + userRole.getRole().getName();
            if (seen.add(roleKey)) {
                authorities.add(new SimpleGrantedAuthority(roleKey));
            }
            for (Permission permission : userRole.getRole().getPermissions()) {
                String permKey = permission.getName();
                if (seen.add(permKey)) {
                    authorities.add(new SimpleGrantedAuthority(permKey));
                }
            }
        }
        return authorities;
    }

    /**
     * Determines whether the user account is allowed to be used based on its status.
     *
     * <p>This method checks multiple conditions to determine if the account is active and can be
     * used for authentication. A user account is considered allowed if:
     *
     * <ul>
     *   <li>The account is enabled.
     *   <li>The account is not expired.
     *   <li>The account is not locked.
     *   <li>The credentials are not expired.
     * </ul>
     *
     * @return {@code true} if the account meets all the conditions to be considered active; {@code
     *     false} otherwise.
     */
    public boolean isAllowUser() {
        return this.enabled
                && this.accountNonExpired
                && this.accountNonLocked
                && this.credentialNonExpired;
    }

    /**
     * Toggles all account-status flags as a group.
     *
     * <p>Sets {@code enabled}, {@code accountNonExpired}, {@code accountNonLocked}, and {@code
     * credentialNonExpired} to the same value. Use this when the entire account should be
     * universally allowed or suspended, rather than toggling individual flags.
     *
     * @param value the common value for all account-status flags
     */
    public void setAllowed(boolean value) {
        this.enabled = value;
        this.accountNonExpired = value;
        this.accountNonLocked = value;
        this.credentialNonExpired = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return enabled == user.enabled
                && accountNonExpired == user.accountNonExpired
                && accountNonLocked == user.accountNonLocked
                && credentialNonExpired == user.credentialNonExpired
                && emailVerified == user.emailVerified
                && googleConnected == user.googleConnected
                && Objects.equals(email, user.email)
                && Objects.equals(password, user.password)
                && Objects.equals(tokensInvalidBefore, user.tokensInvalidBefore);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                email,
                password,
                enabled,
                accountNonExpired,
                accountNonLocked,
                credentialNonExpired,
                emailVerified,
                googleConnected,
                tokensInvalidBefore);
    }

    /**
     * Merges profile data from the given user reference into this user.
     *
     * <p>Only updates if the new profile is non-null and its ID differs from the current profile's
     * ID, preventing unnecessary entity graph churn.
     *
     * @param newUser the reference user whose profile may be merged into this user
     */
    public void updateProfile(User newUser) {
        if (newUser.getProfile() != null
                && !Objects.equals(this.getProfile(), newUser.getProfile())) {
            UUID currProfileId = this.getProfile() != null ? this.getProfile().getId() : null;
            if (!Objects.equals(newUser.getProfile().getId(), currProfileId)) {
                this.setProfile(newUser.getProfile());
            }
        }
    }

    /**
     * Merges advertiser data from the given user reference into this user.
     *
     * <p>Only updates if the new advertiser is non-null and its ID differs from the current
     * advertiser's ID, preventing unnecessary entity graph churn.
     *
     * @param newUser the reference user whose advertiser may be merged into this user
     */
    public void updateAdvertiser(User newUser) {
        if (newUser.getAdvertiser() != null
                && !Objects.equals(this.getAdvertiser(), newUser.getAdvertiser())) {
            UUID currAdvertiserId =
                    this.getAdvertiser() != null ? this.getAdvertiser().getId() : null;
            if (!Objects.equals(newUser.getAdvertiser().getId(), currAdvertiserId)) {
                this.setAdvertiser(newUser.getAdvertiser());
            }
        }
    }
}
