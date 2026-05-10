package com.alpaca.integration.service;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.entity.RefreshToken;
import com.alpaca.entity.Session;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.AuthCode;
import com.alpaca.model.UserPrincipal;
import com.alpaca.persistence.IRefreshTokenDAO;
import com.alpaca.resources.RefreshTokenProvider;
import com.alpaca.resources.SessionProvider;
import com.alpaca.resources.UserProvider;
import com.alpaca.security.manager.JJwtManager;
import com.alpaca.service.ISessionService;
import com.alpaca.service.IUserService;
import com.alpaca.service.impl.RefreshTokenServiceImpl;
import com.alpaca.utils.UUIDv7Generator;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Integration tests for {@link RefreshTokenServiceImpl} */
@SpringBootTest
@Transactional
@DisplayName("RefreshTokenServiceImpl Integration Tests")
class RefreshTokenServiceImplIT {

    @Autowired private RefreshTokenServiceImpl service;

    @Autowired private IRefreshTokenDAO refreshTokenDAO;

    @Autowired private ISessionService sessionService;

    @Autowired private IUserService userService;

    @Autowired private JJwtManager jwtManager;

    @Autowired private UUIDv7Generator uuidv7Generator;

    private User userTemplate;

    @BeforeEach
    void setup() {
        userTemplate = UserProvider.singleTemplate();
    }

    // ------------------------------------------------
    // rotateRefreshToken - input validations
    // ------------------------------------------------

    @Test
    void rotateRefreshToken_whenOldRefreshTokenIsBlank_thenThrowBadRequest() {
        assertThrows(
                BadRequestException.class,
                () -> service.rotateRefreshToken(" ", "client", "agent", "127.0.0.1"));
    }

    @Test
    void rotateRefreshToken_whenClientIdIsBlank_thenThrowBadRequest() {
        assertThrows(
                BadRequestException.class,
                () -> service.rotateRefreshToken("token", " ", "agent", "127.0.0.1"));
    }

    @Test
    void rotateRefreshToken_whenUserAgentIsBlank_thenThrowBadRequest() {
        assertThrows(
                BadRequestException.class,
                () -> service.rotateRefreshToken("token", "client", " ", "127.0.0.1"));
    }

    @Test
    void rotateRefreshToken_whenClientIpIsBlank_thenThrowBadRequest() {
        assertThrows(
                BadRequestException.class,
                () -> service.rotateRefreshToken("token", "client", "agent", " "));
    }

    // ------------------------------------------------
    // rotateRefreshToken - token not found
    // ------------------------------------------------

    @Test
    void rotateRefreshToken_whenTokenDoesNotExist_thenThrowUnauthorized() {
        assertThrows(
                UnauthorizedException.class,
                () ->
                        service.rotateRefreshToken(
                                "non-existent-token", "client", "agent", "127.0.0.1"));
    }

    // ------------------------------------------------
    // rotateRefreshToken - revoked session
    // ------------------------------------------------

    @Test
    @Transactional
    void rotateRefreshToken_whenSessionIsRevoked_thenThrowUnauthorized() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(Instant.now());
        User persistedUser = userService.register(user);

        UUID familyId = uuidv7Generator.generate();

        Session session = SessionProvider.singleTemplate();
        session.setCreatedAt(Instant.now());
        session.setUser(persistedUser);
        session.setFamilyId(familyId);
        session.setLastSeenAt(Instant.now());
        session.setRevoked(true);
        session.setRevokedAt(Instant.now().minusSeconds(10));

        sessionService.save(session);

        RefreshToken refreshToken = RefreshTokenProvider.singleTemplate();
        refreshToken.setCreatedAt(Instant.now());
        refreshToken.setUser(persistedUser);
        refreshToken.setFamilyId(familyId);
        refreshToken.setClientId("client");
        refreshToken.setUserAgent("agent");
        refreshToken.setIpAddress("127.0.0.1");
        refreshToken.setExpiresAt(Instant.now().plusSeconds(3600));

        String jwt = jwtManager.createRefreshToken(refreshToken);
        refreshToken.setTokenHash(jwtManager.createTokenHash(jwt));

        RefreshToken persistedToken = service.save(refreshToken);

        UnauthorizedException ex =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        jwt,
                                        persistedToken.getClientId(),
                                        persistedToken.getUserAgent(),
                                        persistedToken.getIpAddress()));

        assertEquals("Revoked Session", ex.getReason());
    }

    // ------------------------------------------------
    // validateRefreshToken branches
    // ------------------------------------------------

    @Test
    @Transactional
    void rotateRefreshToken_whenTokenIsRevoked_thenThrowReuseDetected() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(Instant.now());
        User persistedUser = userService.register(user);

        RefreshToken token = RefreshTokenProvider.singleTemplate();
        token.setCreatedAt(Instant.now());
        token.setUser(persistedUser);
        token.setFamilyId(uuidv7Generator.generate());
        token.setRevoked(true);
        token.setClientId("client");
        token.setUserAgent("agent");
        token.setIpAddress("127.0.0.1");
        token.setExpiresAt(Instant.now().plusSeconds(3600));

        String jwt = jwtManager.createRefreshToken(token);
        token.setTokenHash(jwtManager.createTokenHash(jwt));

        RefreshToken persisted = service.save(token);

        UnauthorizedException ex =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        jwt,
                                        persisted.getClientId(),
                                        persisted.getUserAgent(),
                                        persisted.getIpAddress()));

        assertEquals("Reuse Detected Refresh Token", ex.getReason());
    }

    @Test
    @Transactional
    void rotateRefreshToken_whenTokenWasAlreadyReplaced_thenThrowReuseDetected() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(Instant.now());
        User persistedUser = userService.register(user);

        UUID familyId = uuidv7Generator.generate();

        RefreshToken replacement = RefreshTokenProvider.singleTemplate();
        replacement.setCreatedAt(Instant.now());
        replacement.setUser(persistedUser);
        replacement.setFamilyId(familyId);
        replacement.setClientId("client");
        replacement.setUserAgent("agent");
        replacement.setIpAddress("127.0.0.1");
        replacement.setExpiresAt(Instant.now().plusSeconds(3600));

        replacement = service.save(replacement);

        RefreshToken token = RefreshTokenProvider.singleTemplate();
        token.setCreatedAt(Instant.now());
        token.setUser(persistedUser);
        token.setFamilyId(familyId);
        token.setClientId("client");
        token.setUserAgent("agent");
        token.setIpAddress("127.0.0.1");
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        token.setReplacedBy(replacement);

        String jwt = jwtManager.createRefreshToken(token);
        token.setTokenHash(jwtManager.createTokenHash(jwt));

        RefreshToken persisted = service.save(token);

        UnauthorizedException ex =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        jwt,
                                        persisted.getClientId(),
                                        persisted.getUserAgent(),
                                        persisted.getIpAddress()));

        assertEquals("Reuse Detected Refresh Token", ex.getReason());
    }

    @Test
    @Transactional
    void rotateRefreshToken_whenTokenExpired_thenThrowReuseDetected() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(Instant.now());
        User persistedUser = userService.register(user);

        RefreshToken token = RefreshTokenProvider.singleTemplate();
        token.setCreatedAt(Instant.now().minusSeconds(3600));
        token.setUser(persistedUser);
        token.setFamilyId(uuidv7Generator.generate());
        token.setClientId("client");
        token.setUserAgent("agent");
        token.setIpAddress("127.0.0.1");
        token.setExpiresAt(Instant.now().minusSeconds(1));

        String jwt = jwtManager.createRefreshToken(token);
        token.setTokenHash(jwtManager.createTokenHash(jwt));

        RefreshToken persisted = service.save(token);

        UnauthorizedException ex =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        jwt,
                                        persisted.getClientId(),
                                        persisted.getUserAgent(),
                                        persisted.getIpAddress()));

        assertEquals("Reuse Detected Refresh Token", ex.getReason());
    }

    @Test
    @Transactional
    void rotateRefreshToken_whenExpiresAtIsNull_thenThrowReuseDetected() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(Instant.now());
        User persistedUser = userService.register(user);

        RefreshToken token = RefreshTokenProvider.singleTemplate();
        token.setCreatedAt(Instant.now());
        token.setUser(persistedUser);
        token.setFamilyId(uuidv7Generator.generate());
        token.setClientId("client");
        token.setUserAgent("agent");
        token.setIpAddress("127.0.0.1");
        token.setExpiresAt(Instant.now().minusSeconds(3600));

        String jwt = jwtManager.createRefreshToken(token);
        token.setTokenHash(jwtManager.createTokenHash(jwt));

        RefreshToken persisted = service.save(token);

        UnauthorizedException ex =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        jwt,
                                        persisted.getClientId(),
                                        persisted.getUserAgent(),
                                        persisted.getIpAddress()));

        assertEquals("Reuse Detected Refresh Token", ex.getReason());
    }

    @Test
    @Transactional
    void rotateRefreshToken_whenCreatedBeforeTokensInvalidBefore_thenThrowUnauthorized() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(Instant.now());
        User persistedUser = userService.register(user);

        persistedUser.setTokensInvalidBefore(Instant.now().plusSeconds(3600));

        RefreshToken token = RefreshTokenProvider.singleTemplate();
        token.setCreatedAt(Instant.now());
        token.setUser(persistedUser);
        token.setFamilyId(uuidv7Generator.generate());
        token.setClientId("client");
        token.setUserAgent("agent");
        token.setIpAddress("127.0.0.1");
        token.setExpiresAt(Instant.now().plusSeconds(3600));

        String jwt = jwtManager.createRefreshToken(token);
        token.setTokenHash(jwtManager.createTokenHash(jwt));

        RefreshToken persisted = service.save(token);

        UnauthorizedException ex =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        jwt,
                                        persisted.getClientId(),
                                        persisted.getUserAgent(),
                                        persisted.getIpAddress()));

        assertEquals("Refresh Token issued before tokens_invalid_before", ex.getReason());
    }

    @Test
    @Transactional
    void rotateRefreshToken_whenClientIdMismatch_thenThrowUnauthorized() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(Instant.now());
        User persistedUser = userService.register(user);

        RefreshToken token = RefreshTokenProvider.singleTemplate();
        token.setCreatedAt(Instant.now());
        token.setUser(persistedUser);
        token.setFamilyId(uuidv7Generator.generate());
        token.setClientId("client-a");
        token.setUserAgent("agent");
        token.setIpAddress("127.0.0.1");
        token.setExpiresAt(Instant.now().plusSeconds(3600));

        String jwt = jwtManager.createRefreshToken(token);
        token.setTokenHash(jwtManager.createTokenHash(jwt));

        RefreshToken persisted = service.save(token);

        UnauthorizedException ex =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        jwt,
                                        "client-b",
                                        persisted.getUserAgent(),
                                        persisted.getIpAddress()));

        assertEquals("Client mismatch", ex.getReason());
    }

    @Test
    @Transactional
    void rotateRefreshToken_whenUserAgentMismatch_thenThrowUnauthorized() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(Instant.now());
        User persistedUser = userService.register(user);

        RefreshToken token = RefreshTokenProvider.singleTemplate();
        token.setCreatedAt(Instant.now());
        token.setUser(persistedUser);
        token.setFamilyId(uuidv7Generator.generate());
        token.setClientId("client");
        token.setUserAgent("agent-a");
        token.setIpAddress("127.0.0.1");
        token.setExpiresAt(Instant.now().plusSeconds(3600));

        String jwt = jwtManager.createRefreshToken(token);
        token.setTokenHash(jwtManager.createTokenHash(jwt));

        RefreshToken persisted = service.save(token);

        UnauthorizedException ex =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        jwt,
                                        persisted.getClientId(),
                                        "agent-b",
                                        persisted.getIpAddress()));

        assertEquals("User-Agent mismatch", ex.getReason());
    }

    // ------------------------------------------------
    // rotateRefreshToken - success path
    // ------------------------------------------------

    @Test
    @Transactional
    void rotateRefreshToken_whenTokenIsValid_thenRotateSuccessfully() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(Instant.now());
        User persistedUser = userService.register(user);

        UUID familyId = uuidv7Generator.generate();
        Instant now = Instant.now();

        Session session = SessionProvider.singleTemplate();
        session.setCreatedAt(now);
        session.setUser(persistedUser);
        session.setFamilyId(familyId);
        session.setLastSeenAt(now);
        session.setClientId("client");
        session.setUserAgent("agent");
        session.setIpAddress("127.0.0.1");
        session.setRevoked(false);

        sessionService.save(session);

        RefreshToken oldToken = RefreshTokenProvider.singleTemplate();
        oldToken.setCreatedAt(now.minusSeconds(5));
        oldToken.setUser(persistedUser);
        oldToken.setFamilyId(familyId);
        oldToken.setClientId("client");
        oldToken.setUserAgent("agent");
        oldToken.setIpAddress("127.0.0.1");
        oldToken.setExpiresAt(now.plusSeconds(3600));
        oldToken.setRevoked(false);

        String oldJwt = jwtManager.createRefreshToken(oldToken);
        oldToken.setTokenHash(jwtManager.createTokenHash(oldJwt));

        RefreshToken persistedOldToken = service.save(oldToken);

        AuthResponseDTO response =
                service.rotateRefreshToken(
                        oldJwt,
                        persistedOldToken.getClientId(),
                        persistedOldToken.getUserAgent(),
                        persistedOldToken.getIpAddress());

        assertNotNull(response);
        assertTrue(StringUtils.hasText(response.accessToken()));
        assertTrue(StringUtils.hasText(response.refreshToken()));

        RefreshToken updatedOldToken =
                refreshTokenDAO
                        .findByTokenHashSecure(persistedOldToken.getTokenHash())
                        .orElseThrow();

        assertTrue(updatedOldToken.isRevoked());
        assertNotNull(updatedOldToken.getRevokedAt());
        assertEquals("rotation", updatedOldToken.getRevokeReason());
        assertNotNull(updatedOldToken.getReplacedBy());

        String newHash = jwtManager.createTokenHash(response.refreshToken());

        RefreshToken newToken = refreshTokenDAO.findByTokenHashSecure(newHash).orElseThrow();

        assertEquals(familyId, newToken.getFamilyId());
        assertEquals(persistedUser.getId(), newToken.getUser().getId());
    }

    // ------------------------------------------------
    // generateJWTTokens(UserPrincipal, Session)
    // ------------------------------------------------

    @Test
    @Transactional
    void generateJWTTokens_whenUsingUserPrincipalAndSession_thenPersistRefreshToken() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(Instant.now());
        User persistedUser = userService.register(user);

        Session session = SessionProvider.singleTemplate();
        session.setCreatedAt(Instant.now());
        session.setUser(persistedUser);
        session.setFamilyId(uuidv7Generator.generate());
        session.setLastSeenAt(Instant.now());

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

    // ------------------------------------------------
    // generateJWTTokens(AuthCode)
    // ------------------------------------------------

    @Test
    @Transactional
    void generateJWTTokens_whenUsingAuthCode_thenCreateSessionAndPersistRefreshToken() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(Instant.now());
        User persistedUser = userService.register(user);

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

    // ------------------------------------------------
    // Delegations
    // ------------------------------------------------

    @Test
    @Transactional
    void delegationMethods_shouldWorkCorrectly() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(Instant.now());
        User persistedUser = userService.register(user);

        RefreshToken token = RefreshTokenProvider.singleTemplate();
        token.setCreatedAt(Instant.now());
        token.setUser(persistedUser);
        token.setFamilyId(uuidv7Generator.generate());
        token.setClientId("client");
        token.setUserAgent("agent");
        token.setIpAddress("127.0.0.1");
        token.setExpiresAt(Instant.now().plusSeconds(3600));

        String jwt = jwtManager.createRefreshToken(token);
        token.setTokenHash(jwtManager.createTokenHash(jwt));

        RefreshToken persisted = service.save(token);

        Optional<UUID> familyId = service.findFamilyIdByTokenHash(persisted.getTokenHash());

        assertTrue(familyId.isPresent());
        assertEquals(persisted.getFamilyId(), familyId.get());

        Optional<RefreshToken> found = service.findByTokenHashSecure(persisted.getTokenHash());

        assertTrue(found.isPresent());
        assertEquals(persisted.getId(), found.get().getId());

        assertDoesNotThrow(
                () ->
                        service.revokeFamilyWithReason(
                                persisted.getFamilyId(), Instant.now(), "reason"));

        assertDoesNotThrow(
                () ->
                        service.revokeRefreshTokensAndSessionByFamilyId(
                                persisted.getFamilyId(), Instant.now(), "reason-2"));
    }
}
