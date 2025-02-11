package com.example.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
     * The roles assigned to the User.
     * A User can have multiple roles, and roles are mapped through the "user_roles" join table.
     */
    @ManyToMany(fetch = FetchType.EAGER, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "role_id", nullable = false))
    private Set<Role> roles = new HashSet<>();

    /**
     * The Profile entity associated with the User.
     * A User has a one-to-one relationship with a {@link Profile}.
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.MERGE,
            fetch = FetchType.LAZY, orphanRemoval = true)
    private Profile profile;

    /**
     * The Advertiser entity associated with the User.
     * A User has a one-to-one relationship with an {@link Advertiser}.
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.MERGE,
            fetch = FetchType.LAZY, orphanRemoval = true)
    private Advertiser advertiser;

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
    public User(String email, String password, boolean enabled, boolean accountNoExpired,
                boolean accountNoLocked, boolean credentialNoExpired, boolean emailVerified,
                boolean googleConnected, Set<Role> roles) {
        this.email = email;
        this.password = password;
        this.enabled = enabled;
        this.accountNoExpired = accountNoExpired;
        this.accountNoLocked = accountNoLocked;
        this.credentialNoExpired = credentialNoExpired;
        this.emailVerified = emailVerified;
        this.googleConnected = googleConnected;
        this.roles = roles;
    }

    /**
     * Retrieves the authorities granted to the User based on their assigned roles.
     * Each role is prefixed with "ROLE_" as per Spring Security conventions.
     *
     * @return Set of {@link SimpleGrantedAuthority} representing the User's permissions.
     */
    public Set<SimpleGrantedAuthority> getAuthorities() {
        Set<SimpleGrantedAuthority> authorities = new HashSet<>(roles.size());
        for (Role role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleName()));
//             Uncomment the following code if permissions should be included in authorities
//             If we add this for cycle to the function it will have a time complexity of O(n^2)
//             for (Permission permission : role.getPermissions()) {
//                 authorities.add(new SimpleGrantedAuthority(permission.getPermissionName()));
//             }
        }
        return authorities;
    }
}
