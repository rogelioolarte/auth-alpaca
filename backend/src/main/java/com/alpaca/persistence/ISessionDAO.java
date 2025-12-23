package com.alpaca.persistence;

import com.alpaca.entity.Session;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data Access Object (DAO) interface for managing {@link Session} entities.
 *
 * <p>Extends {@link IGenericDAO} to inherit common CRUD operations and defines additional queries
 * specific to {@code Session} management.
 *
 * @see IGenericDAO
 */
public interface ISessionDAO extends IGenericDAO<Session, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
                UPDATE Session s
                   SET s.revoked = true,
                       s.revokedAt = :revokedAt,
                       s.revokeReason = :reason
                 WHERE s.familyId = :familyId
                   AND s.revoked = false
            """)
    void revokeSessionByFamilyId(
            @Param("familyId") UUID familyId,
            @Param("revokedAt") Instant revokedAt,
            @Param("reason") String reason);

    Optional<Session> findSessionByFamilyId(UUID familyId);

    Optional<Session> findByUniqueProperties(UUID userId, String userAgent, String clientId);

    long countActiveSessionsByUser(UUID userId);

    List<Session> findActiveSessionsByUserOrderByLastSeen(UUID userId, Pageable pageable);
}
