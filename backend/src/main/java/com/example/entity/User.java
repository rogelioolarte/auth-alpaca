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
 * Entity Class User used for database.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID id;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "enable", nullable = false)
    private boolean enabled = true;

    @Column(name = "account_no_expired", nullable = false)
    private boolean accountNoExpired = true;

    @Column(name = "account_no_locked", nullable = false)
    private boolean accountNoLocked = true;

    @Column(name = "credential_no_expired", nullable = false)
    private boolean credentialNoExpired = true;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "google_connected", nullable = false)
    private boolean googleConnected = false;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.MERGE, fetch = FetchType.LAZY, orphanRemoval = true)
    private Profile profile;

    @OneToOne(mappedBy = "user", cascade = CascadeType.MERGE, fetch = FetchType.LAZY, orphanRemoval = true)
    private Advertiser advertiser;

    /**
     * Instantiates a new User ready to use and save for the database.
     *
     * @param email               the email
     * @param password            the password
     * @param enabled             the enabled
     * @param accountNoExpired    the account no expired
     * @param accountNoLocked     the account no locked
     * @param credentialNoExpired the credential no expired
     * @param emailVerified       the email verified
     * @param googleConnected     the google connected
     * @param roles               the roles
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
     * Gets authorities.
     *
     * @return the authorities
     */
    public Set<SimpleGrantedAuthority> getAuthorities() {
        Set<SimpleGrantedAuthority> authorities = new HashSet<>(roles.size());
        for (Role role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleName()));
//            for(Permission permission : role.getPermissions()) {
//                authorities.add((new SimpleGrantedAuthority(permission.getPermissionName())));
//            }
        }
        return authorities;
    }
}
