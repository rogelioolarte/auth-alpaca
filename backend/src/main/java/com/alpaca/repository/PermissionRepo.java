package com.alpaca.repository;

import com.alpaca.entity.Permission;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link Permission} entities.
 *
 * <p>Extends {@link CustomRepo} to inherit common CRUD operations and defines additional queries
 * for permission-specific operations.
 *
 * @see CustomRepo
 */
@Repository
public interface PermissionRepo extends CustomRepo<Permission, UUID> {

    /**
     * Checks whether a permission with the specified name exists.
     *
     * @param name The name of the permission - must not be null.
     * @return {@code true} if a permission with the given name exists, {@code false} otherwise.
     */
    boolean existsByName(String name);

    /**
     * Retrieves a permission by their permission name.
     *
     * @param name The permission name of the permission - must not be null.
     * @return An {@link Optional} containing the permission if found, otherwise empty.
     */
    Optional<Permission> findByName(String name);
}
