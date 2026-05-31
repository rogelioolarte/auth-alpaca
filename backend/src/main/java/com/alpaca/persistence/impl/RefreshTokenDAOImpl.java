package com.alpaca.persistence.impl;

import com.alpaca.entity.RefreshToken;
import com.alpaca.persistence.IRefreshTokenDAO;
import com.alpaca.repository.CustomRepo;
import com.alpaca.repository.RefreshTokenRepo;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
     * Provides the {@link CustomRepo} instance backing the DAO operations for {@link RefreshToken}.
     *
     * @return the repository used to perform CRUD operations for {@link RefreshToken}
     */
    @Override
    @Generated
    protected CustomRepo<RefreshToken, UUID> getRepo() {
        return repo;
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
        if (refreshToken == null
                || refreshToken.getTokenHash() == null
                || refreshToken.getTokenHash().isBlank()) {
            return false;
        }
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
    public List<RefreshToken> findAllByFamilyId(UUID familyId) {
        return repo.findAllByFamilyId(familyId);
    }

    @Override
    public void revokeTokensByUserId(UUID userId, Instant revokedAt, String reason) {
        repo.revokeTokensByUserId(userId, revokedAt, reason);
    }
}
