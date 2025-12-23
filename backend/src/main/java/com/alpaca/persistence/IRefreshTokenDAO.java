package com.alpaca.persistence;

import com.alpaca.entity.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Data Access Object (DAO) interface for managing {@link RefreshToken} entities.
 *
 * <p>Extends {@link IGenericDAO} to inherit common CRUD operations and defines additional queries
 * specific to {@code RefreshToken} management.
 *
 * @see IGenericDAO
 */
public interface IRefreshTokenDAO extends IGenericDAO<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHashSecure(String hash);

    Optional<UUID> findFamilyIdByTokenHash(String hash);

    void revokeFamilyWithReason(UUID familyId, Instant revokedAt, String reason);

    int revokeByIdWithReason(UUID id, Instant revokedAt, String reason);
}
