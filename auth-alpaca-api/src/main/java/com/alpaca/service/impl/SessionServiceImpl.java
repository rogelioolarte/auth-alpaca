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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer implementation for managing {@link Session} entities. Inherits common CRUD
 * operations from {@link IGenericService}.
 *
 * <p>This service delegates persistence operations to the {@link ISessionDAO} and enforces
 * session-level policies such as per-user session limits and concurrent-device eviction. Session
 * revocation cascades to the associated refresh token family.
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

    /**
     * Constructs the service with DAO dependencies and session-limit configuration.
     *
     * @param maxSessionsPerUser maximum concurrent active sessions allowed per user (default: 10);
     *     must be at least 1
     * @param infinityLogin when {@code true}, exceeding the session limit evicts the oldest session
     *     instead of rejecting the login
     * @throws IllegalStateException if {@code maxSessionsPerUser} is less than 1
     */
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

    /**
     * Revokes all sessions that share the given {@code familyId}. This is a bulk operation
     * typically triggered when a refresh token in the same family is compromised or rotated.
     *
     * @param familyId the token family identifier to revoke
     * @param revokedAt the timestamp marking when the revocation occurred
     * @param reason a human-readable reason recorded for audit
     */
    @Override
    public void revokeSessionByFamilyId(UUID familyId, Instant revokedAt, String reason) {
        dao.revokeSessionByFamilyId(familyId, revokedAt, reason);
    }

    /**
     * Finds any non-deleted session that belongs to the given token family.
     *
     * @param familyId the token family identifier to look up
     * @return an {@link Optional} containing the session if one exists
     */
    @Override
    public Optional<Session> findSessionByFamilyId(UUID familyId) {
        return dao.findSessionByFamilyId(familyId);
    }

    /**
     * Creates or reuses a session for the given user and device fingerprint.
     *
     * <p><b>Session reuse:</b> If an active session already exists for the same user-agent,
     * client-id, and client-IP combination, the existing session is reused. The refresh token
     * family is rotated — the old family is revoked and a new one is assigned.
     *
     * <p><b>Session limit enforcement:</b> When the user has reached the maximum allowed active
     * sessions ({@code maxSessionsPerUser}) and no existing session matches, behavior depends on
     * {@code infinityLogin}:
     *
     * <ul>
     *   <li>If {@code infinityLogin} is {@code true}, the oldest session is evicted (revoked).
     *   <li>If {@code infinityLogin} is {@code false}, an {@link ExceededSessionsException} is
     *       thrown.
     * </ul>
     *
     * <p>A pessimistic lock ({@code lockFindUserById}) is acquired on the user row, and the
     * isolation level is {@link Isolation#READ_COMMITTED} to prevent dirty reads during concurrent
     * session creation.
     *
     * @param userId the user for whom to create a session
     * @param userAgent the HTTP User-Agent header identifying the client device
     * @param clientId an identifier for the OAuth2 client application
     * @param clientIp the IP address from which the request originated
     * @return the created or reused {@link Session} instance
     * @throws NotFoundException if the user does not exist
     * @throws ExceededSessionsException if the session limit is reached and {@code infinityLogin}
     *     is disabled
     */
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
        newSession.setRevokedAt(null);
        newSession.setRevokeReason(null);
        updateTextIfExists(newSession.getIpAddress(), clientIp, newSession::setIpAddress);
        updateIfNotNull(newSession.getFamilyId(), newFamilyId, newSession::setFamilyId);
        updateIfNotNull(newSession.getLastSeenAt(), now, newSession::setLastSeenAt);

        return super.save(newSession);
    }

    /**
     * Revokes a single session owned by the given user. The session is looked up by both session ID
     * and user ID to prevent a user from revoking another user's session. Revocation cascades to
     * the entire refresh token family associated with this session.
     *
     * @param userId the owner of the session to revoke
     * @param id the unique identifier of the session to revoke
     * @throws ForbiddenException if the session does not belong to the specified user
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Override
    public void revokeSessionByUserIdAndId(UUID userId, UUID id) {
        Session session =
                dao.findByIdAndUserId(id, userId)
                        .orElseThrow(() -> new ForbiddenException("Invalid Session to revoke"));
        if (!userId.equals(session.getUser().getId())) {
            throw new ForbiddenException("Invalid Session to revoke");
        }
        dao.revokeSessionByFamilyId(session.getFamilyId(), Instant.now(), USER_SELF_REVOCATION);
        refreshTokenDAO.revokeFamilyWithReason(
                session.getFamilyId(), Instant.now(), USER_SELF_REVOCATION);
    }

    /**
     * Revokes every active session owned by the specified user. This is a bulk operation — all
     * sessions and their associated refresh token families are revoked simultaneously.
     *
     * @param userId the user whose sessions are to be revoked
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Override
    public void revokeAllSessionsByUserId(UUID userId) {
        Instant now = Instant.now();
        dao.revokeSessionsByUserId(userId, now, USER_SELF_REVOCATION);
        refreshTokenDAO.revokeTokensByUserId(userId, now, USER_SELF_REVOCATION);
    }

    /**
     * Retrieves a paginated list of sessions belonging to the specified user.
     *
     * @param userId the user whose sessions to retrieve
     * @param pageable pagination parameters; must not be {@code null}
     * @return a paginated list of sessions
     */
    @Override
    public Page<Session> findAllByUserId(UUID userId, Pageable pageable) {
        return dao.findAllByUserId(userId, pageable);
    }

    /**
     * Checks whether a session with the same unique identifying properties already exists.
     *
     * @param s the session whose properties to check
     * @return {@code true} if a matching session exists
     */
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
    @Override
    @Transactional
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
        return super.save(existingSession);
    }
}
