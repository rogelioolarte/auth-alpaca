package com.alpaca.persistence.impl;

import com.alpaca.entity.Permission;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IPermissionDAO;
import com.alpaca.repository.GenericRepo;
import com.alpaca.repository.PermissionRepo;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementation of the {@link IPermissionDAO} interface for managing {@link Permission} entities.
 * Extends the generic DAO implementation ({@link GenericDAOImpl}) to provide standard CRUD
 * operations and permission-specific persistence logic.
 */
@Component
@RequiredArgsConstructor
public class PermissionDAOImpl extends GenericDAOImpl<Permission, UUID> implements IPermissionDAO {

    private final PermissionRepo repo;

    /**
     * Provides the repository used by the generic DAO framework.
     *
     * @return the {@link GenericRepo} for {@link Permission}
     */
    @Override
    protected GenericRepo<Permission, UUID> getRepo() {
        return repo;
    }

    /**
     * Returns the {@link Permission} entity class managed by this DAO.
     *
     * @return {@code Permission.class}
     */
    @Override
    protected Class<Permission> getEntity() {
        return Permission.class;
    }

    /**
     * Updates an existing {@link Permission} identified by the given ID with the non-null and
     * non-blank values from the provided {@code permission} object. Only changed fields are
     * applied. Throws a {@link NotFoundException} if no matching entity is found.
     *
     * @param permission the permission object containing updated values
     * @param id the unique identifier of the permission to update
     * @return the updated and saved {@link Permission} instance
     * @throws NotFoundException if no permission exists with the specified ID
     */
    @Override
    public Permission updateById(Permission permission, UUID id) {
        Permission existingPermission =
                findById(id)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                String.format(
                                                        "%s with ID %s not found",
                                                        getEntity().getName(), id.toString())));

        if (permission.getPermissionName() != null && !permission.getPermissionName().isBlank()) {
            existingPermission.setPermissionName(permission.getPermissionName());
        }

        return save(existingPermission);
    }

    /**
     * Determines whether a permission with the same name already exists.
     *
     * @param permission the permission to check; its name must be non-null and non-blank
     * @return {@code true} if a permission with the same name exists; {@code false} otherwise
     */
    @Override
    public boolean existsByUniqueProperties(Permission permission) {
        if (permission.getPermissionName() == null || permission.getPermissionName().isBlank()) {
            return false;
        }
        return repo.existsByPermissionName(permission.getPermissionName());
    }

    /**
     * Searches for a {@link Permission} entity by its permission name.
     *
     * @param permissionName the name of the permission to search for; may be null or blank
     * @return an {@link Optional} containing the found permission, or empty if none found
     */
    @Override
    public Optional<Permission> findByPermissionName(String permissionName) {
        return repo.findByPermissionName(permissionName);
    }
}
