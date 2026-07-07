package com.alpaca.config;

import com.alpaca.repository.RefreshTokenRepo;
import com.alpaca.repository.SessionRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled cleanup tasks for expired or revoked data.
 *
 * <p>Runs purge jobs at configured intervals to prevent the refresh_tokens and sessions tables from
 * accumulating stale revoked rows.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupScheduler {

    private final RefreshTokenRepo refreshTokenRepo;
    private final SessionRepo sessionRepo;

    /** Purges all revoked refresh tokens every day at 00:00 UTC. */
    @Transactional
    @Scheduled(cron = "0 0 0 * * ?", zone = "UTC")
    public void purgeRevokedRefreshTokens() {
        int deleted = refreshTokenRepo.deleteRevoked();
        if (deleted > 0) {
            log.info("Purged {} revoked refresh tokens", deleted);
        }
    }

    /** Purges all revoked sessions every Sunday at 00:00 UTC. */
    @Transactional
    @Scheduled(cron = "0 0 0 * * 0", zone = "UTC")
    public void purgeRevokedSessions() {
        int deleted = sessionRepo.deleteRevoked();
        if (deleted > 0) {
            log.info("Purged {} revoked sessions", deleted);
        }
    }
}
