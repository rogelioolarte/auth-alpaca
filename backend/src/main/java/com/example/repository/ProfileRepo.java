package com.example.repository;

import com.example.entity.Profile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository interface for managing {@link Profile} entities.
 * <p>
 * Extends {@link GenericRepo} to inherit common CRUD operations and
 * defines additional queries for profile-specific operations.
 * </p>
 *
 * @see GenericRepo
 */
@Repository
public interface ProfileRepo extends GenericRepo<Profile, UUID> {

    /**
     * Counts the number of profiles associated with a specific user ID.
     *
     * @param userId The ID of the user - must not be null.
     * @return The number of profiles linked to the given user.
     */
    @Query("SELECT COUNT(p) FROM Profile p WHERE p.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);
}
