package com.example.persistence;

import com.example.entity.Role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data Access Object (DAO) interface for managing {@link Role} entities.
 * <p>
 * Extends {@link IGenericDAO} to inherit common CRUD operations and
 * defines additional queries specific to {@code Role} management.
 * </p>
 *
 * @see IGenericDAO
 */
public interface IRoleDAO extends IGenericDAO<Role, UUID> {

    /**
     * Retrieves a list of roles associated with a specific permission.
     *
     * @param permissionId The unique identifier of the permission - must not be null.
     * @return A list of roles linked to the specified permission. Returns an empty list if no roles are found.
     */
    List<Role> findRolesByPermissionId(UUID permissionId);

    /**
     * Finds a role by its unique name.
     *
     * @param roleName The name of the role - must not be null.
     * @return An {@link Optional} containing the role if found, otherwise an empty {@link Optional}.
     */
    Optional<Role> findByRoleName(String roleName);
}
