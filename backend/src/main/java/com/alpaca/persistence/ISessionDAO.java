package com.alpaca.persistence;

import com.alpaca.entity.Session;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Data Access Object (DAO) interface for managing {@link Session} entities.
 *
 * <p>Extends {@link IGenericDAO} to inherit common CRUD operations and defines additional queries
 * specific to {@code Session} management, including bulk revocation, device-fingerprint matching,
 * and session limit enforcement.
 *
 * @see IGenericDAO
 */
public interface ISessionDAO extends IGenericDAO<Session, UUID> {

    /**
     * Revokes all non-revoked sessions sharing a given session family identifier.
     *
     * @param familyId the session family to revoke
     * @param revokedAt timestamp of revocation
     * @param reason reason recorded on each revoked session
     */
    void revokeSessionByFamilyId(UUID familyId, Instant revokedAt, String reason);

    /**
     * Finds a session by ID, scoped to a specific user as an ownership guard.
     *
     * @param id the session UUID
     * @param userId the user who must own the session
     * @return An {@link Optional} containing the session if found and owned by the user
     */
    Optional<Session> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Retrieves a session by its family identifier.
     *
     * @param familyId the session family identifier
     * @return An {@link Optional} containing the session if found, otherwise empty
     */
    Optional<Session> findSessionByFamilyId(UUID familyId);

    /**
     * Finds a non-revoked session matching a user's device fingerprint, with optional client ID and
     * IP address matching.
     *
     * @param userId the user UUID
     * @param userAgent the user-agent string
     * @param clientId optional client identifier, null to skip
     * @param ipAddress optional IP address, null to skip
     * @return An {@link Optional} containing the matching session, if any
     */
    Optional<Session> findByUniqueProperties(
            UUID userId, String userAgent, String clientId, String ipAddress);

    /**
     * Retrieves the oldest active session for a user, intended for eviction or session-limit
     * enforcement.
     *
     * @param userId the user UUID
     * @return An {@link Optional} containing the oldest active session, if any
     */
    Optional<Session> findFirstActiveSessionForUpdate(UUID userId);

    /**
     * Counts the number of active (non-revoked) sessions for a given user.
     *
     * @param userId the user UUID
     * @return the count of active sessions
     */
    long countByUserIdAndRevokedFalse(UUID userId);

    /**
     * Revokes all active sessions for a given user.
     *
     * @param userId the user whose sessions to revoke
     * @param revokedAt timestamp of revocation
     * @param reason reason recorded on each revoked session
     */
    void revokeSessionsByUserId(UUID userId, Instant revokedAt, String reason);

    /**
     * Returns a paginated list of active sessions for a given user.
     *
     * @param userId the user UUID
     * @param pageable pagination and sorting parameters
     * @return a {@link Page} of active sessions
     */
    Page<Session> findAllByUserId(UUID userId, Pageable pageable);
}
