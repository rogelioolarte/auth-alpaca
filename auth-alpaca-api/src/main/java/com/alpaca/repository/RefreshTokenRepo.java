package com.alpaca.repository;

import com.alpaca.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link RefreshToken} entities.
 *
 * <p>Extends {@code GenericRepo} to provide standard CRUD operations. Defines additional methods
 * specific to refresh token handling, including secure lookup and revocation operations with
 * relevant locking and modification behavior.
 *
 * <p>These operations support refresh token lifecycle management such as:
 *
 * <ul>
 *   <li>Securely finding a token by its hash with pessimistic locking.
 *   <li>Revoking a whole refresh token family on suspected reuse.
 *   <li>Revoking an individual refresh token with a specified reason.
 * </ul>
 *
 * @see CustomRepo
 */
@Repository
public interface RefreshTokenRepo extends CustomRepo<RefreshToken, UUID> {

    /**
     * Checks whether a refresh token with the given hash exists in the database.
     *
     * <p>Provides a lightweight existence check without loading the full entity, useful during
     * initial validation before full token processing.
     *
     * @param hash the stored token hash
     * @return {@code true} if a token with the given hash exists, {@code false} otherwise
     */
    boolean existsByTokenHash(String hash);

    /**
     * Retrieves all refresh tokens belonging to the same token family.
     *
     * <p>A token family groups related tokens generated through successive rotations. This method
     * is used during reuse detection to examine all tokens sharing the same family identifier.
     *
     * @param familyId the token family identifier
     * @return a list of refresh tokens in the family, empty if none found
     */
    List<RefreshToken> findAllByFamilyId(UUID familyId);

    /**
     * Resolves the token family identifier from a single token hash.
     *
     * <p>This is typically the first step in reuse detection: given the hash of the token being
     * presented, resolve its family ID to then inspect all family members.
     *
     * @param hash the stored token hash
     * @return An {@link Optional} containing the family UUID if found, otherwise empty
     */
    @Query(
            """
            SELECT r.familyId
            FROM RefreshToken r
            WHERE r.tokenHash = :hash
            """)
    Optional<UUID> findFamilyIdByTokenHash(@Param("hash") String hash);

    /**
     * Retrieves a refresh token by its hashed value in a secure manner.
     *
     * <p>Applies a pessimistic write lock to prevent concurrent reads or updates during validation
     * and processing to mitigate race conditions.
     *
     * @param hash the stored token hash
     * @return an {@code Optional} containing the matching {@code RefreshToken}, or {@code
     *     Optional.empty()} if none found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0")})
    @Query("SELECT r from RefreshToken r WHERE r.tokenHash = :hash")
    Optional<RefreshToken> findByTokenHashSecure(@Param("hash") String hash);

    /**
     * Revokes all non-revoked refresh tokens belonging to the same family identifier.
     *
     * <p>This is typically used to prevent reuse of refresh token families following a compromise
     * or refresh token theft event.
     *
     * @param familyId the unique family identifier shared by related refresh tokens
     * @param revokedAt timestamp when the revocation was executed
     * @param reason textual reason for revocation
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            UPDATE RefreshToken r
               SET r.revoked = true,
                   r.revokedAt = :revokedAt,
                   r.revokeReason = :reason
             WHERE r.familyId = :familyId
               AND r.revoked = false
               AND r.replacedBy IS NULL
            """)
    void revokeFamilyOnReuse(
            @Param("familyId") UUID familyId,
            @Param("revokedAt") Instant revokedAt,
            @Param("reason") String reason);

    /**
     * Revokes all non-revoked, non-replaced refresh tokens belonging to a given user.
     *
     * <p>Used when an administrative action or global logout requires invalidating every active
     * refresh token for a user. Tokens that have already been replaced by a successor ({@code
     * replacedBy IS NOT NULL}) are excluded to avoid interfering with active rotations.
     *
     * @param userId the user whose tokens will be revoked
     * @param revokedAt timestamp when the revocation was executed
     * @param reason textual reason for revocation
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            UPDATE RefreshToken r
               SET r.revoked = true,
                   r.revokedAt = :revokedAt,
                   r.revokeReason = :reason
             WHERE r.user.id = :userId
               AND r.revoked = false
               AND r.replacedBy IS NULL
            """)
    void revokeTokensByUserId(
            @Param("userId") UUID userId,
            @Param("revokedAt") Instant revokedAt,
            @Param("reason") String reason);

    /**
     * Bulk-deletes all refresh tokens that are marked as revoked.
     *
     * <p>Intended for scheduled cleanup (e.g. daily purge of stale revoked tokens). Runs a single
     * bulk DELETE statement without loading entities.
     *
     * @return the number of deleted rows
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM RefreshToken r WHERE r.revoked = true")
    int deleteRevoked();
}
