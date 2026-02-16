package com.alpaca.entity;

import com.alpaca.utils.GeneratorUUIDv7;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import lombok.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Represents a User entity in the system. This entity is mapped to the "users" table in the
 * database and stores authentication and authorization details. It includes information about
 * roles, entity status, and authentication settings.
 */
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
    @Column(name = "user_id")
    private UUID id;

    /** The User's email address. This field is unique and cannot be null. */
    @Column(name = "email", unique = true, nullable = false)
    private String email;

    /** The User's encrypted password. This field cannot be null. */
    @Column(name = "password", nullable = false)
    private String password;

    /** Indicates whether the User's entity is enabled. Defaults to {@code true}. */
    @Builder.Default
    @Column(name = "enable", nullable = false)
    private boolean enabled = true;

    /** Indicates whether the User's entity is not expired. Defaults to {@code true}. */
    @Builder.Default
    @Column(name = "account_no_expired", nullable = false)
    private boolean accountNoExpired = true;

    /** Indicates whether the User's entity is not locked. Defaults to {@code true}. */
    @Builder.Default
    @Column(name = "account_no_locked", nullable = false)
    private boolean accountNoLocked = true;

    /** Indicates whether the User's credentials are not expired. Defaults to {@code true}. */
    @Builder.Default
    @Column(name = "credential_no_expired", nullable = false)
    private boolean credentialNoExpired = true;

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
            fetch = FetchType.EAGER,
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
        this.accountNoExpired = true;
        this.accountNoLocked = true;
        this.credentialNoExpired = true;
        this.emailVerified = emailVerified;
        this.googleConnected = googleConnected;
        this.userRoles = rolesToUserRoles(roles);
    }

    public void setUserRoles(Collection<Role> roles) {
        if (roles != null && !roles.isEmpty()) {
            this.userRoles = rolesToUserRoles(roles);
        } else {
            this.userRoles = new HashSet<>();
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
        if (roles.isEmpty()) return Collections.emptySet();
        Set<UserRole> newUserRoles = HashSet.newHashSet(roles.size());
        if (roles.size() == 1) {
            newUserRoles.add(new UserRole(this, roles.iterator().next()));
        } else {
            for (Role role : roles) {
                newUserRoles.add(new UserRole(this, role));
            }
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
    public List<SimpleGrantedAuthority> getAuthorities() {
        if (userRoles == null || userRoles.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> seen = new HashSet<>();
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (UserRole userRole : userRoles) {
            String roleKey = ROLE_KEY_AUTHORITY + userRole.getRole().getRoleName();
            if (seen.add(roleKey)) {
                authorities.add(new SimpleGrantedAuthority(roleKey));
            }
            for (Permission permission : userRole.getRole().getPermissions()) {
                String permKey = permission.getPermissionName();
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
                && this.accountNoExpired
                && this.accountNoLocked
                && this.credentialNoExpired;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return enabled == user.enabled
                && accountNoExpired == user.accountNoExpired
                && accountNoLocked == user.accountNoLocked
                && credentialNoExpired == user.credentialNoExpired
                && emailVerified == user.emailVerified
                && googleConnected == user.googleConnected
                && Objects.equals(email, user.email)
                && Objects.equals(password, user.password)
                && Objects.equals(tokensInvalidBefore, user.tokensInvalidBefore)
                && Objects.equals(userRoles, user.userRoles)
                && Objects.equals(profile.getId(), user.profile.getId())
                && Objects.equals(advertiser.getId(), user.advertiser.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                email,
                password,
                enabled,
                accountNoExpired,
                accountNoLocked,
                credentialNoExpired,
                emailVerified,
                googleConnected,
                tokensInvalidBefore,
                userRoles,
                profile.getId(),
                advertiser.getId());
    }
}
