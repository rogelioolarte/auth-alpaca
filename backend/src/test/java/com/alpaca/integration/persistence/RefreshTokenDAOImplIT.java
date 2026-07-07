package com.alpaca.integration.persistence;

import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.entity.RefreshToken;
import com.alpaca.entity.User;
import com.alpaca.persistence.IRefreshTokenDAO;
import com.alpaca.persistence.impl.RefreshTokenDAOImpl;
import com.alpaca.repository.RefreshTokenRepo;
import com.alpaca.repository.UserRepo;
import com.alpaca.resources.provider.RefreshTokenProvider;
import com.alpaca.resources.provider.UserProvider;
import com.alpaca.resources.utility.DataJpaIntegrationTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link RefreshTokenDAOImpl} */
@DataJpaIntegrationTest
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

    private User buildUser() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);
        return user;
    }

    private User buildAlternativeUser() {
        User user = UserProvider.alternativeTemplate();
        user.setCreatedAt(now);
        return user;
    }

    private RefreshToken buildRefreshToken() {
        RefreshToken token = RefreshTokenProvider.singleTemplate();
        token.setCreatedAt(now);
        return token;
    }

    private RefreshToken buildAlternativeRefreshToken() {
        RefreshToken token = RefreshTokenProvider.alternativeTemplate();
        token.setCreatedAt(now);
        return token;
    }

    private User persistUser(User user) {
        return userRepo.save(user);
    }

    private RefreshToken persistRefreshToken(RefreshToken token, User user) {
        token.setUser(user);
        return repo.save(token);
    }

    @Test
    @Transactional
    @DisplayName("existsByUniqueProperties: should return true when token hash exists")
    void existsByUniqueProperties_ShouldReturnTrue_WhenTokenHashExists() {

        User user = persistUser(buildUser());

        RefreshToken token = buildRefreshToken();
        token.setTokenHash("existing-hash");

        persistRefreshToken(token, user);

        RefreshToken probe = new RefreshToken();
        probe.setTokenHash("existing-hash");

        boolean result = dao.existsByUniqueProperties(probe);

        assertTrue(result);
    }

    @Test
    @Transactional
    @DisplayName("existsByUniqueProperties: should return false when token hash does not exist")
    void existsByUniqueProperties_ShouldReturnFalse_WhenTokenHashDoesNotExist() {

        RefreshToken probe = new RefreshToken();
        probe.setTokenHash("missing-hash");

        boolean result = dao.existsByUniqueProperties(probe);

        assertFalse(result);
    }

    @Test
    @Transactional
    @DisplayName("findByTokenHashSecure: should return token when hash exists")
    void findByTokenHashSecure_ShouldReturnToken_WhenHashExists() {

        User user = persistUser(buildUser());

        RefreshToken token = buildRefreshToken();
        token.setTokenHash("secure-hash");

        RefreshToken saved = persistRefreshToken(token, user);

        Optional<RefreshToken> result = dao.findByTokenHashSecure("secure-hash");

        assertTrue(result.isPresent());
        assertEquals(saved.getId(), result.get().getId());
    }

    @Test
    @Transactional
    @DisplayName("findByTokenHashSecure: should return empty when hash does not exist")
    void findByTokenHashSecure_ShouldReturnEmpty_WhenHashDoesNotExist() {

        Optional<RefreshToken> result = dao.findByTokenHashSecure("unknown-hash");

        assertTrue(result.isEmpty());
    }

    @Test
    @Transactional
    @DisplayName("findFamilyIdByTokenHash: should return family id when token exists")
    void findFamilyIdByTokenHash_ShouldReturnFamilyId_WhenTokenExists() {

        User user = persistUser(buildUser());

        UUID familyId = UUID.randomUUID();

        RefreshToken token = buildRefreshToken();
        token.setFamilyId(familyId);
        token.setTokenHash("family-hash");

        persistRefreshToken(token, user);

        Optional<UUID> result = dao.findFamilyIdByTokenHash("family-hash");

        assertTrue(result.isPresent());
        assertEquals(familyId, result.get());
    }

    @Test
    @Transactional
    @DisplayName("findFamilyIdByTokenHash: should return empty when token does not exist")
    void findFamilyIdByTokenHash_ShouldReturnEmpty_WhenTokenDoesNotExist() {

        Optional<UUID> result = dao.findFamilyIdByTokenHash("missing-family-hash");

        assertTrue(result.isEmpty());
    }

    @Test
    @Transactional
    @DisplayName("revokeFamilyWithReason: should revoke all tokens in same family")
    void revokeFamilyWithReason_ShouldRevokeAllTokensInFamily() {

        User user = persistUser(buildUser());

        UUID familyId = UUID.randomUUID();

        RefreshToken tokenOne = buildRefreshToken();
        tokenOne.setFamilyId(familyId);
        tokenOne.setTokenHash("family-hash-1");
        tokenOne.setRevoked(false);

        RefreshToken tokenTwo = buildAlternativeRefreshToken();
        tokenTwo.setCreatedAt(now.plusSeconds(1));
        tokenTwo.setFamilyId(familyId);
        tokenTwo.setTokenHash("family-hash-2");
        tokenTwo.setRevoked(false);

        persistRefreshToken(tokenOne, user);
        persistRefreshToken(tokenTwo, user);

        Instant revokedAt = now.plusSeconds(500);

        dao.revokeFamilyWithReason(familyId, revokedAt, "reuse-detected");

        List<RefreshToken> result = dao.findAllByFamilyId(familyId);

        assertEquals(2, result.size());

        assertThat(result)
                .allSatisfy(
                        token -> {
                            assertThat(token.isRevoked()).isTrue();
                            assertThat(token.getRevokedAt())
                                    .isCloseTo(revokedAt, within(1, ChronoUnit.SECONDS));
                            assertThat(token.getRevokeReason()).isEqualTo("reuse-detected");
                        });
    }

    @Test
    @Transactional
    @DisplayName("findAllByFamilyId: should return all tokens for family")
    void findAllByFamilyId_ShouldReturnAllTokensForFamily() {

        User user = persistUser(buildUser());

        UUID familyId = UUID.randomUUID();

        RefreshToken tokenOne = buildRefreshToken();
        tokenOne.setFamilyId(familyId);
        tokenOne.setTokenHash("all-family-hash-1");

        RefreshToken tokenTwo = buildAlternativeRefreshToken();
        tokenTwo.setCreatedAt(now.plusSeconds(1));
        tokenTwo.setFamilyId(familyId);
        tokenTwo.setTokenHash("all-family-hash-2");

        persistRefreshToken(tokenOne, user);
        persistRefreshToken(tokenTwo, user);

        List<RefreshToken> result = dao.findAllByFamilyId(familyId);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(token -> familyId.equals(token.getFamilyId())));
    }

    @Test
    @Transactional
    @DisplayName("findAllByFamilyId: should return empty list when family does not exist")
    void findAllByFamilyId_ShouldReturnEmpty_WhenFamilyDoesNotExist() {

        List<RefreshToken> result = dao.findAllByFamilyId(UUID.randomUUID());

        assertTrue(result.isEmpty());
    }

    @Test
    @Transactional
    @DisplayName("revokeTokensByUserId: should revoke all tokens for user")
    void revokeTokensByUserId_ShouldRevokeAllTokensForUser() {

        User user = persistUser(buildUser());

        User anotherUser = persistUser(buildAlternativeUser());

        RefreshToken userTokenOne = buildRefreshToken();
        userTokenOne.setTokenHash("user-token-1");
        userTokenOne.setRevoked(false);

        RefreshToken userTokenTwo = buildAlternativeRefreshToken();
        userTokenTwo.setCreatedAt(now.plusSeconds(1));
        userTokenTwo.setTokenHash("user-token-2");
        userTokenTwo.setRevoked(false);

        RefreshToken anotherUserToken = buildRefreshToken();
        anotherUserToken.setCreatedAt(now.plusSeconds(2));
        anotherUserToken.setTokenHash("another-user-token");
        anotherUserToken.setRevoked(false);

        persistRefreshToken(userTokenOne, user);
        persistRefreshToken(userTokenTwo, user);
        persistRefreshToken(anotherUserToken, anotherUser);

        Instant revokedAt = now.plusSeconds(1000);

        dao.revokeTokensByUserId(user.getId(), revokedAt, "manual-revoke");

        List<RefreshToken> revokedTokens =
                repo.findAll().stream()
                        .filter(token -> token.getUser().getId().equals(user.getId()))
                        .toList();

        assertEquals(2, revokedTokens.size());

        assertThat(revokedTokens)
                .allSatisfy(
                        token -> {
                            AssertionsForClassTypes.assertThat(token.isRevoked()).isTrue();
                            AssertionsForClassTypes.assertThat(token.getRevokedAt())
                                    .isCloseTo(revokedAt, within(1, ChronoUnit.SECONDS));
                            AssertionsForClassTypes.assertThat(token.getRevokeReason())
                                    .isEqualTo("manual-revoke");
                        });

        RefreshToken unaffectedToken =
                repo.findByTokenHashSecure("another-user-token").orElseThrow();

        assertFalse(unaffectedToken.isRevoked());
    }

    @Test
    @Transactional
    @DisplayName("existsAllByIds: should return true when all ids exist")
    void existsAllByIds_ShouldReturnTrue_WhenAllIdsExist() {

        User user = persistUser(buildUser());

        RefreshToken tokenOne = persistRefreshToken(buildRefreshToken(), user);

        RefreshToken tokenTwo = persistRefreshToken(buildAlternativeRefreshToken(), user);

        boolean result = dao.existsAllByIds(List.of(tokenOne.getId(), tokenTwo.getId()));

        assertTrue(result);
    }

    @Test
    @Transactional
    @DisplayName("existsAllByIds: should return false when some ids do not exist")
    void existsAllByIds_ShouldReturnFalse_WhenSomeIdsDoNotExist() {

        User user = persistUser(buildUser());

        RefreshToken token = persistRefreshToken(buildRefreshToken(), user);

        boolean result = dao.existsAllByIds(List.of(token.getId(), UUID.randomUUID()));

        assertFalse(result);
    }

    @Test
    @Transactional
    @DisplayName("existsAllByIds: should return true for empty collection")
    void existsAllByIds_ShouldReturnTrue_WhenCollectionIsEmpty() {

        boolean result = dao.existsAllByIds(List.of());

        assertTrue(result);
    }
}
