package com.alpaca.repository;

import com.alpaca.entity.Advertiser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository interface for managing {@link Advertiser} entities.
 *
 * <p>Extends {@link CustomRepo} to inherit common CRUD operations and defines additional queries
 * for advertiser-specific operations.
 *
 * @see CustomRepo
 */
@Repository
public interface AdvertiserRepo extends CustomRepo<Advertiser, UUID> {

    /**
     * Counts the number of advertisers associated with a specific user ID.
     *
     * @param userId The ID of the user - must not be null.
     * @return The number of advertisers linked to the given user.
     */
    @Query("SELECT COUNT(p) FROM Advertiser p WHERE p.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);

    /**
     * Returns a paginated list of advertisers that have been marked as indexed.
     *
     * <p>This is used by indexing jobs or search-related queries to process only advertisers whose
     * data is ready for indexing, excluding those still pending or excluded.
     *
     * @param pageable pagination and sorting parameters
     * @return a {@link Page} of indexed advertisers
     */
    Page<Advertiser> findAllByIndexedTrue(Pageable pageable);
}
