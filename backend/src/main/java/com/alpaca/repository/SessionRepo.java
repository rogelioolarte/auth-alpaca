package com.alpaca.repository;

import com.alpaca.entity.Session;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
     * @return number of session records revoked
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
    int revokeSessionByFamilyId(
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
}
