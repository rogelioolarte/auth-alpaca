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
 * Represents the association between a {@link Role} and a {@link Permission}.
 *
 * <p>This entity defines a many-to-many relationship between roles and permissions and is mapped to
 * the "role_permissions" table in the database.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "role_permissions")
@EntityListeners(AuditingEntityListener.class)
public class RolePermission {

    /**
     * Unique identifier for the RolePermission association. This value is automatically generated
     * using a UUID strategy.
     */
    @Id
    @GeneratorUUIDv7
    @Column(name = "id")
    private UUID id;

    /** The Role associated with this RolePermission. This field cannot be null. */
    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    /** The Permission associated with this RolePermission. This field cannot be null. */
    @ManyToOne
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    /**
     * The date and time when the entity was first persisted. This field is managed automatically by
     * Spring Data JPA Auditing. It is set on creation and cannot be updated afterward.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Constructs an instance of a new RolePermission object with the specified attributes.
     *
     * @param role the Role associated with this RolePermission - must not be null
     * @param permission the Permission associated with this RolePermission - must not be null
     */
    public RolePermission(Role role, Permission permission) {
        this.role = role;
        this.permission = permission;
    }
}
