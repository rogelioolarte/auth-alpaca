package com.alpaca.persistence.impl;

import com.alpaca.entity.Session;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.ISessionDAO;
import com.alpaca.repository.GenericRepo;
import com.alpaca.repository.SessionRepo;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
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
     * @return the {@link GenericRepo} implementation used for CRUD operations on {@link Session}
     */
    @Override
    protected GenericRepo<Session, UUID> getRepo() {
        return repo;
    }

    /**
     * Returns the entity class managed by this DAO.
     *
     * @return {@code Session.class}
     */
    @Override
    protected Class<Session> getEntity() {
        return Session.class;
    }

    /**
     * Updates an existing {@link Session} identified by {@code id} using values from the supplied
     * {@code session} parameter. The method applies selective updates:
     *
     * <ul>
     *   <li>Association fields such as {@code user} are replaced only if the incoming association
     *       is non-null, contains an identifier, and its id differs from the existing one.
     *   <li>Scalar/timestamp fields are updated through helper methods that check for non-nullity
     *       and inequality (e.g. {@code updateIfNotNull}, {@code updateIfDifferent}).
     *   <li>Textual fields are updated only when incoming text is present and different from the
     *       stored value (via {@code updateTextIfExists}).
     * </ul>
     *
     * <p>The fields considered for update include {@code user}, {@code familyId}, {@code
     * ipAddress}, {@code userAgent}, {@code clientId}, {@code revoked}, {@code revokedAt} and
     * {@code revokeReason}.
     *
     * @param session the {@link Session} instance containing new values to apply; fields may be
     *     {@code null} to indicate they should remain unchanged
     * @param id the unique identifier of the persisted {@link Session} to update
     * @return the updated and persisted {@link Session} instance
     * @throws NotFoundException if no {@link Session} with the supplied {@code id} exists
     */
    @Override
    public Session updateById(Session session, UUID id) {
        Session existingSession =
                findById(id)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                String.format(
                                                        "%s with ID %s not found",
                                                        getEntity().getName(), id.toString())));
        if (session.getUser() != null && session.getUser().getId() != null) {
            UUID currentUserId =
                    existingSession.getUser() != null ? existingSession.getUser().getId() : null;
            if (!Objects.equals(session.getUser().getId(), currentUserId)) {
                existingSession.setUser(session.getUser());
            }
        }
        updateIfNotNull(
                existingSession.getFamilyId(), session.getFamilyId(), existingSession::setFamilyId);
        updateTextIfExists(
                existingSession.getIpAddress(),
                session.getIpAddress(),
                existingSession::setIpAddress);
        updateTextIfExists(
                existingSession.getUserAgent(),
                session.getUserAgent(),
                existingSession::setUserAgent);
        updateTextIfExists(
                existingSession.getClientId(), session.getClientId(), existingSession::setClientId);
        updateIfDifferent(
                existingSession.isRevoked(), session.isRevoked(), existingSession::setRevoked);
        updateIfNotNull(
                existingSession.getRevokedAt(),
                session.getRevokedAt(),
                existingSession::setRevokedAt);
        updateTextIfExists(
                existingSession.getRevokeReason(),
                session.getRevokeReason(),
                existingSession::setRevokeReason);
        return save(existingSession);
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
    public Optional<Session> findSessionByFamilyId(UUID familyId) {
        return repo.findSessionByFamilyId(familyId);
    }

    @Override
    public Optional<Session> findByUniqueProperties(
            UUID userId, String userAgent, String clientId, String ipAddress) {
        return repo.findByUniqueProperties(userId, userAgent, clientId, ipAddress);
    }

    @Override
    public List<Session> findActiveSessionsByUserOrderByLastSeen(UUID userId, Pageable pageable) {
        return repo.findActiveSessionsByUserOrderByLastSeen(userId, pageable);
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
