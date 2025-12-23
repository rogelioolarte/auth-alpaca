package com.alpaca.repository;

import com.alpaca.entity.Role;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link Role} entities.
 *
 * <p>Extends {@link GenericRepo} to inherit common CRUD operations and defines additional queries
 * for role-specific operations.
 *
 * @see GenericRepo
 */
@Repository
public interface RoleRepo extends GenericRepo<Role, UUID> {

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
}
