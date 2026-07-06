package com.alpaca.persistence;

import com.alpaca.entity.RefreshToken;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data Access Object (DAO) interface for managing {@link RefreshToken} entities.
 *
 * <p>Extends {@link IGenericDAO} to inherit common CRUD operations and defines additional queries
 * specific to {@code RefreshToken} management, including secure hash-based lookup, token family
 * resolution, and bulk revocation on reuse detection.
 *
 * @see IGenericDAO
 */
public interface IRefreshTokenDAO extends IGenericDAO<RefreshToken, UUID> {

    /**
     * Retrieves a refresh token by its hash with a pessimistic write lock to prevent race
     * conditions during validation or rotation.
     *
     * @param hash the stored token hash
     * @return An {@link Optional} containing the matching token, otherwise empty
     */
    Optional<RefreshToken> findByTokenHashSecure(String hash);

    /**
     * Resolves the token family ID from a single token hash.
     *
     * <p>Used as the first step in reuse detection: determine which family the presented token
     * belongs to, then inspect all family members.
     *
     * @param hash the stored token hash
     * @return An {@link Optional} containing the family UUID, otherwise empty
     */
    Optional<UUID> findFamilyIdByTokenHash(String hash);

    /**
     * Revokes all non-revoked, non-replaced tokens in a family following a detected reuse.
     *
     * <p>When a token is presented after its successor has already been issued, the family is
     * considered compromised and all remaining valid tokens are revoked.
     *
     * @param familyId the token family to revoke
     * @param revokedAt timestamp of revocation
     * @param reason reason recorded on each revoked token
     */
    void revokeFamilyWithReason(UUID familyId, Instant revokedAt, String reason);

    /**
     * Retrieves all refresh tokens belonging to a given token family.
     *
     * @param familyId the token family identifier
     * @return a list of tokens in the family, empty if none found
     */
    List<RefreshToken> findAllByFamilyId(UUID familyId);

    /**
     * Revokes all active, non-replaced tokens for a given user.
     *
     * @param userId the user whose tokens to revoke
     * @param revokedAt timestamp of revocation
     * @param reason reason recorded on each revoked token
     */
    void revokeTokensByUserId(UUID userId, Instant revokedAt, String reason);
}
