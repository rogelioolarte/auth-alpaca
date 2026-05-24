package com.alpaca.persistence.impl;

import com.alpaca.entity.Session;
import com.alpaca.persistence.ISessionDAO;
import com.alpaca.repository.GenericRepo;
import com.alpaca.repository.SessionRepo;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
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
     * @return the {@link GenericRepo} implementation used for CRUD operations on {@link Session}
     */
    @Override
    @Generated
    protected GenericRepo<Session, UUID> getRepo() {
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
        if (session.getUser() == null
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

    @Override
    public void revokeSessionByFamilyId(UUID familyId, Instant revokedAt, String reason) {
        repo.revokeSessionByFamilyId(familyId, revokedAt, reason);
    }

    @Override
    public Optional<Session> findByIdAndUserId(UUID id, UUID userId) {
        return repo.findByIdAndUserId(id, userId);
    }

    @Override
    public Optional<Session> findSessionByFamilyId(UUID familyId) {
        return repo.findSessionByFamilyId(familyId);
    }

    @Override
    public Optional<Session> findByUniqueProperties(
            UUID userId, String userAgent, String clientId, String ipAddress) {
        return repo.findByUniqueProperties(userId, userAgent, clientId, ipAddress);
    }

    @Override
    public Optional<Session> findFirstActiveSessionForUpdate(UUID userId) {
        return repo.findFirstActiveSessionForUpdate(userId);
    }

    @Override
    public long countByUserIdAndRevokedFalse(UUID userId) {
        return repo.countByUserIdAndRevokedFalse(userId);
    }

    @Override
    public void revokeSessionsByUserId(UUID userId, Instant revokedAt, String reason) {
        repo.revokeSessionsByUserId(userId, revokedAt, reason);
    }

    /**
     * Verifies whether all entities corresponding to the provided identifiers exist.
     *
     * @param is the collection of IDs to check; may be {@code null}
     * @return {@code true} if the count of matching entities equals the number of IDs provided;
     *     {@code false} otherwise
     */
    @Override
    public boolean existsAllByIds(Collection<UUID> is) {
        return (is.size()) == repo.countByIds(is);
    }
}
