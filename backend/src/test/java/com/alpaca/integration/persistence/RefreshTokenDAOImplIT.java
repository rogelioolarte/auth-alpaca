package com.alpaca.integration.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.entity.RefreshToken;
import com.alpaca.entity.User;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IRefreshTokenDAO;
import com.alpaca.persistence.impl.RefreshTokenDAOImpl;
import com.alpaca.repository.RefreshTokenRepo;
import com.alpaca.repository.UserRepo;
import com.alpaca.resources.RefreshTokenProvider;
import com.alpaca.resources.UserProvider;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import(RefreshTokenDAOImpl.class)
@DisplayName("RefreshTokenDAOImpl Integration Tests")
class RefreshTokenDAOImplIT {

    @Autowired private IRefreshTokenDAO dao;
    @Autowired private RefreshTokenRepo repo;
    @Autowired private UserRepo userRepo;

    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();
    }

    @Test
    @DisplayName("updateById: performs full update on associations and scalar fields")
    @Transactional
    void updateById_ShouldPerformFullUpdate() {
        // Arrange
        User user = userRepo.save(UserProvider.singleTemplate());
        RefreshToken initial = RefreshTokenProvider.singleTemplate();
        initial.setUser(user);
        RefreshToken persisted = repo.save(initial);

        User newUser = userRepo.save(UserProvider.singleTemplate());
        RefreshToken replacement = repo.save(RefreshTokenProvider.singleTemplate());

        RefreshToken updateData = new RefreshToken();
        updateData.setUser(newUser);
        updateData.setReplacedBy(replacement);
        updateData.setTokenJti(UUID.randomUUID());
        updateData.setFamilyId(UUID.randomUUID());
        updateData.setRevoked(true);
        updateData.setRevokedAt(Instant.now());
        updateData.setExpiresAt(Instant.now().plusSeconds(600));
        updateData.setLastUsedAt(Instant.now());
        updateData.setTokenHash("new-hash");
        updateData.setClientId("mobile-app");
        updateData.setIpAddress("192.168.1.1");
        updateData.setUserAgent("Darwin/24.0.0");
        updateData.setRevokeReason("Security Breach");

        // Act
        RefreshToken result = dao.updateById(updateData, persisted.getId());

        // Assert
        assertAll(
                () -> assertEquals(newUser.getId(), result.getUser().getId()),
                () -> assertEquals(replacement.getId(), result.getReplacedBy().getId()),
                () -> assertEquals(updateData.getTokenJti(), result.getTokenJti()),
                () -> assertEquals(updateData.getFamilyId(), result.getFamilyId()),
                () -> assertTrue(result.isRevoked()),
                () -> assertEquals(updateData.getRevokedAt(), result.getRevokedAt()),
                () -> assertEquals(updateData.getTokenHash(), result.getTokenHash()),
                () -> assertEquals("mobile-app", result.getClientId()),
                () -> assertEquals("Security Breach", result.getRevokeReason()));
    }

    @Test
    @DisplayName("updateById: handles edge cases like same associations and nulls")
    @Transactional
    void updateById_ShouldHandleEdgeCases() {
        User user = persistToken(UserProvider.singleTemplate());
        RefreshToken persisted = persistToken(RefreshTokenProvider.singleTemplate(), user);
        persisted.setUser(user);
        repo.saveAndFlush(persisted);

        // Update with same User ID - should not trigger branch logic
        RefreshToken updateWithSameUser = new RefreshToken();
        updateWithSameUser.setUser(user);

        RefreshToken result = dao.updateById(updateWithSameUser, persisted.getId());
        assertEquals(user.getId(), result.getUser().getId());

        // Update with null fields - should maintain existing values
        RefreshToken emptyUpdate = new RefreshToken();
        RefreshToken resultEmpty = dao.updateById(emptyUpdate, persisted.getId());
        assertNotNull(resultEmpty.getTokenHash());
    }

    @Test
    @DisplayName("updateById: throws NotFoundException for invalid ID")
    @Transactional
    void updateById_ShouldThrowNotFound() {
        RefreshToken data = new RefreshToken();
        UUID randomId = UUID.randomUUID();
        assertThrows(NotFoundException.class, () -> dao.updateById(data, randomId));
    }

    @Test
    @DisplayName("existsByUniqueProperties: validates existence by token hash")
    @Transactional
    void existsByUniqueProperties_ShouldWork() {
        User user = persistToken(UserProvider.singleTemplate());
        RefreshToken token = persistToken(RefreshTokenProvider.singleTemplate(), user);

        RefreshToken probe = new RefreshToken();
        probe.setTokenHash(token.getTokenHash());

        assertTrue(dao.existsByUniqueProperties(probe));
    }

    @Test
    @DisplayName("findByTokenHashSecure: retrieves token correctly")
    @Transactional
    void findByTokenHashSecure_ShouldReturnToken() {
        User user = persistToken(UserProvider.singleTemplate());
        RefreshToken token = persistToken(RefreshTokenProvider.singleTemplate(), user);

        Optional<RefreshToken> found = dao.findByTokenHashSecure(token.getTokenHash());

        assertTrue(found.isPresent());
        assertEquals(token.getId(), found.get().getId());
    }

    @Test
    @DisplayName("revokeFamilyWithReason: updates all tokens in a family")
    @Transactional
    void revokeFamilyWithReason_ShouldRevokeAll() {
        UUID familyId = UUID.randomUUID();
        Instant revokedAt = Instant.now();

        User user = persistToken(UserProvider.singleTemplate());
        RefreshToken t1 = RefreshTokenProvider.singleTemplate();
        t1.setUser(user);
        t1.setCreatedAt(now);
        t1.setFamilyId(familyId);
        t1.setTokenHash("hash1");

        RefreshToken t2 = RefreshTokenProvider.singleTemplate();
        t2.setUser(user);
        t2.setCreatedAt(now);
        t2.setFamilyId(familyId);
        t2.setTokenHash("hash2");

        repo.saveAll(List.of(t1, t2));

        dao.revokeFamilyWithReason(familyId, revokedAt, "Compromised");

        List<RefreshToken> family = repo.findAllByFamilyId(familyId);
        assertTrue(
                family.stream()
                        .allMatch(t -> t.isRevoked() && "Compromised".equals(t.getRevokeReason())));
    }

    @Test
    @DisplayName("existsAllByIds: verifies presence of multiple IDs")
    @Transactional
    void existsAllByIds_ShouldVerifyCount() {
        User user = persistToken(UserProvider.singleTemplate());
        RefreshToken t1 = persistToken(RefreshTokenProvider.singleTemplate(), user);
        RefreshToken t2 = persistToken(RefreshTokenProvider.alternativeTemplate(), user);

        assertTrue(dao.existsAllByIds(List.of(t1.getId(), t2.getId())));
        assertFalse(dao.existsAllByIds(List.of(t1.getId(), UUID.randomUUID())));
    }

    private RefreshToken persistToken(RefreshToken token, User user) {
        token.setCreatedAt(now);
        token.setUser(user);
        return repo.save(token);
    }

    private User persistToken(User user) {
        user.setCreatedAt(now);
        return userRepo.save(user);
    }
}
