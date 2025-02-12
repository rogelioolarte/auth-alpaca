package com.example.entity;

import com.example.entity.intermediate.RolePermission;
import com.example.entity.intermediate.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

/**
 * Represents a Role entity in the system.
 * This entity is mapped to the "roles" table in the database
 * and defines user roles with specific permissions.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "roles")
public class Role {

    /**
     * Unique identifier for the Role.
     * This value is automatically generated using a UUID strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "role_id")
    private UUID id;

    /**
     * The name of the Role.
     * This field is unique and cannot be null.
     */
    @Column(name = "role_name", nullable = false, unique = true)
    private String roleName;

    /**
     * A brief description of the Role.
     * This field is unique and cannot be null.
     */
    @Column(name = "role_description", nullable = false, unique = true)
    private String roleDescription;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RolePermission> rolePermissions = new HashSet<>();

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserRole> userRoles = new HashSet<>();

    /**
     * Constructs an instance of a new Role object with the specified attributes.
     * The generated object is ready to be used and stored in the database.
     *
     * @param roleName        Name of the Role - must not be null
     * @param roleDescription Short description of the Role - must not be null
     * @param permissions     Set of permissions associated with this Role
     */
    public Role(String roleName, String roleDescription, Set<Permission> permissions) {
        this.roleName = roleName;
        this.roleDescription = roleDescription;
        this.rolePermissions = permissionsToRolePermissions(permissions);
    }

    public Set<RolePermission> permissionsToRolePermissions(Set<Permission> permissions) {
        Set<RolePermission> rolePermissions = new HashSet<>(permissions.size());
        for(Permission permission: permissions) {
            rolePermissions.add(new RolePermission(this, permission));
        }
        return rolePermissions;
    }

    public List<Permission> getPermissions() {
        List<Permission> permissions = new ArrayList<>(getRolePermissions().size());
        for(RolePermission rolePermission : getRolePermissions()) {
            permissions.add(rolePermission.getPermission());
        }
        return permissions;
    }

}
