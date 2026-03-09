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
 * @see GenericRepo
 */
@Repository
public interface RefreshTokenRepo extends GenericRepo<RefreshToken, UUID> {

    boolean existsByTokenHash(String hash);

    List<RefreshToken> findAllByFamilyId(UUID familyId);

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
               AND r.expiresAt > :revokedAt
               AND r.replacedBy IS NULL
            """)
    void revokeFamilyOnReuse(
            @Param("familyId") UUID familyId,
            @Param("revokedAt") Instant revokedAt,
            @Param("reason") String reason);

    /**
     * Revokes a specific refresh token by its identifier, if it is not already revoked.
     *
     * @param id the UUID of the refresh token to revoke
     * @param revokedAt timestamp at which revocation occurred
     * @param reason the reason for revocation
     * @return the number of tokens revoked (0 or 1)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            UPDATE RefreshToken r
               SET r.revoked = true,
                   r.revokedAt = :revokedAt,
                   r.revokeReason = :reason
             WHERE r.id = :id
             AND r.revoked = false
             AND r.replacedBy IS NULL
            """)
    int revokeByIdWithReason(
            @Param("id") UUID id,
            @Param("revokedAt") Instant revokedAt,
            @Param("reason") String reason);

    @Query(
            """
            SELECT COUNT(r) > 0
            FROM RefreshToken r
            WHERE r.familyId = :familyId
            AND r.revoked = false
            AND r.expiresAt > CURRENT_TIMESTAMP
            """)
    boolean existsActiveTokenInFamily(UUID familyId);
}
