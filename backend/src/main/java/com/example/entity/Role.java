package com.example.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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

    /**
     * The set of permissions associated with this Role.
     * A Role can have multiple permissions, forming a many-to-many relationship.
     */
    @ManyToMany(cascade = CascadeType.MERGE)
    @JoinTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions = new HashSet<>();

    /**
     * The set of users associated with this Role.
     * A Role can have multiple users, forming a many-to-many relationship.
     */
    @ManyToMany(mappedBy = "roles")
    private Set<User> users = new HashSet<>();

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
        this.permissions = permissions;
    }
}
