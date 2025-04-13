package com.alpaca.repository;

import com.alpaca.entity.Advertiser;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AdvertiserRepo extends GenericRepo<Advertiser, UUID> {

  /**
   * Counts the number of advertisers associated with a specific user ID.
   *
   * @param userId The ID of the user - must not be null.
   * @return The number of advertisers linked to the given user.
   */
  @Query("SELECT COUNT(p) FROM Advertiser p WHERE p.user.id = :userId")
  long countByUserId(@Param("userId") UUID userId);
}
