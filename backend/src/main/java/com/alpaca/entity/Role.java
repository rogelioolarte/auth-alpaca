package com.alpaca.entity;

import com.alpaca.utils.GeneratorUUIDv7;
import jakarta.persistence.*;
import java.util.*;
import lombok.*;

/**
 * Represents a Role entity in the system. This entity is mapped to the "roles" table in the
 * database and defines user roles with specific permissions.
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "roles")
public class Role extends Auditable {

    /**
     * Unique identifier for the Role. This value is automatically generated using a UUID strategy.
     */
    @Id
    @GeneratorUUIDv7
    @Column(name = "id")
    private UUID id;

    /** The name of the Role. This field is unique and cannot be null. */
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    /** A brief description of the Role. This field is unique and cannot be null. */
    @Column(name = "description", nullable = false, unique = true)
    private String description;

    /**
     * Indicates the set of Permission has the Role.
     *
     * <p>A Role has a many-to-many relationship with an {@link Permission} through {@link
     * RolePermission}
     */
    @Builder.Default
    @OneToMany(
            mappedBy = "role",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private Set<RolePermission> rolePermissions = new HashSet<>();

    /**
     * Indicates the set of User has the Role.
     *
     * <p>A Role has a many-to-many relationship with an {@link User} through {@link UserRole}
     */
    @Builder.Default
    @OneToMany(
            mappedBy = "role",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private Set<UserRole> userRoles = new HashSet<>();

    /**
     * Constructs an instance of a new Role object with the specified attributes. The generated
     * object is ready to be used and stored in the database.
     *
     * @param name Name of the Role - must not be null
     * @param description Short description of the Role - must not be null
     * @param permissions Set of permissions associated with this Role
     */
    public Role(String name, String description, Set<Permission> permissions) {
        this.name = name;
        this.description = description;
        this.rolePermissions = permissionsToRolePermissions(permissions);
    }

    /**
     * Replaces all permission assignments for this role with the given permissions.
     *
     * <p>Clears existing {@link RolePermission} associations and rebuilds them from the provided
     * collection. Has no effect if the collection is null or empty.
     *
     * @param permissions the collection of permissions to assign; may be empty or {@code null}
     *     (no-op)
     */
    public void setRolePermissions(Collection<Permission> permissions) {
        if (this.rolePermissions == null) {
            this.rolePermissions = new HashSet<>();
        }
        if (permissions != null && !permissions.isEmpty()) {
            this.rolePermissions.clear();
            Set<RolePermission> newRolePermissions = permissionsToRolePermissions(permissions);
            this.rolePermissions.addAll(newRolePermissions);
        }
    }

    /**
     * Converts a set of {@link Permission} objects into a set of {@link RolePermission} objects
     * associated with this Role.
     *
     * @param permissions the set of {@link Permission} objects to be converted.
     * @return a set of {@link RolePermission} objects associated with this Role.
     */
    public Set<RolePermission> permissionsToRolePermissions(Collection<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) return Collections.emptySet();
        Set<RolePermission> rolePermissionSet = HashSet.newHashSet(permissions.size());
        for (Permission permission : permissions) {
            rolePermissionSet.add(new RolePermission(this, permission));
        }
        return rolePermissionSet;
    }

    /**
     * Retrieves the list of permissions assigned to this Role.
     *
     * @return a list of {@link Permission} objects associated with this Role.
     */
    public List<Permission> getPermissions() {
        if (rolePermissions.isEmpty()) return Collections.emptyList();
        List<Permission> permissions = new ArrayList<>(rolePermissions.size());
        for (RolePermission rolePermission : getRolePermissions()) {
            permissions.add(rolePermission.getPermission());
        }
        return permissions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role role)) return false;
        return Objects.equals(name, role.name) && Objects.equals(description, role.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description);
    }
}
