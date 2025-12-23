package com.alpaca.persistence.impl;

import com.alpaca.entity.RefreshToken;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IRefreshTokenDAO;
import com.alpaca.repository.GenericRepo;
import com.alpaca.repository.RefreshTokenRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of the {@link IRefreshTokenDAO} interface for managing {@link RefreshToken}
 * entities. This class extends the generic DAO implementation ({@link GenericDAOImpl}) to provide
 * standard CRUD operations along with custom refresh-token-specific persistence logic.
 *
 * <p>Update operations in this DAO perform selective field updates: only non-null/meaningful values
 * (and values that differ from the existing ones) are applied to the persisted entity. The class
 * relies on helper methods provided by the superclass (e.g. {@code updateIfNotNull}, {@code
 * updateIfDifferent}, {@code updateTextIfExists}) to centralize common update semantics.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenDAOImpl extends GenericDAOImpl<RefreshToken, UUID>
        implements IRefreshTokenDAO {

    private final RefreshTokenRepo repo;

    /**
     * Provides the {@link GenericRepo} instance backing the DAO operations for {@link
     * RefreshToken}.
     *
     * @return the repository used to perform CRUD operations for {@link RefreshToken}
     */
    @Override
    protected GenericRepo<RefreshToken, UUID> getRepo() {
        return repo;
    }

    /**
     * Returns the {@code Class} object representing the {@link RefreshToken} entity managed by this
     * DAO.
     *
     * @return {@code RefreshToken.class}
     */
    @Override
    protected Class<RefreshToken> getEntity() {
        return RefreshToken.class;
    }

    /**
     * Updates an existing {@link RefreshToken} identified by {@code id} with values supplied in
     * {@code refreshToken}. This method applies selective updates:
     *
     * <ul>
     *   <li>Associations such as {@code user} and {@code replacedBy} are updated only if the
     *       incoming association is non-null, has an identifier, and differs from the stored one.
     *   <li>Scalar and timestamp fields are updated through helper methods that check for
     *       non-nullity and inequality (e.g. {@code updateIfNotNull}, {@code updateIfDifferent}).
     *   <li>Textual fields are updated only when incoming text is present and differs from the
     *       existing value (see {@code updateTextIfExists}).
     * </ul>
     *
     * <p>The specific fields that may be updated include: {@code user}, {@code replacedBy}, {@code
     * tokenJti}, {@code familyId}, {@code revoked}, {@code revokedAt}, {@code expiresAt}, {@code
     * lastUsedAt}, {@code tokenHash}, {@code clientId}, {@code ipAddress}, {@code userAgent}, and
     * {@code revokeReason}.
     *
     * @param refreshToken the {@link RefreshToken} containing new values to apply; may include
     *     nulls for fields that should remain unchanged
     * @param id the unique identifier of the persisted {@link RefreshToken} to update
     * @return the updated and saved {@link RefreshToken} instance
     * @throws NotFoundException if a {@link RefreshToken} with the supplied {@code id} does not
     *     exist
     */
    @Override
    public RefreshToken updateById(RefreshToken refreshToken, UUID id) {
        RefreshToken existingRefreshToken =
                findById(id)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                String.format(
                                                        "%s with ID %s not found",
                                                        getEntity().getName(), id.toString())));
        if (refreshToken.getUser() != null
                && refreshToken.getUser().getId() != null
                && !existingRefreshToken.getUser().getId().equals(refreshToken.getUser().getId())) {
            existingRefreshToken.setUser(refreshToken.getUser());
        }
        if (refreshToken.getReplacedBy() != null
                && refreshToken.getReplacedBy().getId() != null
                && !existingRefreshToken
                        .getReplacedBy()
                        .getId()
                        .equals(refreshToken.getReplacedBy().getId())) {
            existingRefreshToken.setReplacedBy(refreshToken.getReplacedBy());
        }

        updateIfNotNull(
                existingRefreshToken.getTokenJti(),
                refreshToken.getTokenJti(),
                existingRefreshToken::setTokenJti);
        updateIfNotNull(
                existingRefreshToken.getFamilyId(),
                refreshToken.getFamilyId(),
                existingRefreshToken::setFamilyId);
        updateIfDifferent(
                existingRefreshToken.getRevoked(),
                refreshToken.getRevoked(),
                existingRefreshToken::setRevoked);
        updateIfNotNull(
                existingRefreshToken.getRevokedAt(),
                refreshToken.getRevokedAt(),
                existingRefreshToken::setRevokedAt);
        updateIfNotNull(
                existingRefreshToken.getExpiresAt(),
                refreshToken.getExpiresAt(),
                existingRefreshToken::setExpiresAt);
        updateIfNotNull(
                existingRefreshToken.getLastUsedAt(),
                refreshToken.getLastUsedAt(),
                existingRefreshToken::setLastUsedAt);

        updateTextIfExists(
                existingRefreshToken.getTokenHash(),
                refreshToken.getTokenHash(),
                existingRefreshToken::setTokenHash);
        updateTextIfExists(
                existingRefreshToken.getClientId(),
                refreshToken.getClientId(),
                existingRefreshToken::setClientId);
        updateTextIfExists(
                existingRefreshToken.getIpAddress(),
                refreshToken.getIpAddress(),
                existingRefreshToken::setIpAddress);
        updateTextIfExists(
                existingRefreshToken.getUserAgent(),
                refreshToken.getUserAgent(),
                existingRefreshToken::setUserAgent);
        updateTextIfExists(
                existingRefreshToken.getRevokeReason(),
                refreshToken.getRevokeReason(),
                existingRefreshToken::setRevokeReason);
        return save(existingRefreshToken);
    }

    /**
     * Determines whether a {@link RefreshToken} with the same unique properties already exists in
     * the persistence store.
     *
     * @param refreshToken the {@link RefreshToken} whose unique properties should be checked
     * @return {@code true} if a refresh token with equivalent unique properties exists, {@code
     *     false} otherwise
     */
    @Override
    public boolean existsByUniqueProperties(RefreshToken refreshToken) {
        return repo.existsByTokenHash(refreshToken.getTokenHash());
    }

    @Override
    public Optional<RefreshToken> findByTokenHashSecure(String hash) {
        return repo.findByTokenHashSecure(hash);
    }

    @Override
    public Optional<UUID> findFamilyIdByTokenHash(String hash) {
        return repo.findFamilyIdByTokenHash(hash);
    }

    @Override
    public void revokeFamilyWithReason(UUID familyId, Instant revokedAt, String reason) {
        repo.revokeFamilyOnReuse(familyId, revokedAt, reason);
    }

    @Override
    public int revokeByIdWithReason(UUID id, Instant revokedAt, String reason) {
        return repo.revokeByIdWithReason(id, revokedAt, reason);
    }
}
