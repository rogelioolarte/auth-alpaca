package com.alpaca.persistence;

import com.alpaca.entity.Session;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    Optional<Session> findSessionByFamilyId(UUID familyId);

    Optional<Session> findByUniqueProperties(
            UUID userId, String userAgent, String clientId, String ipAddress);

    List<Session> findActiveSessionsByUserOrderByLastSeen(UUID userId, Pageable pageable);
}
