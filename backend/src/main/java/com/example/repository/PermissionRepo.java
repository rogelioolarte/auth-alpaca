package com.example.repository;

import com.example.entity.Permission;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository interface for managing {@link Permission} entities.
 * <p>
 * Extends {@link GenericRepo} to inherit common CRUD operations
 * and defines additional queries for permission-specific operations.
 * </p>
 *
 * @see GenericRepo
 */
@Repository
public interface PermissionRepo extends GenericRepo<Permission, UUID> {

    /**
     * Checks whether a permission with the specified name exists.
     *
     * @param permissionName The name of the permission - must not be null.
     * @return {@code true} if a permission with the given name exists, {@code false} otherwise.
     */
    boolean existsByPermissionName(String permissionName);
}
