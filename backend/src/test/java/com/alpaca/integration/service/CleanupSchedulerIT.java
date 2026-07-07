package com.alpaca.integration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.alpaca.config.CleanupScheduler;
import com.alpaca.entity.RefreshToken;
import com.alpaca.entity.Session;
import com.alpaca.entity.User;
import com.alpaca.repository.RefreshTokenRepo;
import com.alpaca.repository.SessionRepo;
import com.alpaca.repository.UserRepo;
import com.alpaca.resources.provider.RefreshTokenProvider;
import com.alpaca.resources.provider.SessionProvider;
import com.alpaca.resources.provider.UserProvider;
import com.alpaca.resources.utility.BaseIntegrationTests;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Integration tests for {@link CleanupScheduler}. */
@DisplayName("CleanupScheduler Integration Tests")
class CleanupSchedulerIT extends BaseIntegrationTests {

    @Autowired private CleanupScheduler cleanupScheduler;
    @Autowired private RefreshTokenRepo refreshTokenRepo;
    @Autowired private SessionRepo sessionRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private EntityManager entityManager;

    private User savedUser;

    @BeforeEach
    void setup() {
        User user = UserProvider.singleTemplate();
        savedUser = userRepo.save(user);
    }

    @Test
    @DisplayName("purgeRevokedRefreshTokens: should delete only revoked refresh tokens")
    void purgeRevokedRefreshTokens_ShouldDeleteOnlyRevokedTokens() {
        RefreshToken revoked = buildRevokedRefreshToken();
        RefreshToken active = buildActiveRefreshToken();
        flush();

        cleanupScheduler.purgeRevokedRefreshTokens();

        assertThat(refreshTokenRepo.count()).isEqualTo(1);
        assertThat(refreshTokenRepo.findById(active.getId())).isPresent();
        assertThat(refreshTokenRepo.findById(revoked.getId())).isEmpty();
    }

    @Test
    @DisplayName("purgeRevokedRefreshTokens: should do nothing when no revoked tokens exist")
    void purgeRevokedRefreshTokens_ShouldDoNothing_WhenNoRevokedTokens() {
        buildActiveRefreshToken();
        flush();

        long before = refreshTokenRepo.count();
        cleanupScheduler.purgeRevokedRefreshTokens();

        assertThat(refreshTokenRepo.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("purgeRevokedRefreshTokens: should delete all revoked tokens regardless of age")
    void purgeRevokedRefreshTokens_ShouldDeleteAllRevokedTokens_RegardlessOfAge() {
        RefreshToken recentlyRevoked = buildRevokedRefreshToken(Instant.now().minusSeconds(30));
        RefreshToken oldRevoked =
                buildRevokedRefreshToken(Instant.now().minusSeconds(3600 * 24 * 30));
        flush();

        cleanupScheduler.purgeRevokedRefreshTokens();

        assertThat(refreshTokenRepo.findById(recentlyRevoked.getId())).isEmpty();
        assertThat(refreshTokenRepo.findById(oldRevoked.getId())).isEmpty();
    }

    @Test
    @DisplayName("purgeRevokedSessions: should delete only revoked sessions")
    void purgeRevokedSessions_ShouldDeleteOnlyRevokedSessions() {
        Session revoked = buildRevokedSession();
        Session active = buildActiveSession();
        flush();

        cleanupScheduler.purgeRevokedSessions();

        assertThat(sessionRepo.count()).isEqualTo(1);
        assertThat(sessionRepo.findById(active.getId())).isPresent();
        assertThat(sessionRepo.findById(revoked.getId())).isEmpty();
    }

    @Test
    @DisplayName("purgeRevokedSessions: should do nothing when no revoked sessions exist")
    void purgeRevokedSessions_ShouldDoNothing_WhenNoRevokedSessions() {
        buildActiveSession();
        flush();

        long before = sessionRepo.count();
        cleanupScheduler.purgeRevokedSessions();

        assertThat(sessionRepo.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("both purges: should not affect the other table")
    void bothPurges_ShouldNotAffectOtherTable() {
        buildRevokedRefreshToken();
        buildRevokedSession();
        flush();

        cleanupScheduler.purgeRevokedRefreshTokens();

        assertThat(refreshTokenRepo.count()).isZero();
        assertThat(sessionRepo.count()).isEqualTo(1);

        buildRevokedRefreshToken();
        flush();

        cleanupScheduler.purgeRevokedSessions();

        assertThat(sessionRepo.count()).isZero();
        assertThat(refreshTokenRepo.count()).isEqualTo(1);
    }

    // -- helpers --

    private void flush() {
        entityManager.flush();
        entityManager.clear();
    }

    private RefreshToken buildRevokedRefreshToken() {
        return buildRevokedRefreshToken(Instant.now().minusSeconds(3600));
    }

    private RefreshToken buildRevokedRefreshToken(Instant revokedAt) {
        RefreshToken token = RefreshTokenProvider.singleTemplate();
        token.setTokenHash(UUID.randomUUID().toString());
        token.setUser(savedUser);
        token.setRevoked(true);
        token.setRevokedAt(revokedAt);
        return refreshTokenRepo.save(token);
    }

    private RefreshToken buildActiveRefreshToken() {
        RefreshToken token = RefreshTokenProvider.alternativeTemplate();
        token.setTokenHash(UUID.randomUUID().toString());
        token.setUser(savedUser);
        return refreshTokenRepo.save(token);
    }

    private Session buildRevokedSession() {
        Session session = SessionProvider.singleTemplate();
        session.setUser(savedUser);
        session.setRevoked(true);
        session.setRevokedAt(Instant.now().minusSeconds(3600));
        return sessionRepo.save(session);
    }

    private Session buildActiveSession() {
        Session session = SessionProvider.alternativeTemplate();
        session.setUser(savedUser);
        return sessionRepo.save(session);
    }
}
