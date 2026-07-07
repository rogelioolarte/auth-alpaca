package com.alpaca.entity;

import com.alpaca.utils.GeneratorUUIDv7;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
@EntityListeners(AuditingEntityListener.class)
public class UserRole {

    /**
     * Unique identifier for the UserRole association. This value is automatically generated using a
     * UUID strategy.
     */
    @Id
    @GeneratorUUIDv7
    @Column(name = "id")
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
     * The date and time when the entity was first persisted. This field is managed automatically by
     * Spring Data JPA Auditing. It is set on creation and cannot be updated afterward.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

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
