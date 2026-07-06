package com.alpaca.entity;

import com.alpaca.utils.GeneratorUUIDv7;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.*;

/**
 * Represents a Permission entity in the system. This entity is used to manage user permissions and
 * is mapped to the "permissions" table in the database.
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "permissions")
public class Permission extends Auditable {

    /**
     * Unique identifier for the Permission. This value is automatically generated using a UUID
     * strategy.
     */
    @Id
    @GeneratorUUIDv7
    @Column(name = "id")
    private UUID id;

    /** The name of the Permission. This field is unique and cannot be null. */
    @Column(name = "name", unique = true, nullable = false)
    private String name;

    /**
     * Inverse side of the many-to-many relationship with {@link Role} through the join entity
     * {@link RolePermission}. Maintained for JPA cascade operations; typically not accessed
     * directly.
     */
    @Builder.Default
    @OneToMany(mappedBy = "permission", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RolePermission> rolePermissions = new HashSet<>();

    /**
     * Constructs an instance of a new Permission object with the specified attributes. The
     * generated object is ready to be used and stored in the database.
     *
     * @param name Name of the Permission - must not be null
     */
    public Permission(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permission that)) return false;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
