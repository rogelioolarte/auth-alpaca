package com.example.entity;

import com.example.entity.intermediate.RolePermission;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a Permission entity in the system.
 * This entity is used to manage user permissions
 * and is mapped to the "permissions" table in the database.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "permissions")
public class Permission {

    /**
     * Unique identifier for the Permission.
     * This value is automatically generated using a UUID strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "permission_id")
    private UUID id;

    /**
     * The name of the Permission.
     * This field is unique and cannot be null.
     */
    @Column(name = "permission_name", unique = true, nullable = false)
    private String permissionName;

    @OneToMany(mappedBy = "permission", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RolePermission> rolePermissions = new HashSet<>();

    /**
     * Constructs an instance of a new Permission object with the specified attributes.
     * The generated object is ready to be used and stored in the database.
     *
     * @param permissionName Name of the Permission - must not be null
     */
    public Permission(String permissionName) {
        this.permissionName = permissionName;
    }
}
