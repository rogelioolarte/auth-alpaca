package com.alpaca.integration.service;

import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.entity.RefreshToken;
import com.alpaca.entity.Session;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.AuthCode;
import com.alpaca.model.UserPrincipal;
import com.alpaca.persistence.IRefreshTokenDAO;
import com.alpaca.resources.provider.RefreshTokenProvider;
import com.alpaca.resources.provider.SessionProvider;
import com.alpaca.resources.provider.UserProvider;
import com.alpaca.resources.utility.BaseIntegrationTests;
import com.alpaca.security.manager.JJwtManager;
import com.alpaca.service.ISessionService;
import com.alpaca.service.IUserService;
import com.alpaca.service.impl.RefreshTokenServiceImpl;
import com.alpaca.utils.UUIDv7Generator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Integration tests for {@link RefreshTokenServiceImpl} */
@DisplayName("RefreshTokenServiceImpl Integration Tests")
class RefreshTokenServiceImplIT extends BaseIntegrationTests {

    @Autowired private RefreshTokenServiceImpl service;

    @Autowired private IRefreshTokenDAO refreshTokenDAO;

    @Autowired private ISessionService sessionService;

    @Autowired private IUserService userService;

    @Autowired private JJwtManager jwtManager;

    @Autowired private UUIDv7Generator uuidv7Generator;

    private Instant now;

    @BeforeEach
    void setup() {
        now = Instant.now();
    }

    @Test
    void rotateRefreshToken_ShouldThrowBadRequest_WhenOldRefreshTokenIsBlank() {
        assertThrows(
                BadRequestException.class,
                () -> service.rotateRefreshToken(" ", "client", "agent", "127.0.0.1"));
    }

    @Test
    void rotateRefreshToken_ShouldThrowBadRequest_WhenClientIdIsBlank() {
        assertThrows(
                BadRequestException.class,
                () -> service.rotateRefreshToken("token", " ", "agent", "127.0.0.1"));
    }

    @Test
    void rotateRefreshToken_ShouldThrowBadRequest_WhenUserAgentIsBlank() {
        assertThrows(
                BadRequestException.class,
                () -> service.rotateRefreshToken("token", "client", " ", "127.0.0.1"));
    }

    @Test
    void rotateRefreshToken_ShouldThrowBadRequest_WhenClientIpIsBlank() {
        assertThrows(
                BadRequestException.class,
                () -> service.rotateRefreshToken("token", "client", "agent", " "));
    }

    @Test
    void rotateRefreshToken_ShouldThrowUnauthorized_WhenTokenDoesNotExist() {
        assertThrows(
                UnauthorizedException.class,
                () ->
                        service.rotateRefreshToken(
                                "non-existent-token", "client", "agent", "127.0.0.1"));
    }

    @Test
    @Transactional
    void rotateRefreshToken_ShouldThrowUnauthorized_WhenSessionIsRevoked() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);
        User persistedUser = userService.save(user);

        UUID familyId = uuidv7Generator.generate();

        Session session = SessionProvider.singleTemplate();
        session.setCreatedAt(now);
        session.setUser(persistedUser);
        session.setFamilyId(familyId);
        session.setLastSeenAt(now);
        session.setClientId("client");
        session.setUserAgent("agent");
        session.setIpAddress("127.0.0.1");
        session.setRevoked(true);
        session.setRevokedAt(now.minusSeconds(30));

        Session persistedSession = sessionService.save(session);

        RefreshToken refreshToken = RefreshTokenProvider.singleTemplate();
        refreshToken.setCreatedAt(now);
        refreshToken.setUser(persistedUser);
        refreshToken.setFamilyId(persistedSession.getFamilyId());
        refreshToken.setClientId(persistedSession.getClientId());
        refreshToken.setUserAgent(persistedSession.getUserAgent());
        refreshToken.setIpAddress(persistedSession.getIpAddress());
        refreshToken.setExpiresAt(now.plusSeconds(3600));

        String jwt = jwtManager.createRefreshToken(refreshToken);
        refreshToken.setTokenHash(jwtManager.createTokenHash(jwt));

        RefreshToken persistedToken = service.save(refreshToken);

        UnauthorizedException exception =
                assertThrowsExactly(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        jwt,
                                        persistedToken.getClientId(),
                                        persistedToken.getUserAgent(),
                                        persistedToken.getIpAddress()));

        assertEquals("Revoked Session", exception.getReason());
    }

    @Test
    @Transactional
    void rotateRefreshToken_ShouldThrowUnauthorized_WhenSessionRevokedAtIsBeforeNow() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);
        User persistedUser = userService.save(user);

        UUID familyId = uuidv7Generator.generate();

        Session session = SessionProvider.singleTemplate();
        session.setCreatedAt(now);
        session.setUser(persistedUser);
        session.setFamilyId(familyId);
        session.setLastSeenAt(now);
        session.setClientId("client");
        session.setUserAgent("agent");
        session.setIpAddress("127.0.0.1");
        session.setRevoked(false);
        session.setRevokedAt(now.minusSeconds(1));

        Session persistedSession = sessionService.save(session);

        RefreshToken refreshToken = RefreshTokenProvider.singleTemplate();
        refreshToken.setCreatedAt(now);
        refreshToken.setUser(persistedUser);
        refreshToken.setFamilyId(persistedSession.getFamilyId());
        refreshToken.setClientId(persistedSession.getClientId());
        refreshToken.setUserAgent(persistedSession.getUserAgent());
        refreshToken.setIpAddress(persistedSession.getIpAddress());
        refreshToken.setExpiresAt(now.plusSeconds(3600));

        String jwt = jwtManager.createRefreshToken(refreshToken);
        refreshToken.setTokenHash(jwtManager.createTokenHash(jwt));

        RefreshToken persistedToken = service.save(refreshToken);

        UnauthorizedException exception =
                assertThrowsExactly(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        jwt,
                                        persistedToken.getClientId(),
                                        persistedToken.getUserAgent(),
                                        persistedToken.getIpAddress()));

        assertEquals("Revoked Session", exception.getReason());
    }

    @Test
    @Transactional
    void rotateRefreshToken_ShouldThrowUnauthorized_WhenRefreshTokenAlreadyRevoked() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);
        User persistedUser = userService.save(user);

        RefreshToken refreshToken = RefreshTokenProvider.singleTemplate();
        refreshToken.setCreatedAt(now);
        refreshToken.setUser(persistedUser);
        refreshToken.setFamilyId(uuidv7Generator.generate());
        refreshToken.setRevoked(true);
        refreshToken.setRevokeReason("reuse-detected");
        refreshToken.setClientId("client");
        refreshToken.setUserAgent("agent");
        refreshToken.setIpAddress("127.0.0.1");
        refreshToken.setExpiresAt(now.plusSeconds(3600));

        String jwt = jwtManager.createRefreshToken(refreshToken);
        refreshToken.setTokenHash(jwtManager.createTokenHash(jwt));

        RefreshToken persistedToken = service.save(refreshToken);

        UnauthorizedException exception =
                assertThrowsExactly(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        jwt,
                                        persistedToken.getClientId(),
                                        persistedToken.getUserAgent(),
                                        persistedToken.getIpAddress()));

        assertEquals("Refresh Token already revoked", exception.getReason());

        Optional<RefreshToken> revokedToken = refreshTokenDAO.findById(persistedToken.getId());

        assertTrue(revokedToken.isPresent());
        assertEquals("reuse-detected", revokedToken.get().getRevokeReason());
    }

    @Test
    @Transactional
    void rotateRefreshToken_ShouldThrowUnauthorized_WhenRefreshTokenWasReplaced() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);
        User persistedUser = userService.save(user);

        UUID familyId = uuidv7Generator.generate();

        RefreshToken replacement = RefreshTokenProvider.singleTemplate();
        replacement.setCreatedAt(now);
        replacement.setUser(persistedUser);
        replacement.setFamilyId(familyId);
        replacement.setClientId("client");
        replacement.setUserAgent("agent");
        replacement.setIpAddress("127.0.0.1");
        replacement.setExpiresAt(now.plusSeconds(3600));

        RefreshToken persistedReplacement = service.save(replacement);

        RefreshToken refreshToken = RefreshTokenProvider.singleTemplate();
        refreshToken.setCreatedAt(now);
        refreshToken.setUser(persistedUser);
        refreshToken.setFamilyId(familyId);
        refreshToken.setReplacedBy(persistedReplacement);
        refreshToken.setClientId("client");
        refreshToken.setUserAgent("agent");
        refreshToken.setIpAddress("127.0.0.1");
        refreshToken.setExpiresAt(now.plusSeconds(3600));

        String jwt = jwtManager.createRefreshToken(refreshToken);
        refreshToken.setTokenHash(jwtManager.createTokenHash(jwt));

        RefreshToken persistedToken = service.save(refreshToken);

        UnauthorizedException exception =
                assertThrowsExactly(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        jwt,
                                        persistedToken.getClientId(),
                                        persistedToken.getUserAgent(),
                                        persistedToken.getIpAddress()));

        assertEquals("Reuse Detected Refresh Token", exception.getReason());
    }

    @Test
    @Transactional
    void rotateRefreshToken_ShouldThrowUnauthorized_WhenRefreshTokenExpired() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);
        User persistedUser = userService.save(user);

        RefreshToken refreshToken = RefreshTokenProvider.singleTemplate();
        refreshToken.setCreatedAt(now.minusSeconds(3600));
        refreshToken.setUser(persistedUser);
        refreshToken.setFamilyId(uuidv7Generator.generate());
        refreshToken.setClientId("client");
        refreshToken.setUserAgent("agent");
        refreshToken.setIpAddress("127.0.0.1");
        refreshToken.setExpiresAt(now.minusSeconds(1));

        String jwt = jwtManager.createRefreshToken(refreshToken);
        refreshToken.setTokenHash(jwtManager.createTokenHash(jwt));

        RefreshToken persistedToken = service.save(refreshToken);

        UnauthorizedException exception =
                assertThrowsExactly(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        jwt,
                                        persistedToken.getClientId(),
                                        persistedToken.getUserAgent(),
                                        persistedToken.getIpAddress()));

        assertEquals("Reuse Detected Refresh Token", exception.getReason());
    }

    @Test
    @Transactional
    void rotateRefreshToken_ShouldThrowUnauthorized_WhenTokenIssuedBeforeTokensInvalidBefore() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);
        user.setTokensInvalidBefore(now.plusSeconds(3600));

        User persistedUser = userService.save(user);

        RefreshToken refreshToken = RefreshTokenProvider.singleTemplate();
        refreshToken.setCreatedAt(now);
        refreshToken.setUser(persistedUser);
        refreshToken.setFamilyId(uuidv7Generator.generate());
        refreshToken.setClientId("client");
        refreshToken.setUserAgent("agent");
        refreshToken.setIpAddress("127.0.0.1");
        refreshToken.setExpiresAt(now.plusSeconds(3600));

        String jwt = jwtManager.createRefreshToken(refreshToken);
        refreshToken.setTokenHash(jwtManager.createTokenHash(jwt));

        RefreshToken persistedToken = service.save(refreshToken);

        UnauthorizedException exception =
                assertThrowsExactly(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        jwt,
                                        persistedToken.getClientId(),
                                        persistedToken.getUserAgent(),
                                        persistedToken.getIpAddress()));

        assertEquals("Refresh Token already revoked", exception.getReason());
    }

    @Test
    @Transactional
    void rotateRefreshToken_ShouldThrowUnauthorized_WhenClientIdMismatch() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);

        User persistedUser = userService.save(user);

        RefreshToken refreshToken = RefreshTokenProvider.singleTemplate();
        refreshToken.setCreatedAt(now);
        refreshToken.setUser(persistedUser);
        refreshToken.setFamilyId(uuidv7Generator.generate());
        refreshToken.setClientId("client-a");
        refreshToken.setUserAgent("agent");
        refreshToken.setIpAddress("127.0.0.1");
        refreshToken.setExpiresAt(now.plusSeconds(3600));

        String jwt = jwtManager.createRefreshToken(refreshToken);
        refreshToken.setTokenHash(jwtManager.createTokenHash(jwt));

        RefreshToken persistedToken = service.save(refreshToken);

        UnauthorizedException exception =
                assertThrowsExactly(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        jwt,
                                        "client-b",
                                        persistedToken.getUserAgent(),
                                        persistedToken.getIpAddress()));

        assertEquals("Client mismatch", exception.getReason());
    }

    @Test
    @Transactional
    void rotateRefreshToken_ShouldThrowUnauthorized_WhenUserAgentMismatch() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);

        User persistedUser = userService.save(user);

        RefreshToken refreshToken = RefreshTokenProvider.singleTemplate();
        refreshToken.setCreatedAt(now);
        refreshToken.setUser(persistedUser);
        refreshToken.setFamilyId(uuidv7Generator.generate());
        refreshToken.setClientId("client");
        refreshToken.setUserAgent("agent-a");
        refreshToken.setIpAddress("127.0.0.1");
        refreshToken.setExpiresAt(now.plusSeconds(3600));

        String jwt = jwtManager.createRefreshToken(refreshToken);
        refreshToken.setTokenHash(jwtManager.createTokenHash(jwt));

        RefreshToken persistedToken = service.save(refreshToken);

        UnauthorizedException exception =
                assertThrowsExactly(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        jwt,
                                        persistedToken.getClientId(),
                                        "agent-b",
                                        persistedToken.getIpAddress()));

        assertEquals("User-Agent mismatch", exception.getReason());
    }

    @Test
    @Transactional
    void rotateRefreshToken_ShouldRotateSuccessfully_WhenRefreshTokenIsValid() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);

        User persistedUser = userService.save(user);

        UUID familyId = uuidv7Generator.generate();

        Session session = SessionProvider.singleTemplate();
        session.setCreatedAt(now);
        session.setUser(persistedUser);
        session.setFamilyId(familyId);
        session.setLastSeenAt(now);
        session.setClientId("client");
        session.setUserAgent("agent");
        session.setIpAddress("127.0.0.1");
        session.setRevoked(false);

        Session persistedSession = sessionService.save(session);

        RefreshToken refreshToken = RefreshTokenProvider.singleTemplate();
        refreshToken.setCreatedAt(now.minusSeconds(5));
        refreshToken.setUser(persistedUser);
        refreshToken.setFamilyId(persistedSession.getFamilyId());
        refreshToken.setClientId(persistedSession.getClientId());
        refreshToken.setUserAgent(persistedSession.getUserAgent());
        refreshToken.setIpAddress(persistedSession.getIpAddress());
        refreshToken.setExpiresAt(now.plusSeconds(3600));
        refreshToken.setRevoked(false);

        String oldJwt = jwtManager.createRefreshToken(refreshToken);
        refreshToken.setTokenHash(jwtManager.createTokenHash(oldJwt));

        RefreshToken persistedToken = service.save(refreshToken);

        AuthResponseDTO response =
                service.rotateRefreshToken(
                        oldJwt,
                        persistedToken.getClientId(),
                        persistedToken.getUserAgent(),
                        persistedToken.getIpAddress());

        assertNotNull(response);
        assertTrue(StringUtils.hasText(response.accessToken()));
        assertTrue(StringUtils.hasText(response.refreshToken()));

        RefreshToken updatedToken =
                refreshTokenDAO.findByTokenHashSecure(persistedToken.getTokenHash()).orElseThrow();

        assertTrue(updatedToken.isRevoked());
        assertNotNull(updatedToken.getRevokedAt());
        assertEquals("rotation", updatedToken.getRevokeReason());
        assertNotNull(updatedToken.getReplacedBy());
        assertNotNull(updatedToken.getLastUsedAt());

        String newHash = jwtManager.createTokenHash(response.refreshToken());

        RefreshToken newToken = refreshTokenDAO.findByTokenHashSecure(newHash).orElseThrow();

        assertEquals(updatedToken.getFamilyId(), newToken.getFamilyId());
        assertEquals(persistedUser.getId(), newToken.getUser().getId());
    }

    @Test
    @Transactional
    void generateJWTTokens_ShouldPersistRefreshToken_WhenUsingUserPrincipalAndSession() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);

        User persistedUser = userService.save(user);

        Session session = SessionProvider.singleTemplate();
        session.setCreatedAt(now);
        session.setUser(persistedUser);
        session.setFamilyId(uuidv7Generator.generate());
        session.setLastSeenAt(now);

        Session persistedSession = sessionService.save(session);

        UserPrincipal principal = new UserPrincipal(persistedUser);

        AuthResponseDTO response = service.generateJWTTokens(principal, persistedSession);

        assertNotNull(response);
        assertTrue(StringUtils.hasText(response.accessToken()));
        assertTrue(StringUtils.hasText(response.refreshToken()));

        String hash = jwtManager.createTokenHash(response.refreshToken());

        Optional<RefreshToken> persistedToken = refreshTokenDAO.findByTokenHashSecure(hash);

        assertTrue(persistedToken.isPresent());
        assertEquals(persistedUser.getId(), persistedToken.get().getUser().getId());
    }

    @Test
    @Transactional
    void generateJWTTokens_ShouldCreateSessionAndPersistRefreshToken_WhenUsingAuthCode() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);

        User persistedUser = userService.save(user);

        AuthCode authCode =
                new AuthCode(
                        "code",
                        "challenge",
                        "client-id",
                        "agent",
                        "127.0.0.1",
                        persistedUser.getId(),
                        "http://localhost:4200");

        AuthResponseDTO response = service.generateJWTTokens(authCode);

        assertNotNull(response);
        assertTrue(StringUtils.hasText(response.accessToken()));
        assertTrue(StringUtils.hasText(response.refreshToken()));

        String hash = jwtManager.createTokenHash(response.refreshToken());

        Optional<RefreshToken> persistedToken = refreshTokenDAO.findByTokenHashSecure(hash);

        assertTrue(persistedToken.isPresent());
        assertEquals(persistedUser.getId(), persistedToken.get().getUser().getId());
    }

    @Test
    @Transactional
    void delegationMethods_ShouldWorkCorrectly() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);

        User persistedUser = userService.save(user);

        RefreshToken refreshToken = RefreshTokenProvider.singleTemplate();
        refreshToken.setCreatedAt(now);
        refreshToken.setUser(persistedUser);
        refreshToken.setFamilyId(uuidv7Generator.generate());
        refreshToken.setClientId("client");
        refreshToken.setUserAgent("agent");
        refreshToken.setIpAddress("127.0.0.1");
        refreshToken.setExpiresAt(now.plusSeconds(3600));

        String jwt = jwtManager.createRefreshToken(refreshToken);
        refreshToken.setTokenHash(jwtManager.createTokenHash(jwt));

        RefreshToken persistedToken = service.save(refreshToken);

        Optional<UUID> familyId = service.findFamilyIdByTokenHash(persistedToken.getTokenHash());

        assertTrue(familyId.isPresent());
        assertEquals(persistedToken.getFamilyId(), familyId.get());

        Optional<RefreshToken> found = service.findByTokenHashSecure(persistedToken.getTokenHash());

        assertTrue(found.isPresent());
        assertEquals(persistedToken.getId(), found.get().getId());

        assertDoesNotThrow(
                () -> service.revokeFamilyWithReason(persistedToken.getFamilyId(), now, "reason"));

        assertDoesNotThrow(
                () ->
                        service.revokeRefreshTokensAndSessionByFamilyId(
                                persistedToken.getFamilyId(), now, "reason-2"));
    }

    @Test
    @Transactional
    void updateById_ShouldUpdateAllFieldsSuccessfully() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);

        User persistedUser = userService.save(user);

        RefreshToken originalToken = RefreshTokenProvider.singleTemplate();
        originalToken.setCreatedAt(now);
        originalToken.setUser(persistedUser);
        originalToken.setFamilyId(uuidv7Generator.generate());
        originalToken.setClientId("client-a");
        originalToken.setUserAgent("agent-a");
        originalToken.setIpAddress("127.0.0.1");
        originalToken.setTokenHash("hash-a");
        originalToken.setExpiresAt(now.plusSeconds(3600));

        RefreshToken persistedToken = service.save(originalToken);

        RefreshToken replacement = RefreshTokenProvider.alternativeTemplate();
        replacement.setCreatedAt(now);
        replacement.setUser(persistedUser);
        replacement.setFamilyId(uuidv7Generator.generate());
        replacement.setClientId("client-b");
        replacement.setUserAgent("agent-b");
        replacement.setIpAddress("10.0.0.1");
        replacement.setTokenHash("hash-b");
        replacement.setExpiresAt(now.plusSeconds(7200));

        RefreshToken persistedReplacement = service.save(replacement);

        RefreshToken update = RefreshTokenProvider.alternativeTemplate();
        update.setUser(persistedUser);
        update.setReplacedBy(persistedReplacement);
        update.setTokenJti(uuidv7Generator.generate());
        update.setFamilyId(uuidv7Generator.generate());
        update.setRevoked(true);
        update.setRevokedAt(now);
        update.setExpiresAt(now.plusSeconds(9000));
        update.setLastUsedAt(now);
        update.setTokenHash("updated-hash");
        update.setClientId("updated-client");
        update.setIpAddress("192.168.0.1");
        update.setUserAgent("updated-agent");
        update.setRevokeReason("updated-reason");

        RefreshToken updated = service.updateById(update, persistedToken.getId());

        assertEquals(update.getUser().getId(), updated.getUser().getId());
        assertEquals(update.getReplacedBy().getId(), updated.getReplacedBy().getId());
        assertEquals(update.getTokenJti(), updated.getTokenJti());
        assertEquals(update.getFamilyId(), updated.getFamilyId());
        assertTrue(updated.isRevoked());
        assertEquals(update.getRevokedAt(), updated.getRevokedAt());
        assertEquals(update.getExpiresAt(), updated.getExpiresAt());
        assertEquals(update.getLastUsedAt(), updated.getLastUsedAt());
        assertEquals(update.getTokenHash(), updated.getTokenHash());
        assertEquals(update.getClientId(), updated.getClientId());
        assertEquals(update.getIpAddress(), updated.getIpAddress());
        assertEquals(update.getUserAgent(), updated.getUserAgent());
        assertEquals(update.getRevokeReason(), updated.getRevokeReason());
    }

    @Test
    @Transactional
    void updateById_ShouldThrowBadRequest_WhenRefreshTokenIsNull() {
        BadRequestException exception =
                assertThrowsExactly(
                        BadRequestException.class,
                        () -> service.updateById(null, UUID.randomUUID()));

        assertNotNull(exception.getReason());
        assertTrue(exception.getReason().contains("cannot be updated"));
    }

    @Test
    @Transactional
    void updateById_ShouldThrowBadRequest_WhenIdIsNull() {
        RefreshToken refreshToken = RefreshTokenProvider.singleTemplate();

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class, () -> service.updateById(refreshToken, null));

        assertNotNull(exception.getReason());
        assertTrue(exception.getReason().contains("cannot be updated"));
    }

    @Test
    @Transactional
    void updateById_ShouldThrowNotFound_WhenRefreshTokenDoesNotExist() {
        RefreshToken refreshToken = RefreshTokenProvider.singleTemplate();

        assertThrowsExactly(
                NotFoundException.class, () -> service.updateById(refreshToken, UUID.randomUUID()));
    }
}
