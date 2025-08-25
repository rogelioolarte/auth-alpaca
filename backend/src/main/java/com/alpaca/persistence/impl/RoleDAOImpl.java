package com.alpaca.persistence.impl;

import com.alpaca.entity.Role;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IRoleDAO;
import com.alpaca.repository.GenericRepo;
import com.alpaca.repository.RolePermissionRepo;
import com.alpaca.repository.RoleRepo;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementation of the {@link IRoleDAO} interface for managing {@link Role} entities. This class
 * extends the generic DAO implementation ({@link GenericDAOImpl}) to provide standard CRUD
 * operations along with custom role-specific persistence logic.
 */
@Component
@RequiredArgsConstructor
public class RoleDAOImpl extends GenericDAOImpl<Role, UUID> implements IRoleDAO {

    private final RoleRepo repo;
    private final RolePermissionRepo rolePermissionRepo;

    /**
     * Provides the specific repository used by the generic DAO system.
     *
     * @return the {@link GenericRepo} implementation for {@link Role}
     */
    @Override
    protected GenericRepo<Role, UUID> getRepo() {
        return repo;
    }

    /**
     * Returns the class object representing the entity managed by this DAO.
     *
     * @return {@code Role.class}
     */
    @Override
    protected Class<Role> getEntity() {
        return Role.class;
    }

    /**
     * Retrieves a {@link Role} entity by its role name.
     *
     * @param roleName the name of the role to search for; may be {@code null} or blank
     * @return an {@link Optional} containing the found role, or empty if not found or invalid input
     */
    @Override
    public Optional<Role> findByRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return Optional.empty();
        }
        return repo.findByRoleName(roleName);
    }

    /**
     * Updates an existing {@link Role} identified by the given ID with the non-null, non-blank
     * properties from the supplied {@code role} object. Only changed fields are applied. Throws
     * {@link NotFoundException} if no existing entity is found.
     *
     * @param role the role object containing updated values
     * @param id the unique identifier of the role to update
     * @return the updated and saved {@link Role} instance
     * @throws NotFoundException if no role exists with the specified ID
     */
    @Override
    public Role updateById(Role role, UUID id) {
        Role existingRole =
                findById(id)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                String.format(
                                                        "%s with ID %s not found",
                                                        getEntity().getName(), id.toString())));

        if (role.getRoleName() != null && !role.getRoleName().isBlank()) {
            existingRole.setRoleName(role.getRoleName());
        }
        if (role.getRoleDescription() != null && !role.getRoleDescription().isBlank()) {
            existingRole.setRoleDescription(role.getRoleDescription());
        }
        if (role.getRolePermissions() != null && !role.getRolePermissions().isEmpty()) {
            existingRole.setRolePermissions(role.getPermissions());
        }
        return save(existingRole);
    }

    /**
     * Determines whether a role already exists based on its unique properties. Both role name and
     * description are required to be non-null and non-blank to perform the check.
     *
     * @param role the role object whose uniqueness is to be verified
     * @return {@code true} if a role with the same name exists; {@code false} otherwise or if input
     *     is invalid
     */
    @Override
    public boolean existsByUniqueProperties(Role role) {
        if (role.getRoleName() == null
                || role.getRoleName().isBlank()
                || role.getRoleDescription() == null
                || role.getRoleDescription().isBlank()) {
            return false;
        }
        return repo.existsByRoleName(role.getRoleName());
    }
}
