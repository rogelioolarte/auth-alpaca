package com.alpaca.repository;

import com.alpaca.entity.Profile;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link Profile} entities.
 *
 * <p>Extends {@link GenericRepo} to inherit common CRUD operations and defines additional queries
 * for profile-specific operations.
 *
 * @see GenericRepo
 */
@Repository
public interface ProfileRepo extends GenericRepo<Profile, UUID> {

    /**
     * Counts the number of profiles associated with a specific user ID.
     *
     * @param email The email of the user - must not be null.
     * @return The number of profiles linked to the given user.
     */
    @Query("SELECT COUNT(p) FROM Profile p WHERE p.user.email = :email")
    long countByUserEmail(@Param("email") String email);

    /**
     * Counts the number of entities with the given IDs.
     *
     * @param ids The collection of entity IDs to count - must not be null.
     * @return The number of entities found matching the provided IDs.
     */
    @Query("SELECT COUNT(e) FROM Profile e WHERE e.id IN :ids")
    long countByIds(@Param("ids") Collection<UUID> ids);
}
