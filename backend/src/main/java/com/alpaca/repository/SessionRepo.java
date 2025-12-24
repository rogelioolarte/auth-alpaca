package com.alpaca.repository;

import com.alpaca.entity.Session;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link Session} entities.
 *
 * <p>Extends {@code GenericRepo} to provide basic CRUD operations. Includes a custom modification
 * query for revoking all sessions belonging to a given session family identifier.
 *
 * <p>This is useful for session lifecycle management and bulk logout scenarios across related
 * sessions.
 *
 * @see GenericRepo
 */
@Repository
public interface SessionRepo extends GenericRepo<Session, UUID> {

    /**
     * Marks all active sessions in a session family as revoked.
     *
     * <p>Used, for example, when a user logs out from all devices or a security event invalidates a
     * full session family group.
     *
     * @param familyId unique group identifier of related sessions
     * @param revokedAt timestamp when revocation was applied
     * @param reason reason for revocation, recorded on each revoked session
     */
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "javax.persistence.lock.timeout", value = "0"))
    @Query(
            """
              SELECT s
                FROM Session s
               WHERE s.user.id = :userId
                 AND s.userAgent = :userAgent
                 AND s.clientId = :clientId
                 AND s.revoked = false
            """)
    Optional<Session> findByUniqueProperties(
            @Param("userId") UUID userId,
            @Param("userAgent") String userAgent,
            @Param("clientId") String clientId);

    @Query(
            """
            SELECT COUNT(s)
              FROM Session s
             WHERE s.user.id = :userId
               AND s.revoked = false
            """)
    long countActiveSessionsByUser(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "javax.persistence.lock.timeout", value = "0"))
    @Query(
            """
              SELECT s
                FROM Session s
               WHERE s.user.id = :userId
                 AND s.revoked = false
               ORDER BY CASE WHEN s.lastSeenAt IS NULL THEN 0 ELSE 1 END ASC, s.lastSeenAt ASC
            """)
    List<Session> findActiveSessionsByUserOrderByLastSeen(
            @Param("userId") UUID userId, Pageable pageable);
}
