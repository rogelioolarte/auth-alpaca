package com.alpaca.unit.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alpaca.entity.RefreshToken;
import com.alpaca.persistence.impl.RefreshTokenDAOImpl;
import com.alpaca.repository.RefreshTokenRepo;
import com.alpaca.resources.provider.RefreshTokenProvider;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link RefreshTokenDAOImpl} implementation. */
@ExtendWith(MockitoExtension.class)
class RefreshTokenDAOImplTest {

    @Mock private RefreshTokenRepo repo;

    @InjectMocks private RefreshTokenDAOImpl dao;

    private RefreshToken refreshToken;
    private UUID familyId;
    private UUID userId;
    private String tokenHash;
    private List<RefreshToken> refreshTokens;

    @BeforeEach
    void setUp() {
        refreshToken = RefreshTokenProvider.singleEntity();

        familyId = refreshToken.getFamilyId();
        tokenHash = refreshToken.getTokenHash();

        userId =
                refreshToken.getUser() != null ? refreshToken.getUser().getId() : UUID.randomUUID();

        refreshTokens = RefreshTokenProvider.listEntities();
    }

    @Test
    void existsByUniqueProperties_WhenTokenExists_ReturnsTrue() {
        when(repo.existsByTokenHash(tokenHash)).thenReturn(true);

        boolean result = dao.existsByUniqueProperties(refreshToken);

        assertTrue(result);

        verify(repo).existsByTokenHash(tokenHash);
    }

    @Test
    void existsByUniqueProperties_WhenTokenDoesNotExist_ReturnsFalse() {
        when(repo.existsByTokenHash(tokenHash)).thenReturn(false);

        boolean result = dao.existsByUniqueProperties(refreshToken);

        assertFalse(result);

        verify(repo).existsByTokenHash(tokenHash);
    }

    @Test
    void findByTokenHashSecure_WhenTokenExists_ReturnsRefreshToken() {
        when(repo.findByTokenHashSecure(tokenHash)).thenReturn(Optional.of(refreshToken));

        Optional<RefreshToken> result = dao.findByTokenHashSecure(tokenHash);

        assertTrue(result.isPresent());
        assertEquals(refreshToken, result.get());

        verify(repo).findByTokenHashSecure(tokenHash);
    }

    @Test
    void findByTokenHashSecure_WhenTokenDoesNotExist_ReturnsEmptyOptional() {
        when(repo.findByTokenHashSecure(tokenHash)).thenReturn(Optional.empty());

        Optional<RefreshToken> result = dao.findByTokenHashSecure(tokenHash);

        assertTrue(result.isEmpty());

        verify(repo).findByTokenHashSecure(tokenHash);
    }

    @Test
    void findFamilyIdByTokenHash_WhenFamilyIdExists_ReturnsFamilyId() {
        when(repo.findFamilyIdByTokenHash(tokenHash)).thenReturn(Optional.of(familyId));

        Optional<UUID> result = dao.findFamilyIdByTokenHash(tokenHash);

        assertTrue(result.isPresent());
        assertEquals(familyId, result.get());

        verify(repo).findFamilyIdByTokenHash(tokenHash);
    }

    @Test
    void findFamilyIdByTokenHash_WhenFamilyIdDoesNotExist_ReturnsEmptyOptional() {
        when(repo.findFamilyIdByTokenHash(tokenHash)).thenReturn(Optional.empty());

        Optional<UUID> result = dao.findFamilyIdByTokenHash(tokenHash);

        assertTrue(result.isEmpty());

        verify(repo).findFamilyIdByTokenHash(tokenHash);
    }

    @Test
    void revokeFamilyWithReason_DelegatesToRepository() {
        Instant revokedAt = Instant.now();
        String reason = "reuse-detected";

        dao.revokeFamilyWithReason(familyId, revokedAt, reason);

        verify(repo).revokeFamilyOnReuse(familyId, revokedAt, reason);
    }

    @Test
    void findAllByFamilyId_ReturnsRepositoryResult() {
        when(repo.findAllByFamilyId(familyId)).thenReturn(refreshTokens);

        List<RefreshToken> result = dao.findAllByFamilyId(familyId);

        assertEquals(refreshTokens, result);

        verify(repo).findAllByFamilyId(familyId);
    }

    @Test
    void revokeTokensByUserId_DelegatesToRepository() {
        Instant revokedAt = Instant.now();
        String reason = "manual-revocation";

        dao.revokeTokensByUserId(userId, revokedAt, reason);

        verify(repo).revokeTokensByUserId(userId, revokedAt, reason);
    }

    @Test
    void existsAllByIds_WhenAllIdsExist_ReturnsTrue() {
        Collection<UUID> ids = refreshTokens.stream().map(RefreshToken::getId).toList();

        when(repo.countEntitiesIds(ids)).thenReturn((long) ids.size());

        boolean result = dao.existsAllByIds(ids);

        assertTrue(result);

        verify(repo).countEntitiesIds(ids);
    }

    @Test
    void existsAllByIds_WhenNotAllIdsExist_ReturnsFalse() {
        Collection<UUID> ids = refreshTokens.stream().map(RefreshToken::getId).toList();

        when(repo.countEntitiesIds(ids)).thenReturn((long) ids.size() - 1L);

        boolean result = dao.existsAllByIds(ids);

        assertFalse(result);

        verify(repo).countEntitiesIds(ids);
    }
}
