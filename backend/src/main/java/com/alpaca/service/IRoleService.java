package com.alpaca.service;

import com.alpaca.entity.Role;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;

import java.util.Set;
import java.util.UUID;

/**
 * Service interface for managing {@code Role} entities.
 * Extends {@link IGenericService} to inherit common CRUD operations.
 *
 * @see IGenericService
 */
public interface IRoleService extends IGenericService<Role, UUID> {

    /**
     * Finds a role by its name.
     *
     * @param roleName The name of the role - must not be null.
     * @return The {@code Role} entity if found.
     * @throws BadRequestException if the roleName is null.
     * @throws NotFoundException   if the entity is not found.
     */
    Role findByRoleName(String roleName);

    /**
     * Retrieves the set of roles assigned to the current user.
     *
     * @return A {@code Set} containing the user's roles.
     * @throws NotFoundException    if the entity is not found.
     */
    Set<Role> getUserRoles();
}
