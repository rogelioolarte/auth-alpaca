package com.alpaca.service;

import com.alpaca.entity.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for managing {@link Session} entities. Extends {@link IGenericService} to
 * inherit common CRUD operations.
 *
 * @see IGenericService
 */
public interface ISessionService extends IGenericService<Session, UUID> {

    /**
     * Revokes all sessions belonging to the given family.
     *
     * <p>A session family groups all sessions created by the same login event (e.g., across
     * devices). Revoking by family ID invalidates the entire group.
     *
     * @param familyId the family identifier shared by related sessions
     * @param revokedAt the instant marking when the revocation occurred
     * @param reason a human-readable explanation for the revocation
     */
    void revokeSessionByFamilyId(UUID familyId, Instant revokedAt, String reason);

    /**
     * Finds the most recent session in a family.
     *
     * @param familyId the family identifier to look up
     * @return an {@code Optional} containing the session if one exists, or empty if the family has
     *     no sessions
     */
    Optional<Session> findSessionByFamilyId(UUID familyId);

    /**
     * Creates a new session for the specified user, capturing device and network context.
     *
     * @param userId the user identifier the session belongs to
     * @param userAgent the {@code User-Agent} header from the client's request, used for device
     *     fingerprinting
     * @param clientId the OAuth2 client identifier that initiated the session, if applicable
     * @param clientIp the IP address of the client at session creation time
     * @return the newly created {@code Session}
     */
    Session createSession(UUID userId, String userAgent, String clientId, String clientIp);

    /**
     * Revokes a specific session for a user, scoped by both user and session identifiers.
     *
     * <p>This prevents users from revoking sessions that do not belong to them. No-op if the
     * session does not exist or does not belong to the user.
     *
     * @param userId the owner of the session
     * @param id the session identifier to revoke
     */
    void revokeSessionByUserIdAndId(UUID userId, UUID id);

    /**
     * Revokes every active session for the given user.
     *
     * <p>This is typically invoked after a password change or when an administrator forces logout
     * across all devices.
     *
     * @param userId the user whose sessions should all be revoked
     */
    void revokeAllSessionsByUserId(UUID userId);

    /**
     * Retrieves a paginated list of sessions for a given user.
     *
     * @param userId the user whose sessions to retrieve
     * @param pageable the pagination configuration — must not be null
     * @return a {@code Page} containing the user's sessions
     */
    Page<Session> findAllByUserId(UUID userId, Pageable pageable);
}
