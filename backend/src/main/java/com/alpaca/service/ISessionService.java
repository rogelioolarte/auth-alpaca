package com.alpaca.service;

import com.alpaca.entity.Session;
import java.time.Instant;
import java.util.UUID;

/**
 * Service interface for managing {@link Session} entities. Extends {@link IGenericService} to
 * inherit common CRUD operations.
 *
 * @see IGenericService
 */
public interface ISessionService extends IGenericService<Session, UUID> {

    int revokeSessionByFamilyId(UUID familyId, Instant revokedAt, String reason);

    Session findSessionByFamilyId(String familyId);
}
