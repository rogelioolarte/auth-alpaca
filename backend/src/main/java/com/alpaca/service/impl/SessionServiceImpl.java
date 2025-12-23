package com.alpaca.service.impl;

import com.alpaca.entity.Session;
import com.alpaca.persistence.IGenericDAO;
import com.alpaca.persistence.ISessionDAO;
import com.alpaca.service.IGenericService;
import com.alpaca.service.IRefreshTokenService;
import com.alpaca.service.ISessionService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
@RequiredArgsConstructor
public class SessionServiceImpl extends GenericServiceImpl<Session, UUID>
        implements ISessionService {

    private final ISessionDAO dao;

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
    public int revokeSessionByFamilyId(UUID familyId, Instant revokedAt, String reason) {
        return dao.revokeSessionByFamilyId(familyId, revokedAt, reason);
    }

    @Override
    public Session findSessionByFamilyId(String familyId) {
        return dao.findSessionByFamilyId(familyId);
    }
}
