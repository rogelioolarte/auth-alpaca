package com.example.repository;

import com.example.entity.Role;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link Role} entities.
 * <p>
 * Extends {@link GenericRepo} to inherit common CRUD operations and
 * defines additional queries for role-specific operations.
 * </p>
 *
 * @see GenericRepo
 */
@Repository
public interface RoleRepo extends GenericRepo<Role, UUID> {

    /**
     * Deletes all user-role associations for a given role ID.
     * <p>
     * This operation modifies the database directly and is executed as a transactional query.
     * </p>
     *
     * @param roleId The ID of the role to remove associations for - must not be null.
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM user_roles WHERE role_id = ?1", nativeQuery = true)
    void deleteUserRolesByRoleId(UUID roleId);

    /**
     * Retrieves a role by its name.
     *
     * @param roleName The name of the role.
     * @return An {@link Optional} containing the role if found, otherwise empty.
     */
    Optional<Role> findByRoleName(String roleName);

    /**
     * Checks whether a role with the specified name exists.
     *
     * @param roleName The name of the role to check - must not be null.
     * @return {@code true} if a role with the given name exists, {@code false} otherwise.
     */
    boolean existsByRoleName(String roleName);

    /**
     * Retrieves a list of roles associated with a specific permission.
     *
     * @param permissionId The ID of the permission - must not be null.
     * @return A list of roles that have the specified permission.
     */
    @Query("SELECT r FROM Role r JOIN r.permissions p WHERE p.id = :permissionId")
    List<Role> findRolesByPermissionId(@Param("permissionId") UUID permissionId);
}
