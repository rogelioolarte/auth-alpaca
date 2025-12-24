package com.alpaca.service.impl;

import com.alpaca.entity.Session;
import com.alpaca.entity.User;
import com.alpaca.exception.ExceededSessionsException;
import com.alpaca.persistence.IGenericDAO;
import com.alpaca.persistence.ISessionDAO;
import com.alpaca.service.IGenericService;
import com.alpaca.service.IRefreshTokenService;
import com.alpaca.service.ISessionService;
import com.alpaca.utils.UUIDv7Generator;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    private final UUIDv7Generator uuidv7Generator;

    private final int maxSessionsPerUser;
    private final Pageable pageableForMaxSessions;

    public SessionServiceImpl(
            ISessionDAO dao,
            UUIDv7Generator uuidv7Generator,
            @Value("${security.max.session.per.user:10}") @NotNull int maxSessionsPerUser) {
        if (maxSessionsPerUser < 1) {
            throw new IllegalStateException("security.max.session.per.user must be >= 1");
        }
        this.dao = dao;
        this.uuidv7Generator = uuidv7Generator;
        this.maxSessionsPerUser = maxSessionsPerUser;
        // Fetch maxSessionsPerUser + 1 to detect limit overflow without COUNT(*)
        this.pageableForMaxSessions = PageRequest.of(0, maxSessionsPerUser + 1);
    }

    /**
     * Supplies the DAO component for data access operations.
     *
     * @return the {@link IGenericDAO} corresponding to the entity type {@code Session}
     */
    @Override
    protected IGenericDAO<Session, UUID> getDAO() {
        return dao;
    }

    /**
     * Provides a human-readable entity name to be used in exception messages.
     *
     * @return the name of the entity "Session"
     */
    @Override
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

    @Override
    public Optional<Session> findByUniqueProperties(
            UUID userId, String userAgent, String clientId) {
        return dao.findByUniqueProperties(userId, userAgent, clientId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Override
    public Session createSession(UUID userId, String userAgent, String clientId, String clientIp) {
        Instant now = Instant.now();
        // Revoke existing session for same device/client before enforcing limits
        dao.findByUniqueProperties(userId, userAgent, clientId)
                .ifPresent(
                        actualSession ->
                                dao.revokeSessionByFamilyId(
                                        actualSession.getFamilyId(), now, "new-session"));
        List<Session> activeSessions =
                dao.findActiveSessionsByUserOrderByLastSeen(userId, pageableForMaxSessions);
        if (activeSessions.size() >= maxSessionsPerUser) {
            throw new ExceededSessionsException(maxSessionsPerUser);
        }
        User user = new User();
        user.setId(userId);
        Session newSession = new Session();
        newSession.setUser(user);
        newSession.setFamilyId(uuidv7Generator.generate());
        newSession.setCreatedAt(now);
        newSession.setIpAddress(clientIp);
        newSession.setUserAgent(userAgent);
        newSession.setClientId(clientId);
        return dao.save(newSession);
    }
}
