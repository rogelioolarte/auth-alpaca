package com.alpaca.persistence.impl;

import com.alpaca.entity.Session;
import com.alpaca.persistence.ISessionDAO;
import com.alpaca.repository.CustomRepo;
import com.alpaca.repository.SessionRepo;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Implementation of the {@link ISessionDAO} interface for managing {@link Session} entities. This
 * class extends the generic DAO implementation ({@link GenericDAOImpl}) to provide standard CRUD
 * operations together with session-specific persistence and selective-update semantics.
 *
 * <p>Update operations performed by this DAO apply changes selectively: only non-null or meaningful
 * incoming values that differ from the persisted values are applied. The class relies on helper
 * methods in the superclass (for example {@code updateIfNotNull}, {@code updateIfDifferent} and
 * {@code updateTextIfExists}) to centralize and standardize update behavior across entities.
 */
@Component
@RequiredArgsConstructor
public class SessionDAOImpl extends GenericDAOImpl<Session, UUID> implements ISessionDAO {

    private final SessionRepo repo;

    /**
     * Returns the repository instance backing this DAO's persistence operations for {@link
     * Session}.
     *
     * @return the {@link CustomRepo} implementation used for CRUD operations on {@link Session}
     */
    @Override
    @Generated
    protected CustomRepo<Session, UUID> getRepo() {
        return repo;
    }

    /**
     * Determines whether a {@link Session} with equivalent unique properties already exists in the
     * persistence store.
     *
     * @param session the {@link Session} whose unique properties should be validated
     * @return {@code true} if a session with equivalent unique properties exists; {@code false}
     *     otherwise
     */
    @Override
    public boolean existsByUniqueProperties(Session session) {
        if (session == null
                || session.getUser() == null
                || session.getUser().getId() == null
                || !StringUtils.hasText(session.getUserAgent())
                || !StringUtils.hasText(session.getClientId())
                || !StringUtils.hasText(session.getIpAddress())) {
            return false;
        }
        return repo.countByUniqueProperties(
                        session.getUser().getId(),
                        session.getUserAgent(),
                        session.getClientId(),
                        session.getIpAddress())
                > 0L;
    }

    /**
     * Revokes all sessions sharing the given family identifier.
     *
     * @param familyId the session family to revoke
     * @param revokedAt timestamp of the revocation
     * @param reason recorded reason for the revocation
     */
    @Override
    public void revokeSessionByFamilyId(UUID familyId, Instant revokedAt, String reason) {
        repo.revokeSessionByFamilyId(familyId, revokedAt, reason);
    }

    /**
     * Finds a session by ID, restricted to a specific user as an authorization guard.
     *
     * @param id the session UUID
     * @param userId the user who must own the session
     * @return An {@link Optional} containing the session if found and owned by the user
     */
    @Override
    public Optional<Session> findByIdAndUserId(UUID id, UUID userId) {
        return repo.findByIdAndUserId(id, userId);
    }

    /**
     * Retrieves a session by its family identifier.
     *
     * @param familyId the session family identifier
     * @return An {@link Optional} containing the session if found, otherwise empty
     */
    @Override
    public Optional<Session> findSessionByFamilyId(UUID familyId) {
        return repo.findSessionByFamilyId(familyId);
    }

    /**
     * Finds an active session matching the user's device fingerprint.
     *
     * @param userId the user UUID
     * @param userAgent the user-agent string
     * @param clientId optional, may be null to skip
     * @param ipAddress optional, may be null to skip
     * @return An {@link Optional} containing the matching session if found
     */
    @Override
    public Optional<Session> findByUniqueProperties(
            UUID userId, String userAgent, String clientId, String ipAddress) {
        return repo.findByUniqueProperties(userId, userAgent, clientId, ipAddress);
    }

    /**
     * Retrieves the oldest active session for a user, used in eviction decisions.
     *
     * @param userId the user UUID
     * @return An {@link Optional} containing the oldest active session if any
     */
    @Override
    public Optional<Session> findFirstActiveSessionForUpdate(UUID userId) {
        return repo.findFirstByUserIdAndRevokedFalseOrderByLastSeenAtAsc(userId);
    }

    /**
     * Counts active (non-revoked) sessions for a user.
     *
     * @param userId the user UUID
     * @return the count of active sessions
     */
    @Override
    public long countByUserIdAndRevokedFalse(UUID userId) {
        return repo.countByUserIdAndRevokedFalse(userId);
    }

    /**
     * Revokes all active sessions for a user.
     *
     * @param userId the user whose sessions to revoke
     * @param revokedAt timestamp of the revocation
     * @param reason recorded reason for the revocation
     */
    @Override
    public void revokeSessionsByUserId(UUID userId, Instant revokedAt, String reason) {
        repo.revokeSessionsByUserId(userId, revokedAt, reason);
    }

    /**
     * Returns a paginated list of active sessions for a user.
     *
     * @param userId the user UUID
     * @param pageable pagination and sorting parameters
     * @return a {@link Page} of active sessions
     */
    @Override
    public Page<Session> findAllByUserId(UUID userId, Pageable pageable) {
        return repo.findAllByUserId(userId, pageable);
    }
}
