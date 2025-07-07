package com.alpaca.entity.intermediate;

import com.alpaca.entity.Role;
import com.alpaca.entity.User;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the association between a {@link User} and a {@link Role}.
 *
 * <p>This entity defines a many-to-many relationship between users and roles and is mapped to the
 * "user_roles" table in the database.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_roles")
public class UserRole {

    /**
     * Unique identifier for the UserRole association. This value is automatically generated using a
     * UUID strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_role_id")
    private UUID id;

    /** The User associated with this UserRole. This field cannot be null. */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** The Role associated with this UserRole. This field cannot be null. */
    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    /**
     * Constructs an instance of a new UserRole object with the specified attributes.
     *
     * @param user the User associated with this UserRole - must not be null
     * @param role the Role associated with this UserRole - must not be null
     */
    public UserRole(User user, Role role) {
        this.user = user;
        this.role = role;
    }
}
