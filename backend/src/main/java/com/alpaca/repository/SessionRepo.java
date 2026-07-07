package com.alpaca.repository;

import com.alpaca.entity.Session;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
 * @see CustomRepo
 */
@Repository
public interface SessionRepo extends CustomRepo<Session, UUID> {

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

    /**
     * Marks all non-revoked sessions for a given user as revoked.
     *
     * <p>Used when the user initiates a global logout (e.g. "log out of all devices") or when an
     * administrative action invalidates every session belonging to that user.
     *
     * @param userId user whose sessions are being revoked
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
             WHERE s.user.id = :userId
               AND s.revoked = false
            """)
    void revokeSessionsByUserId(
            @Param("userId") UUID userId,
            @Param("revokedAt") Instant revokedAt,
            @Param("reason") String reason);

    /**
     * Retrieves the first session associated with a given session family identifier.
     *
     * <p>Session family IDs group related sessions (e.g. from the same login flow on different
     * devices). This lookup is typically used during family-wide revocation or inspection of a
     * related session cluster.
     *
     * @param familyId the session family identifier
     * @return An {@link Optional} containing the session if found, otherwise empty
     */
    Optional<Session> findSessionByFamilyId(UUID familyId);

    /**
     * Retrieves a session scoped to a specific user.
     *
     * <p>This dual-key lookup ensures the session belongs to the identified user, acting as an
     * authorization guard at the data-access layer: callers must prove ownership of the session
     * rather than accessing it by ID alone.
     *
     * @param id the session UUID
     * @param userId the user who owns the session
     * @return An {@link Optional} containing the session if found and owned by the user, otherwise
     *     empty
     */
    Optional<Session> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Finds a non-revoked session matching the given composite device properties, with a
     * pessimistic write lock.
     *
     * <p>During session rotation, the client provides its device fingerprint ({@code userAgent},
     * {@code clientId}, {@code ipAddress}). This method matches an existing active session for the
     * same user and device to support session reuse or deduplication. The {@code clientId} and
     * {@code ipAddress} parameters are optional — when {@code null} they are excluded from the
     * match. The pessimistic lock prevents concurrent session creation for the same device profile.
     * A lock timeout of zero skips waiting if another transaction holds the lock.
     *
     * @param userId the user UUID
     * @param userAgent the browser or client user-agent string
     * @param clientId optional client identifier, may be null to skip matching
     * @param ipAddress optional IP address, may be null to skip matching
     * @return An {@link Optional} containing the matching session if found, otherwise empty
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
    @Query(
            """
              SELECT s
                FROM Session s
               WHERE s.user.id = :userId
                  AND s.userAgent = :userAgent
                  AND (:clientId IS NULL OR s.clientId = :clientId)
                  AND (:ipAddress IS NULL OR s.ipAddress = :ipAddress)
                  AND s.revoked = false
            """)
    Optional<Session> findByUniqueProperties(
            @Param("userId") UUID userId,
            @Param("userAgent") String userAgent,
            @Param("clientId") String clientId,
            @Param("ipAddress") String ipAddress);

    /**
     * Counts non-revoked sessions matching the given composite device properties.
     *
     * <p>Serves as a lighter alternative to {@link #findByUniqueProperties} when only existence or
     * cardinality is needed, avoiding the overhead of a pessimistic lock.
     *
     * @param userId the user UUID
     * @param userAgent the browser or client user-agent string
     * @param clientId optional client identifier, may be null to skip matching
     * @param ipAddress optional IP address, may be null to skip matching
     * @return the count of matching non-revoked sessions
     */
    @Query(
            """
              SELECT COUNT(s)
                FROM Session s
               WHERE s.user.id = :userId
                  AND s.userAgent = :userAgent
                  AND (:clientId IS NULL OR s.clientId = :clientId)
                  AND (:ipAddress IS NULL OR s.ipAddress = :ipAddress)
                  AND s.revoked = false
            """)
    long countByUniqueProperties(
            @Param("userId") UUID userId,
            @Param("userAgent") String userAgent,
            @Param("clientId") String clientId,
            @Param("ipAddress") String ipAddress);

    /**
     * Returns the session with the oldest {@code lastSeenAt} for a user, used when the user exceeds
     * their session limit and one must be evicted.
     *
     * <p>The lock timeout of zero makes the query fail fast instead of waiting for another
     * transaction — if the oldest session is already being evicted, this caller skips it rather
     * than contending.
     *
     * @param userId the user to find sessions for
     * @return the least-recently-used active session, or empty if none
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
    Optional<Session> findFirstByUserIdAndRevokedFalseOrderByLastSeenAtAsc(UUID userId);

    /**
     * Counts active (non-revoked) sessions for a given user.
     *
     * @param userId the user UUID
     * @return the number of non-revoked sessions belonging to the user
     */
    long countByUserIdAndRevokedFalse(UUID userId);

    /**
     * Returns a paginated view of active sessions for a given user.
     *
     * <p>The explicit {@code countQuery} ensures Spring Data uses a lightweight aggregate query for
     * page metadata rather than counting over the full result set.
     *
     * @param userId the user UUID
     * @param pageable pagination and sorting parameters
     * @return a {@link Page} of active sessions for the user
     */
    @Query(
            value =
                    """
                    SELECT s
                      FROM Session s
                     WHERE s.user.id = :userId
                       AND s.revoked = false
                    """,
            countQuery =
                    """
                    SELECT COUNT(s)
                      FROM Session s
                     WHERE s.user.id = :userId
                       AND s.revoked = false
                    """)
    Page<Session> findAllByUserId(@Param("userId") UUID userId, Pageable pageable);
}
