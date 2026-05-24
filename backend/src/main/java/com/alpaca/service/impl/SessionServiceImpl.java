package com.alpaca.service.impl;

import com.alpaca.entity.Session;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.ExceededSessionsException;
import com.alpaca.exception.ForbiddenException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IGenericDAO;
import com.alpaca.persistence.IRefreshTokenDAO;
import com.alpaca.persistence.ISessionDAO;
import com.alpaca.persistence.IUserDAO;
import com.alpaca.service.IGenericService;
import com.alpaca.service.IRefreshTokenService;
import com.alpaca.service.ISessionService;
import com.alpaca.utils.UUIDv7Generator;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.Generated;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer implementation for managing {@link Session} entities. Inherits common CRUD
 * operations from {@link IGenericService}.
 *
 * <p>This service delegates persistence operations to the {@link ISessionDAO} and provides a clear
 * abstraction point for future business logic related to permissions.
 *
 * @see IGenericService
 * @see IRefreshTokenService
 */
@Service
public class SessionServiceImpl extends GenericServiceImpl<Session, UUID>
        implements ISessionService {

    private final ISessionDAO dao;
    private final IUserDAO userDAO;
    private final IRefreshTokenDAO refreshTokenDAO;
    private final UUIDv7Generator uuidv7Generator;

    private final int maxSessionsPerUser;
    private final boolean infinityLogin;
    private static final String USER_SELF_REVOCATION = "user-self-revocation";

    public SessionServiceImpl(
            ISessionDAO dao,
            IUserDAO userDAO,
            IRefreshTokenDAO refreshTokenDAO,
            UUIDv7Generator uuidv7Generator,
            @Value("${security.max.session.per.user:10}") @NotNull int maxSessionsPerUser,
            @Value("${security.infinity.login:false}") @NotNull boolean infinityLogin) {
        if (maxSessionsPerUser < 1) {
            throw new IllegalStateException("security.max.session.per.user must be >= 1");
        }
        this.dao = dao;
        this.userDAO = userDAO;
        this.refreshTokenDAO = refreshTokenDAO;
        this.uuidv7Generator = uuidv7Generator;
        this.maxSessionsPerUser = maxSessionsPerUser;
        this.infinityLogin = infinityLogin;
    }

    /**
     * Supplies the DAO component for data access operations.
     *
     * @return the {@link IGenericDAO} corresponding to the entity type {@code Session}
     */
    @Override
    @Generated
    protected IGenericDAO<Session, UUID> getDAO() {
        return dao;
    }

    /**
     * Provides a human-readable entity name to be used in exception messages.
     *
     * @return the name of the entity "Session"
     */
    @Override
    @Generated
    protected String getEntityName() {
        return "Session";
    }

    @Override
    public void revokeSessionByFamilyId(UUID familyId, Instant revokedAt, String reason) {
        dao.revokeSessionByFamilyId(familyId, revokedAt, reason);
    }

    @Override
    public Optional<Session> findSessionByFamilyId(UUID familyId) {
        return dao.findSessionByFamilyId(familyId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Override
    public Session createSession(UUID userId, String userAgent, String clientId, String clientIp) {
        User user =
                userDAO.lockFindUserById(userId)
                        .orElseThrow(() -> new NotFoundException("User not found"));

        // Reuse existing session for same device and rotate refresh-token family
        Optional<Session> session =
                dao.findByUniqueProperties(userId, userAgent, clientId, clientIp);
        UUID newFamilyId = uuidv7Generator.generate();
        Instant now = Instant.now();
        Session newSession;
        String newSessionCreatedReason = "new-session-created";
        if (session.isPresent()) {
            newSession = session.get();
            // All previous refresh tokens are revoked
            refreshTokenDAO.revokeFamilyWithReason(
                    newSession.getFamilyId(), now, newSessionCreatedReason);
        } else {
            long activeSession = dao.countByUserIdAndRevokedFalse(userId);
            Optional<Session> lastSession = dao.findFirstActiveSessionForUpdate(userId);

            if (activeSession >= maxSessionsPerUser) {
                if (this.infinityLogin && lastSession.isPresent()) {
                    UUID oldestSessionFamilyId = lastSession.get().getFamilyId();
                    refreshTokenDAO.revokeFamilyWithReason(
                            oldestSessionFamilyId, now, newSessionCreatedReason);
                    dao.revokeSessionByFamilyId(
                            oldestSessionFamilyId, now, newSessionCreatedReason);
                } else if (!this.infinityLogin) {
                    throw new ExceededSessionsException(maxSessionsPerUser);
                }
            }
            newSession = new Session();
            newSession.setUser(user);

            updateTextIfExists(newSession.getUserAgent(), userAgent, newSession::setUserAgent);
            updateIfNotNull(newSession.getClientId(), clientId, newSession::setClientId);
        }
        newSession.setRevoked(false);
        updateTextIfExists(newSession.getIpAddress(), clientIp, newSession::setIpAddress);
        updateIfNotNull(newSession.getFamilyId(), newFamilyId, newSession::setFamilyId);
        updateIfNotNull(newSession.getLastSeenAt(), now, newSession::setLastSeenAt);

        return dao.save(newSession);
    }

    @Transactional
    @Override
    public void revokeSessionByUserIdAndId(UUID userId, UUID id) {
        Session session =
                dao.findByIdAndUserId(id, userId)
                        .orElseThrow(() -> new ForbiddenException("Invalid Session to revoke"));
        if (!userId.equals(session.getUser().getId())) {
            throw new ForbiddenException("Invalid Session to revoke");
        }
        revokeSessionByFamilyId(session.getFamilyId(), Instant.now(), USER_SELF_REVOCATION);
    }

    @Transactional
    @Override
    public void revokeAllSessionsByUserId(UUID userId) {
        Instant now = Instant.now();
        dao.revokeSessionsByUserId(userId, now, USER_SELF_REVOCATION);
        refreshTokenDAO.revokeTokensByUserId(userId, now, USER_SELF_REVOCATION);
    }

    @Override
    public boolean existsByUniqueProperties(Session s) {
        return dao.existsByUniqueProperties(s);
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
    @Transactional
    @Override
    public Session updateById(Session session, UUID id) {
        if (session == null || id == null)
            throw new BadRequestException(
                    String.format("%s with ID %s cannot be updated", getEntityName(), id));

        Session existingSession =
                dao.findById(id)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                String.format(
                                                        "%s with ID %s not found",
                                                        getEntityName(), id)));
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
        return dao.save(existingSession);
    }
}
