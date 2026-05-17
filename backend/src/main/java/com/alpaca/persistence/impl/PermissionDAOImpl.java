package com.alpaca.persistence.impl;

import com.alpaca.entity.Permission;
import com.alpaca.persistence.IPermissionDAO;
import com.alpaca.repository.GenericRepo;
import com.alpaca.repository.PermissionRepo;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import lombok.Generated;
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
    @Generated
    protected GenericRepo<Permission, UUID> getRepo() {
        return repo;
    }

    /**
     * Determines whether a permission with the same name already exists.
     *
     * @param permission the permission to check; its name must be non-null and non-blank
     * @return {@code true} if a permission with the same name exists; {@code false} otherwise
     */
    @Override
    public boolean existsByUniqueProperties(Permission permission) {
        if (permission.getName() == null || permission.getName().isBlank()) {
            return false;
        }
        return repo.existsByName(permission.getName());
    }

    /**
     * Searches for a {@link Permission} entity by its permission name.
     *
     * @param permissionName the name of the permission to search for; may be null or blank
     * @return an {@link Optional} containing the found permission, or empty if none found
     */
    @Override
    public Optional<Permission> findByPermissionName(String permissionName) {
        return repo.findByName(permissionName);
    }

    /**
     * Verifies whether all entities corresponding to the provided identifiers exist.
     *
     * @param is the collection of IDs to check; may be {@code null}
     * @return {@code true} if the count of matching entities equals the number of IDs provided;
     *     {@code false} otherwise
     */
    @Override
    public boolean existsAllByIds(Collection<UUID> is) {
        return (is.size()) == repo.countByIds(is);
    }
}
