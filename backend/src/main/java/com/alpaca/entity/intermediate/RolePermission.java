package com.alpaca.entity.intermediate;

import com.alpaca.entity.Permission;
import com.alpaca.entity.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Represents the association between a {@link Role} and a {@link Permission}.
 * <p>
 * This entity defines a many-to-many relationship between roles and permissions
 * and is mapped to the "role_permissions" table in the database.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "role_permissions")
public class RolePermission {

    /**
     * Unique identifier for the RolePermission association.
     * This value is automatically generated using a UUID strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "role_permission_id")
    private UUID id;

    /**
     * The Role associated with this RolePermission.
     * This field cannot be null.
     */
    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    /**
     * The Permission associated with this RolePermission.
     * This field cannot be null.
     */
    @ManyToOne
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    /**
     * Constructs an instance of a new RolePermission object with the specified attributes.
     *
     * @param role       the Role associated with this RolePermission - must not be null
     * @param permission the Permission associated with this RolePermission - must not be null
     */
    public RolePermission(Role role, Permission permission) {
        this.role = role;
        this.permission = permission;
    }
}