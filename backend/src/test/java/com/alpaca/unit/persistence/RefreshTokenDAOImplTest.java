package com.alpaca.unit.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alpaca.entity.RefreshToken;
import com.alpaca.persistence.impl.RefreshTokenDAOImpl;
import com.alpaca.repository.RefreshTokenRepo;
import com.alpaca.resources.RefreshTokenProvider;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link RefreshTokenDAOImpl}. Ensures 100% coverage of custom persistence logic and
 * branching.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenDAOImplTest {

    @Mock private RefreshTokenRepo repo;

    @InjectMocks private RefreshTokenDAOImpl dao;

    private RefreshToken existingToken;
    private List<RefreshToken> entities;
    private List<UUID> ids;
    private final UUID tokenId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        existingToken = new RefreshToken();
        existingToken.setId(tokenId);
        entities = RefreshTokenProvider.listEntities();
        ids = entities.stream().map(RefreshToken::getId).toList();
    }

    // --- existsByUniqueProperties ---

    @Test
    @DisplayName("Should check existence by token hash")
    void existsByUniqueProperties_ChecksByTokenHash() {
        RefreshToken token = new RefreshToken();
        token.setTokenHash("hash-123");

        when(repo.existsByTokenHash("hash-123")).thenReturn(true);

        assertTrue(dao.existsByUniqueProperties(token));
        verify(repo).existsByTokenHash("hash-123");
    }

    // --- Custom Repository Delegations ---

    @Test
    @DisplayName("Should delegate findByTokenHashSecure to repo")
    void findByTokenHashSecure_DelegatesToRepo() {
        String hash = "secure-hash";
        when(repo.findByTokenHashSecure(hash)).thenReturn(Optional.of(existingToken));

        Optional<RefreshToken> result = dao.findByTokenHashSecure(hash);

        assertTrue(result.isPresent());
        verify(repo).findByTokenHashSecure(hash);
    }

    @Test
    @DisplayName("Should delegate findFamilyIdByTokenHash to repo")
    void findFamilyIdByTokenHash_DelegatesToRepo() {
        String hash = "family-hash";
        UUID familyId = UUID.randomUUID();
        when(repo.findFamilyIdByTokenHash(hash)).thenReturn(Optional.of(familyId));

        UUID result = dao.findFamilyIdByTokenHash(hash).orElse(null);

        assertEquals(familyId, result);
        verify(repo).findFamilyIdByTokenHash(hash);
    }

    @Test
    @DisplayName("Should delegate revokeFamilyWithReason to repo using correct parameter mapping")
    void revokeFamilyWithReason_DelegatesToRepoMapping() {
        UUID familyId = UUID.randomUUID();
        Instant now = Instant.now();
        String reason = "Compromised";

        dao.revokeFamilyWithReason(familyId, now, reason);

        // Note: The DAO calls repo.revokeFamilyOnReuse
        verify(repo).revokeFamilyOnReuse(familyId, now, reason);
    }

    @Test
    @DisplayName("findAllByFamilyId: Should return all entities by Id")
    void findAllByFamilyId_Success() {
        UUID id = UUID.randomUUID();
        when(repo.findAllByFamilyId(id)).thenReturn(entities);
        assertThat(dao.findAllByFamilyId(id)).isEqualTo(entities);
    }

    @Test
    @DisplayName("existsAllByIds: Should compare input size with repository count")
    void existsAllByIds_Coverage() {
        when(repo.countByIds(ids)).thenReturn((long) ids.size());
        assertThat(dao.existsAllByIds(ids)).isTrue();

        when(repo.countByIds(ids)).thenReturn(0L);
        assertThat(dao.existsAllByIds(ids)).isFalse();
    }
}
