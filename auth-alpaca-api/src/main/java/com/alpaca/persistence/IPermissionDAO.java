package com.alpaca.persistence;

import com.alpaca.entity.Permission;
import java.util.Optional;
import java.util.UUID;

/**
 * Data Access Object (DAO) interface for managing {@code Permission} entities. Extends {@link
 * IGenericDAO} to inherit common CRUD operations.
 *
 * @see IGenericDAO
 */
public interface IPermissionDAO extends IGenericDAO<Permission, UUID> {

    /**
     * Looks up a permission by its unique name.
     *
     * @param permissionName the permission name - must not be null
     * @return An {@link Optional} containing the matching permission if found, otherwise empty
     */
    Optional<Permission> findByPermissionName(String permissionName);
}
