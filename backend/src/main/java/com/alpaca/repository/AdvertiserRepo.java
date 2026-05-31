package com.alpaca.repository;

import com.alpaca.entity.Advertiser;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    Page<Advertiser> findAllPageByIndexedTrue(Pageable pageable);
}
