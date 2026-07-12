package com.alpaca.unit.config;

import static org.mockito.Mockito.*;

import com.alpaca.config.CleanupScheduler;
import com.alpaca.repository.RefreshTokenRepo;
import com.alpaca.repository.SessionRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link CleanupScheduler}. */
@ExtendWith(MockitoExtension.class)
class CleanupSchedulerTest {

    private RefreshTokenRepo refreshTokenRepo;
    private SessionRepo sessionRepo;
    private CleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        refreshTokenRepo = mock(RefreshTokenRepo.class);
        sessionRepo = mock(SessionRepo.class);
        scheduler = new CleanupScheduler(refreshTokenRepo, sessionRepo);
    }

    @Test
    @DisplayName("purgeRevokedRefreshTokens: should delegate to RefreshTokenRepo")
    void purgeRevokedRefreshTokens_ShouldDelegateToRefreshTokenRepo() {
        when(refreshTokenRepo.deleteRevoked()).thenReturn(5);

        scheduler.purgeRevokedRefreshTokens();

        verify(refreshTokenRepo).deleteRevoked();
        verifyNoInteractions(sessionRepo);
    }

    @Test
    @DisplayName("purgeRevokedRefreshTokens: should work when no tokens are revoked")
    void purgeRevokedRefreshTokens_ShouldWork_WhenNoTokens() {
        when(refreshTokenRepo.deleteRevoked()).thenReturn(0);

        scheduler.purgeRevokedRefreshTokens();

        verify(refreshTokenRepo).deleteRevoked();
    }

    @Test
    @DisplayName("purgeRevokedSessions: should delegate to SessionRepo")
    void purgeRevokedSessions_ShouldDelegateToSessionRepo() {
        when(sessionRepo.deleteRevoked()).thenReturn(3);

        scheduler.purgeRevokedSessions();

        verify(sessionRepo).deleteRevoked();
        verifyNoInteractions(refreshTokenRepo);
    }

    @Test
    @DisplayName("purgeRevokedSessions: should work when no sessions are revoked")
    void purgeRevokedSessions_ShouldWork_WhenNoSessions() {
        when(sessionRepo.deleteRevoked()).thenReturn(0);

        scheduler.purgeRevokedSessions();

        verify(sessionRepo).deleteRevoked();
    }

    @Test
    @DisplayName("both methods: should not interfere with each other")
    void bothMethods_ShouldNotInterfere() {
        when(refreshTokenRepo.deleteRevoked()).thenReturn(2);
        when(sessionRepo.deleteRevoked()).thenReturn(1);

        scheduler.purgeRevokedRefreshTokens();
        scheduler.purgeRevokedSessions();

        verify(refreshTokenRepo).deleteRevoked();
        verify(sessionRepo).deleteRevoked();
    }
}
