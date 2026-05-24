package com.alpaca.service;

import com.alpaca.entity.Session;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing {@link Session} entities. Extends {@link IGenericService} to
 * inherit common CRUD operations.
 *
 * @see IGenericService
 */
public interface ISessionService extends IGenericService<Session, UUID> {

    void revokeSessionByFamilyId(UUID familyId, Instant revokedAt, String reason);

    Optional<Session> findSessionByFamilyId(UUID familyId);

    Session createSession(UUID userId, String userAgent, String clientId, String clientIp);

    void revokeSessionByUserIdAndId(UUID userId, UUID id);

    void revokeAllSessionsByUserId(UUID userId);

    Page<Session> findAllByUserId(UUID userId, Pageable pageable);
}
