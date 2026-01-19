package com.alpaca.integration.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.alpaca.entity.RefreshToken;
import com.alpaca.entity.User;
import com.alpaca.persistence.IRefreshTokenDAO;
import com.alpaca.resources.UserProvider;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RefreshTokenDAOImplIT {

    @Autowired private IRefreshTokenDAO refreshTokenDAO;

    private User testUser;
    private UUID familyId;
    private Instant now;

    @BeforeEach
    void setUp() {
        familyId = UUID.randomUUID();
        now = Instant.now();
        testUser = UserProvider.singleEntity();
    }

    @Test
    @DisplayName("Should save refresh token successfully")
    void shouldSaveRefreshTokenSuccessfully() {
        RefreshToken refreshToken =
                new RefreshToken(
                        UUID.randomUUID(),
                        testUser,
                        "token-hash",
                        UUID.randomUUID(),
                        familyId,
                        null,
                        false,
                        null,
                        now.plusSeconds(3600),
                        now,
                        "web-client",
                        "192.168.1.1",
                        "Mozilla/5.0",
                        null);

        RefreshToken savedToken = refreshTokenDAO.save(refreshToken);

        assertNotNull(savedToken);
        assertEquals(testUser, savedToken.getUser());
        assertEquals("token-hash", savedToken.getTokenHash());
        assertEquals(familyId, savedToken.getFamilyId());
        assertEquals("web-client", savedToken.getClientId());
        assertEquals("192.168.1.1", savedToken.getIpAddress());
        assertEquals("Mozilla/5.0", savedToken.getUserAgent());
        assertFalse(savedToken.getRevoked());
    }

    @Test
    @DisplayName("Should find refresh token by hash successfully")
    void shouldFindRefreshTokenByHashSuccessfully() {
        RefreshToken refreshToken =
                new RefreshToken(
                        UUID.randomUUID(),
                        testUser,
                        "token-hash",
                        UUID.randomUUID(),
                        familyId,
                        null,
                        false,
                        null,
                        now.plusSeconds(3600),
                        now,
                        "web-client",
                        "192.168.1.1",
                        "Mozilla/5.0",
                        null);

        when(refreshTokenDAO.save(refreshToken)).thenReturn(refreshToken);

        Optional<RefreshToken> result = refreshTokenDAO.findByTokenHashSecure("token-hash");

        assertTrue(result.isPresent());
        assertEquals(refreshToken, result.get());
    }

    @Test
    @DisplayName("Should return empty when refresh token not found")
    void shouldReturnEmptyWhenRefreshTokenNotFound() {
        String tokenHash = "unknown-token-hash";

        when(refreshTokenDAO.findByTokenHashSecure(tokenHash)).thenReturn(Optional.empty());

        Optional<RefreshToken> result = refreshTokenDAO.findByTokenHashSecure(tokenHash);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should find family ID by token hash successfully")
    void shouldFindFamilyIdByTokenHashSuccessfully() {
        String tokenHash = "token-hash";
        UUID expectedFamilyId = UUID.randomUUID();

        when(refreshTokenDAO.findFamilyIdByTokenHash(tokenHash))
                .thenReturn(Optional.of(expectedFamilyId));

        Optional<UUID> result = refreshTokenDAO.findFamilyIdByTokenHash(tokenHash);

        assertTrue(result.isPresent());
        assertEquals(expectedFamilyId, result.get());
    }

    @Test
    @DisplayName("Should revoke family successfully")
    void shouldRevokeFamilySuccessfully() {
        Instant revokedAt = now.plusSeconds(3600);
        String reason = "Test revoke";

        refreshTokenDAO.revokeFamilyWithReason(familyId, revokedAt, reason);
    }

    @Test
    @DisplayName("Should replace refresh token successfully")
    void shouldReplaceRefreshTokenSuccessfully() {
        RefreshToken oldToken =
                new RefreshToken(
                        UUID.randomUUID(),
                        testUser,
                        "old-token-hash",
                        UUID.randomUUID(),
                        familyId,
                        null,
                        false,
                        null,
                        now.minusSeconds(3600),
                        now.minusSeconds(3600),
                        "web-client",
                        "192.168.1.1",
                        "Mozilla/5.0",
                        null);

        RefreshToken newToken =
                new RefreshToken(
                        UUID.randomUUID(),
                        testUser,
                        "new-token-hash",
                        UUID.randomUUID(),
                        familyId,
                        null,
                        false,
                        null,
                        now.plusSeconds(3600),
                        now.plusSeconds(3600),
                        "web-client",
                        "192.168.1.1",
                        "Mozilla/5.0",
                        null);

        when(refreshTokenDAO.save(oldToken)).thenReturn(oldToken);
        when(refreshTokenDAO.save(newToken)).thenReturn(newToken);

        oldToken.setReplacedBy(newToken);
        RefreshToken updatedOldToken = refreshTokenDAO.save(oldToken);

        assertNotNull(updatedOldToken);
        assertEquals(newToken, updatedOldToken.getReplacedBy());
    }
}
