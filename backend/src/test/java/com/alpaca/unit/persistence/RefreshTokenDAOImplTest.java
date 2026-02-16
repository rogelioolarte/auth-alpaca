package com.alpaca.unit.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.alpaca.entity.RefreshToken;
import com.alpaca.entity.User;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.impl.RefreshTokenDAOImpl;
import com.alpaca.repository.RefreshTokenRepo;
import java.time.Instant;
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
    private final UUID tokenId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        existingToken = new RefreshToken();
        existingToken.setId(tokenId);
    }

    // --- updateById Tests ---

    @Test
    @DisplayName("Should throw NotFoundException when refresh token does not exist")
    void updateById_WhenNotFound_ThrowsException() {
        when(repo.findById(tokenId)).thenReturn(Optional.empty());
        RefreshToken updateData = new RefreshToken();

        assertThrows(NotFoundException.class, () -> dao.updateById(updateData, tokenId));
        verify(repo).findById(tokenId);
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("Should update all fields when valid and new data is provided")
    void updateById_WhenDataIsNew_UpdatesAllFields() {
        // Arrange
        User oldUser = new User();
        oldUser.setId(UUID.randomUUID());
        existingToken.setUser(oldUser);
        existingToken.setRevoked(false);

        User newUser = new User();
        newUser.setId(UUID.randomUUID());

        RefreshToken replacementToken = new RefreshToken();
        replacementToken.setId(UUID.randomUUID());

        Instant now = Instant.now();
        UUID jti = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();

        RefreshToken updateData = new RefreshToken();
        updateData.setUser(newUser);
        updateData.setReplacedBy(replacementToken);
        updateData.setTokenJti(jti);
        updateData.setFamilyId(familyId);
        updateData.setRevoked(true);
        updateData.setRevokedAt(now);
        updateData.setExpiresAt(now.plusSeconds(3600));
        updateData.setLastUsedAt(now);
        updateData.setTokenHash("new-hash");
        updateData.setClientId("client-xyz");
        updateData.setIpAddress("10.0.0.1");
        updateData.setUserAgent("Mozilla/5.0");
        updateData.setRevokeReason("Token Rotated");

        when(repo.findById(tokenId)).thenReturn(Optional.of(existingToken));
        when(repo.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        RefreshToken result = dao.updateById(updateData, tokenId);

        // Assert
        assertAll(
                () -> assertEquals(newUser.getId(), result.getUser().getId()),
                () -> assertEquals(replacementToken.getId(), result.getReplacedBy().getId()),
                () -> assertEquals(jti, result.getTokenJti()),
                () -> assertEquals(familyId, result.getFamilyId()),
                () -> assertTrue(result.getRevoked()),
                () -> assertEquals(now, result.getRevokedAt()),
                () -> assertEquals("new-hash", result.getTokenHash()),
                () -> assertEquals("Token Rotated", result.getRevokeReason()));
        verify(repo).save(existingToken);
    }

    @Test
    @DisplayName("Should skip association updates when IDs are identical or input is null")
    void updateById_WhenAssociationsAreIdentical_SkipsUpdate() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        existingToken.setUser(user);
        existingToken.setReplacedBy(null);

        RefreshToken updateData = new RefreshToken();
        // Case 1: Identical User ID
        User sameUser = new User();
        sameUser.setId(userId);
        updateData.setUser(sameUser);

        // Case 2: ReplacedBy input has null ID
        RefreshToken invalidReplacement = new RefreshToken();
        updateData.setReplacedBy(invalidReplacement);

        when(repo.findById(tokenId)).thenReturn(Optional.of(existingToken));
        when(repo.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        RefreshToken result = dao.updateById(updateData, tokenId);

        // Assert
        assertSame(user, result.getUser());
        assertNull(result.getReplacedBy());
        verify(repo).save(existingToken);
    }

    @Test
    @DisplayName("Should skip textual updates when input is null or blank")
    void updateById_WhenTextIsInvalid_SkipsUpdate() {
        existingToken.setTokenHash("original-hash");

        RefreshToken updateData = new RefreshToken();
        updateData.setTokenHash("");
        updateData.setClientId(null);
        updateData.setIpAddress("   ");

        when(repo.findById(tokenId)).thenReturn(Optional.of(existingToken));
        when(repo.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshToken result = dao.updateById(updateData, tokenId);

        assertEquals("original-hash", result.getTokenHash());
        verify(repo).save(existingToken);
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
}
