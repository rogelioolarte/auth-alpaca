package com.alpaca.entity;

import com.alpaca.entity.intermediate.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.*;

/**
 * Represents a User entity in the system.
 * This entity is mapped to the "users" table in the database
 * and stores authentication and authorization details.
 * It includes information about roles, entity status, and authentication settings.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    /**
     * Unique identifier for the User.
     * This value is automatically generated using a UUID strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID id;

    /**
     * The User's email address.
     * This field is unique and cannot be null.
     */
    @Column(name = "email", unique = true, nullable = false)
    private String email;

    /**
     * The User's encrypted password.
     * This field cannot be null.
     */
    @Column(name = "password", nullable = false)
    private String password;

    /**
     * Indicates whether the User's entity is enabled.
     * Defaults to {@code true}.
     */
    @Column(name = "enable", nullable = false)
    private boolean enabled = true;

    /**
     * Indicates whether the User's entity is not expired.
     * Defaults to {@code true}.
     */
    @Column(name = "account_no_expired", nullable = false)
    private boolean accountNoExpired = true;

    /**
     * Indicates whether the User's entity is not locked.
     * Defaults to {@code true}.
     */
    @Column(name = "account_no_locked", nullable = false)
    private boolean accountNoLocked = true;

    /**
     * Indicates whether the User's credentials are not expired.
     * Defaults to {@code true}.
     */
    @Column(name = "credential_no_expired", nullable = false)
    private boolean credentialNoExpired = true;

    /**
     * Indicates whether the User's email has been verified.
     * Defaults to {@code false}.
     */
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    /**
     * Indicates whether the User has connected their account to Google.
     * Defaults to {@code false}.
     */
    @Column(name = "google_connected", nullable = false)
    private boolean googleConnected = false;

    /**
     * Indicates the set of Role has the User.
     * <p>
     * A User has a many-to-many relationship with an {@link Role} through {@link UserRole}
     * </p>
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserRole> userRoles = new HashSet<>();

    /**
     * The Profile entity associated with the User.
     * <p>
     * A User has a one-to-one relationship with a {@link Profile}.
     * </p>
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.MERGE,
            fetch = FetchType.LAZY, orphanRemoval = true)
    private Profile profile;

    /**
     * The Advertiser entity associated with the User.
     * <p>
     * A User has a one-to-one relationship with an {@link Advertiser}.
     * </p>
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.MERGE,
            fetch = FetchType.LAZY, orphanRemoval = true)
    private Advertiser advertiser;

    /**
     * Constructs an instance of a new User object with the specified attributes.
     * The generated object is ready to be used by default.
     *
     * @param email               the User's email - must not be null
     * @param password            the User's encrypted password - must not be null
     * @param roles               Set of Roles assigned to the User
     */
    public User(String email, String password, Set<Role> roles) {
        this.email = email;
        this.password = password;
        this.userRoles = rolesToUserRoles(roles);
    }

    /**
     * Constructs an instance of a new User object with the specified attributes.
     * The generated object is ready to be used and stored in the database.
     *
     * @param email               the User's email - must not be null
     * @param password            the User's encrypted password - must not be null
     * @param enabled             indicates whether the entity is enabled
     * @param accountNoExpired    indicates whether the entity is not expired
     * @param accountNoLocked     indicates whether the entity is not locked
     * @param credentialNoExpired indicates whether the credentials are not expired
     * @param emailVerified       indicates whether the email has been verified
     * @param googleConnected     indicates whether the User has connected with Google
     * @param roles               Set of Roles assigned to the User
     */
    public User(String email, String password, boolean enabled,
                boolean accountNoExpired, boolean accountNoLocked,
                boolean credentialNoExpired, boolean emailVerified,
                boolean googleConnected, Set<Role> roles) {
        this.email = email;
        this.password = password;
        this.enabled = enabled;
        this.accountNoExpired = accountNoExpired;
        this.accountNoLocked = accountNoLocked;
        this.credentialNoExpired = credentialNoExpired;
        this.emailVerified = emailVerified;
        this.googleConnected = googleConnected;
        this.userRoles = rolesToUserRoles(roles);
    }

    /**
     * Converts a set of {@link Role} objects into a set of {@link UserRole} objects
     * associated with the current User.
     *
     * @param roles the set of {@link Role} objects to be converted.
     * @return a set of {@link UserRole} objects associated with the User.
     */
    private Set<UserRole> rolesToUserRoles(Set<Role> roles) {
        if(roles.isEmpty()) return Collections.emptySet();
        Set<UserRole> userRoles = new HashSet<>(roles.size());
        if(roles.size() == 1) {
            userRoles.add(new UserRole(this, roles.iterator().next()));
        } else {
            for(Role role: roles) {
                userRoles.add(new UserRole(this, role));
            }
        }
        return userRoles;
    }

    /**
     * Retrieves the list of roles assigned to the User.
     *
     * @return a List of {@link Role} objects representing the User's roles.
     */
    public List<Role> getRoles() {
        if(getUserRoles().isEmpty()) return Collections.emptyList();
        List<Role> roles = new ArrayList<>(getUserRoles().size());
        if(getUserRoles().size() == 1) {
            roles.add(getUserRoles().iterator().next().getRole());
        } else {
            for(UserRole userRole : getUserRoles()) {
                roles.add(userRole.getRole());
            }
        }
        return roles;
    }

    /**
     * Retrieves the authorities granted to the User based on their assigned roles.
     * Each role is prefixed with "ROLE_" as per Spring Security conventions.
     *
     * @return Set of {@link SimpleGrantedAuthority} representing the User's permissions.
     */
    public Set<SimpleGrantedAuthority> getAuthorities() {
        if(userRoles.isEmpty()) return Collections.emptySet();
        Set<SimpleGrantedAuthority> authorities = new HashSet<>(userRoles.size());
        if(userRoles.size() == 1) {
            authorities.add(new SimpleGrantedAuthority(
                    "ROLE_" + userRoles.iterator().next().getRole().getRoleName()));
        } else {
            for (UserRole userRole : userRoles) {
                authorities.add(new SimpleGrantedAuthority(
                        "ROLE_" + userRole.getRole().getRoleName()));
//             Uncomment the following code if permissions should be included in authorities
//             If we add this for cycle to the function it will have a time complexity of O(n^2)
//             for (Permission permission : userRole.getRole().getPermissions()) {
//                 authorities.add(new SimpleGrantedAuthority(permission.getPermissionName()));
//             }
            }
        }
        return authorities;
    }
}
