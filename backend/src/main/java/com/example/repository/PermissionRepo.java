package com.example.repository;

import com.example.entity.Permission;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Deletes all role-permission associations for a given permission ID.
     * <p>
     * This operation modifies the database directly and is executed as a transactional query.
     * </p>
     *
     * @param permission_id The ID of the permission to remove associations for - must not be null.
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM role_permissions WHERE permission_id = ?1", nativeQuery = true)
    void deleteRolePermissionsByPermissionId(@Param("permission_id") UUID permission_id);
}
