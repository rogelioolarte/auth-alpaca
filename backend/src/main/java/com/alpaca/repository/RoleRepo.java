package com.alpaca.repository;

import com.alpaca.entity.Role;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
    Optional<Role> findByName(String roleName);

    /**
     * Checks whether a role with the specified name exists.
     *
     * @param roleName The name of the role to check - must not be null.
     * @return {@code true} if a role with the given name exists, {@code false} otherwise.
     */
    boolean existsByName(String roleName);

    /**
     * Counts the number of entities with the given IDs.
     *
     * @param ids The collection of entity IDs to count - must not be null.
     * @return The number of entities found matching the provided IDs.
     */
    @Query("SELECT COUNT(e) FROM Role e WHERE e.id IN :ids")
    long countByIds(@Param("ids") Collection<UUID> ids);
}
