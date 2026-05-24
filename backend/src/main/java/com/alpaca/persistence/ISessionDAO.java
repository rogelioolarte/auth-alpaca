package com.alpaca.persistence;

import com.alpaca.entity.Session;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Data Access Object (DAO) interface for managing {@link Session} entities.
 *
 * <p>Extends {@link IGenericDAO} to inherit common CRUD operations and defines additional queries
 * specific to {@code Session} management.
 *
 * @see IGenericDAO
 */
public interface ISessionDAO extends IGenericDAO<Session, UUID> {

    void revokeSessionByFamilyId(UUID familyId, Instant revokedAt, String reason);

    Optional<Session> findByIdAndUserId(UUID id, UUID userId);

    Optional<Session> findSessionByFamilyId(UUID familyId);

    Optional<Session> findByUniqueProperties(
            UUID userId, String userAgent, String clientId, String ipAddress);

    Optional<Session> findFirstActiveSessionForUpdate(UUID userId);

    long countByUserIdAndRevokedFalse(UUID userId);

    void revokeSessionsByUserId(UUID userId, Instant revokedAt, String reason);

    Page<Session> findAllByUserId(UUID userId, Pageable pageable);
}
