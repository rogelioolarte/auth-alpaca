package com.alpaca.integration.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.entity.RefreshToken;
import com.alpaca.entity.User;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IRefreshTokenDAO;
import com.alpaca.persistence.impl.RefreshTokenDAOImpl;
import com.alpaca.repository.RefreshTokenRepo;
import com.alpaca.repository.UserRepo;
import com.alpaca.resources.UserProvider;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import(RefreshTokenDAOImpl.class)
class RefreshTokenDAOImplIT {

    @Autowired private IRefreshTokenDAO dao;

    @Autowired private RefreshTokenRepo repo;

    @Autowired private UserRepo userRepo;

    private UUID familyId;
    private Instant now;

    @BeforeEach
    void setup() {
        familyId = UUID.randomUUID();
        now = Instant.now();
    }

    private RefreshToken newToken(User user) {
        User unsavedUser;
        if (user == null) {
            unsavedUser = UserProvider.singleTemplate();
        } else {
            unsavedUser = user;
        }
        Instant now = Instant.now();
        String userId = UUID.randomUUID().toString();
        unsavedUser.setCreatedAt(now);
        unsavedUser.setCreatedBy(userId);
        User newUser = userRepo.save(unsavedUser);
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(UUID.randomUUID().toString());
        token.setTokenJti(UUID.randomUUID());
        token.setFamilyId(familyId);
        token.setExpiresAt(now.plusSeconds(3600));
        token.setLastUsedAt(now);
        token.setClientId("web");
        token.setIpAddress("127.0.0.1");
        token.setUserAgent("Firefox");
        token.setRevoked(false);
        token.setCreatedAt(now);
        token.setCreatedBy(userId);
        token.setUser(newUser);
        return token;
    }

    @Test
    @DisplayName("save persists refresh token")
    @Transactional
    void save() {
        RefreshToken saved = dao.save(newToken(null));

        assertNotNull(saved.getId());

        RefreshToken db = repo.findById(saved.getId()).orElseThrow();

        assertEquals(saved.getTokenHash(), db.getTokenHash());
    }

    @Test
    void findById() {

        RefreshToken saved = dao.save(newToken(null));

        Optional<RefreshToken> result = dao.findById(saved.getId());

        assertTrue(result.isPresent());
    }

    @Test
    void existsByUniqueProperties() {

        RefreshToken saved = dao.save(newToken(null));

        boolean exists = dao.existsByUniqueProperties(saved);

        assertTrue(exists);
    }

    @Test
    void findByTokenHashSecure() {

        RefreshToken saved = dao.save(newToken(null));

        Optional<RefreshToken> result = dao.findByTokenHashSecure(saved.getTokenHash());

        assertTrue(result.isPresent());
        assertEquals(saved.getId(), result.get().getId());
    }

    @Test
    void findByTokenHashSecureNotFound() {

        Optional<RefreshToken> result = dao.findByTokenHashSecure("unknown");

        assertTrue(result.isEmpty());
    }

    @Test
    void findFamilyIdByTokenHash() {

        RefreshToken saved = dao.save(newToken(null));

        Optional<UUID> result = dao.findFamilyIdByTokenHash(saved.getTokenHash());

        assertTrue(result.isPresent());
        assertEquals(saved.getFamilyId(), result.get());
    }

    @Test
    void revokeFamilyWithReason() {
        RefreshToken token1 = dao.save(newToken(null));
        User secondUser = UserProvider.alternativeTemplate();
        secondUser.setCreatedAt(now);
        secondUser.setCreatedBy(UUID.randomUUID().toString());
        User secUser = userRepo.save(secondUser);
        RefreshToken token2 = dao.save(newToken(secUser));

        dao.revokeFamilyWithReason(familyId, now, "reuse");

        RefreshToken db1 = repo.findById(token1.getId()).orElseThrow();
        RefreshToken db2 = repo.findById(token2.getId()).orElseThrow();

        assertTrue(db1.isRevoked());
        assertTrue(db2.isRevoked());
        assertEquals("reuse", db1.getRevokeReason());
    }

    @Test
    void updateByIdUpdatesFields() {

        RefreshToken existing = dao.save(newToken(null));

        RefreshToken update = new RefreshToken();
        update.setClientId("mobile");
        update.setIpAddress("10.0.0.1");
        update.setUserAgent("Chrome");
        update.setRevoked(true);
        update.setRevokeReason("logout");

        RefreshToken updated = dao.updateById(update, existing.getId());

        assertAll(
                () -> assertEquals("mobile", updated.getClientId()),
                () -> assertEquals("10.0.0.1", updated.getIpAddress()),
                () -> assertEquals("Chrome", updated.getUserAgent()),
                () -> assertTrue(updated.isRevoked()),
                () -> assertEquals("logout", updated.getRevokeReason()));
    }

    @Test
    void updateByIdReplacedBy() {

        RefreshToken oldToken = dao.save(newToken(null));
        RefreshToken newToken = dao.save(newToken(null));

        RefreshToken update = new RefreshToken();
        update.setReplacedBy(newToken);

        RefreshToken result = dao.updateById(update, oldToken.getId());

        assertEquals(newToken.getId(), result.getReplacedBy().getId());
    }

    @Test
    void updateNotFound() {

        assertThrows(
                NotFoundException.class,
                () -> dao.updateById(new RefreshToken(), UUID.randomUUID()));
    }
}
