package com.alpaca.persistence;

import com.alpaca.entity.Session;
import java.time.Instant;
import java.util.UUID;

/**
 * Data Access Object (DAO) interface for managing {@link Session} entities.
 *
 * <p>Extends {@link IGenericDAO} to inherit common CRUD operations and defines additional queries
 * specific to {@code Session} management.
 *
 * @see IGenericDAO
 */
public interface ISessionDAO extends IGenericDAO<Session, UUID> {

    int revokeSessionByFamilyId(UUID familyId, Instant revokedAt, String reason);

    Session findSessionByFamilyId(String familyId);
}
