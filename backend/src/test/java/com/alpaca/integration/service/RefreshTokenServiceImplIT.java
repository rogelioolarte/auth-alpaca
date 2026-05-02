package com.alpaca.integration.service;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.entity.RefreshToken;
import com.alpaca.entity.Session;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.UnauthorizedException;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Integration tests for {@link RefreshTokenServiceImpl}. */
@SpringBootTest
@Transactional
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

    // -------------------------
    // Input validations (rotateRefreshToken)
    // -------------------------

    @Test
    void rotateRefreshToken_whenOldTokenBlank_thenBadRequest() {
        assertThrows(
                BadRequestException.class,
                () -> service.rotateRefreshToken(" ", "client", "agent", "127.0.0.1"));
    }

    @Test
    void rotateRefreshToken_whenClientIdBlank_thenBadRequest() {
        assertThrows(
                BadRequestException.class,
                () -> service.rotateRefreshToken("old", " ", "agent", "127.0.0.1"));
    }

    @Test
    void rotateRefreshToken_whenUserAgentBlank_thenBadRequest() {
        assertThrows(
                BadRequestException.class,
                () -> service.rotateRefreshToken("old", "client", " ", "127.0.0.1"));
    }

    @Test
    void rotateRefreshToken_whenClientIpBlank_thenBadRequest() {
        assertThrows(
                BadRequestException.class,
                () -> service.rotateRefreshToken("old", "client", "agent", " "));
    }

    // -------------------------
    // rotateRefreshToken : token not found -> Unauthorized
    // -------------------------

    @Test
    void rotateRefreshToken_whenTokenHashNotFound_thenUnauthorized() {
        // Use a random string that will not map to any saved token
        String fakeRaw = "this-token-does-not-exist";
        assertThrows(
                UnauthorizedException.class,
                () -> service.rotateRefreshToken(fakeRaw, "client", "agent", "127.0.0.1"));
    }

    // -------------------------
    // rotateRefreshToken : revoked session
    // -------------------------

    @Test
    @Transactional
    void rotateRefreshToken_whenSessionRevoked_thenUnauthorized() {
        // prepare user
        User persistedUser = userService.register(userTemplate);

        // prepare a family id
        UUID familyId = uuidv7Generator.generate();

        // create session and set revoked = true
        Session s = SessionProvider.singleTemplate();
        s.setFamilyId(familyId);
        s.setRevoked(true);
        s.setLastSeenAt(Instant.now());
        s.setUser(persistedUser);
        sessionService.save(s);

        // create refresh token that belongs to familyId and points to savedSession.getUser()
        RefreshToken token = RefreshTokenProvider.singleTemplate();
        token.setFamilyId(familyId);
        token.setClientId("client-1");
        token.setUserAgent("agent-1");
        token.setIpAddress("127.0.0.1");
        token.setUser(persistedUser);
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        // create jwt + hash
        String jwt = jwtManager.createRefreshToken(token);
        token.setTokenHash(jwtManager.createTokenHash(jwt));
        RefreshToken saved = service.save(token);

        // rotating should fail because session is revoked
        assertThrows(
                UnauthorizedException.class,
                () ->
                        service.rotateRefreshToken(
                                jwt,
                                saved.getClientId(),
                                saved.getUserAgent(),
                                saved.getIpAddress()));
    }

    // -------------------------
    // validateRefreshToken branches
    // -------------------------

    @Test
    @Transactional
    void rotateRefreshToken_whenTokenRevoked_andReplacedByNull_thenUnauthorizedRevokedToken() {
        User persistedUser = userService.register(userTemplate);

        RefreshToken token = RefreshTokenProvider.singleTemplate();
        token.setFamilyId(uuidv7Generator.generate());
        token.setUser(persistedUser);
        token.setRevoked(true);
        token.setReplacedBy(null);
        token.setClientId("client");
        token.setUserAgent("agent");
        token.setIpAddress("127.0.0.1");
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        String jwt = jwtManager.createRefreshToken(token);
        token.setTokenHash(jwtManager.createTokenHash(jwt));
        RefreshToken saved = service.save(token);

        UnauthorizedException ex =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        jwt,
                                        saved.getClientId(),
                                        saved.getUserAgent(),
                                        saved.getIpAddress()));
        assertTrue(ex.getMessage().contains("Revoked Refresh Token"));
    }

    @Test
    @Transactional
    void rotateRefreshToken_whenTokenRevoked_andReplacedByNotNull_thenUnauthorizedReuseDetected() {
        User persistedUser = userService.register(userTemplate);

        UUID familyId = uuidv7Generator.generate();

        // create original token (replaced)
        RefreshToken replaced = RefreshTokenProvider.singleTemplate();
        replaced.setFamilyId(familyId);
        replaced.setUser(persistedUser);
        replaced.setCreatedAt(Instant.now());
        replaced.setExpiresAt(Instant.now().plusSeconds(3600));
        String jwtReplaced = jwtManager.createRefreshToken(replaced);
        replaced.setTokenHash(jwtManager.createTokenHash(jwtReplaced));
        replaced = service.save(replaced);

        // create token that is revoked and has replacedBy set
        RefreshToken token = RefreshTokenProvider.singleTemplate();
        token.setFamilyId(familyId);
        token.setUser(persistedUser);
        token.setRevoked(true);
        token.setReplacedBy(replaced);
        token.setClientId("client");
        token.setUserAgent("agent");
        token.setIpAddress("127.0.0.1");
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        String jwt = jwtManager.createRefreshToken(token);
        token.setTokenHash(jwtManager.createTokenHash(jwt));
        RefreshToken saved = service.save(token);

        UnauthorizedException ex =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        jwt,
                                        saved.getClientId(),
                                        saved.getUserAgent(),
                                        saved.getIpAddress()));
        assertTrue(ex.getMessage().contains("Reuse Detected Refresh Token"));
    }

    @Test
    @Transactional
    void rotateRefreshToken_whenExpired_thenUnauthorizedExpired() {
        User persistedUser = userService.register(userTemplate);

        RefreshToken token = RefreshTokenProvider.singleTemplate();
        token.setFamilyId(uuidv7Generator.generate());
        token.setUser(persistedUser);
        token.setCreatedAt(Instant.now().minusSeconds(3600));
        token.setExpiresAt(Instant.now().minusSeconds(10)); // expired
        token.setClientId("client");
        token.setUserAgent("agent");
        token.setIpAddress("127.0.0.1");
        String jwt = jwtManager.createRefreshToken(token);
        token.setTokenHash(jwtManager.createTokenHash(jwt));
        RefreshToken saved = service.save(token);

        UnauthorizedException ex =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        jwt,
                                        saved.getClientId(),
                                        saved.getUserAgent(),
                                        saved.getIpAddress()));
        assertTrue(ex.getMessage().contains("Expired Refresh Token"));
    }

    @Test
    @Transactional
    void rotateRefreshToken_whenIssuedBeforeTokensInvalidBefore_thenUnauthorized() {
        // user with tokensInvalidBefore set after token.createdAt -> should fail
        User persistedUser = userService.register(userTemplate);
        Instant tokensInvalidBefore = Instant.now().plusSeconds(3600);
        persistedUser.setTokensInvalidBefore(tokensInvalidBefore);
        userService.register(persistedUser); // persist update

        RefreshToken token = RefreshTokenProvider.singleTemplate();
        token.setFamilyId(uuidv7Generator.generate());
        token.setUser(persistedUser);
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        token.setClientId("client");
        token.setUserAgent("agent");
        token.setIpAddress("127.0.0.1");
        String jwt = jwtManager.createRefreshToken(token);
        token.setTokenHash(jwtManager.createTokenHash(jwt));
        RefreshToken saved = service.save(token);

        UnauthorizedException ex =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        jwt,
                                        saved.getClientId(),
                                        saved.getUserAgent(),
                                        saved.getIpAddress()));
        assertTrue(ex.getMessage().contains("Refresh Token issued before tokens_invalid_before"));
    }

    @Test
    @Transactional
    void rotateRefreshToken_whenClientIdMismatch_thenUnauthorizedClientMismatch() {
        User persistedUser = userService.register(userTemplate);

        RefreshToken token = RefreshTokenProvider.singleTemplate();
        token.setFamilyId(uuidv7Generator.generate());
        token.setUser(persistedUser);
        token.setClientId("client-A");
        token.setUserAgent("agent");
        token.setIpAddress("127.0.0.1");
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        String jwt = jwtManager.createRefreshToken(token);
        token.setTokenHash(jwtManager.createTokenHash(jwt));
        RefreshToken saved = service.save(token);

        UnauthorizedException ex =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        jwt,
                                        "client-B",
                                        saved.getUserAgent(),
                                        saved.getIpAddress()));
        assertTrue(ex.getMessage().contains("Client mismatch"));
    }

    @Test
    @Transactional
    void rotateRefreshToken_whenUserAgentMismatch_thenUnauthorizedUaMismatch() {
        User persistedUser = userService.register(userTemplate);

        RefreshToken token = RefreshTokenProvider.singleTemplate();
        token.setFamilyId(uuidv7Generator.generate());
        token.setUser(persistedUser);
        token.setClientId("client");
        token.setUserAgent("agent-A");
        token.setIpAddress("127.0.0.1");
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        String jwt = jwtManager.createRefreshToken(token);
        token.setTokenHash(jwtManager.createTokenHash(jwt));
        RefreshToken saved = service.save(token);

        UnauthorizedException ex =
                assertThrows(
                        UnauthorizedException.class,
                        () ->
                                service.rotateRefreshToken(
                                        jwt, saved.getClientId(), "agent-B", saved.getIpAddress()));
        assertTrue(ex.getMessage().contains("User-Agent mismatch"));
    }

    // -------------------------
    // rotateRefreshToken success path
    // -------------------------

    @Test
    @Transactional
    void rotateRefreshToken_whenValid_thenRotateAndReturnTokens() {
        User persistedUser = userService.register(userTemplate);

        UUID familyId = uuidv7Generator.generate();

        // create old token valid
        RefreshToken old = RefreshTokenProvider.singleTemplate();
        old.setFamilyId(familyId);
        old.setUser(persistedUser);
        old.setClientId("client-ok");
        old.setUserAgent("agent-ok");
        old.setIpAddress("127.0.0.1");
        old.setRevoked(false);
        old.setRevokedAt(null);
        Instant now = Instant.now();
        old.setCreatedAt(now.minusSeconds(10));
        old.setExpiresAt(now.plusSeconds(5000));

        String oldJwt = jwtManager.createRefreshToken(old);
        old.setTokenHash(jwtManager.createTokenHash(oldJwt));
        RefreshToken savedOld = service.save(old);

        // create a session not revoked for that family id
        Session s = SessionProvider.singleTemplate();
        s.setFamilyId(familyId);
        s.setClientId(savedOld.getClientId());
        s.setIpAddress(savedOld.getIpAddress());
        s.setUserAgent(savedOld.getUserAgent());
        s.setUser(persistedUser);
        s.setLastSeenAt(now);
        s.setRevoked(false);
        s.setRevokedAt(null);
        sessionService.save(s);

        // rotate
        AuthResponseDTO response =
                service.rotateRefreshToken(
                        oldJwt,
                        savedOld.getClientId(),
                        savedOld.getUserAgent(),
                        savedOld.getIpAddress());

        assertNotNull(response);
        assertTrue(StringUtils.hasText(response.accessToken()));
        assertTrue(StringUtils.hasText(response.refreshToken()));

        // Old token should be revoked and replacedBy set
        RefreshToken reloadedOld =
                refreshTokenDAO.findByTokenHashSecure(savedOld.getTokenHash()).orElse(null);
        assertNotNull(reloadedOld);
        assertTrue(reloadedOld.isRevoked());
        assertNotNull(reloadedOld.getReplacedBy());

        // New token must be persisted with tokenHash from response.refreshToken()
        String newHash = jwtManager.createTokenHash(response.refreshToken());
        Optional<RefreshToken> newTokenOpt = refreshTokenDAO.findByTokenHashSecure(newHash);
        assertTrue(newTokenOpt.isPresent());
        RefreshToken newToken = newTokenOpt.get();
        assertEquals(savedOld.getFamilyId(), newToken.getFamilyId());
        assertEquals(savedOld.getUser().getId(), newToken.getUser().getId());
    }

    // -------------------------
    // generateJWTTokens
    // -------------------------

    @Test
    @Transactional
    void generateJWTTokens_shouldCreateAndPersistRefreshTokenAndReturnTokens() {
        User persistedUser = userService.register(userTemplate);
        UUID familyId = UUID.randomUUID();
        // create and persist session used for tokens
        Session s = SessionProvider.singleTemplate();
        s.setUser(persistedUser);
        s.setFamilyId(familyId);
        s.setLastSeenAt(Instant.now());
        s.setClientId("client-gen");
        Session savedSession = sessionService.save(s);

        // prepare user principal-like object (service.create uses UserPrincipal when building
        // access token)
        // but generateJWTTokens signature accepts UserPrincipal and Session; here we only need
        // session and user principal
        com.alpaca.model.UserPrincipal userPrincipal =
                new com.alpaca.model.UserPrincipal(persistedUser);

        AuthResponseDTO response = service.generateJWTTokens(userPrincipal, savedSession);

        assertNotNull(response);
        assertTrue(StringUtils.hasText(response.accessToken()));
        assertTrue(StringUtils.hasText(response.refreshToken()));

        // token should be persisted
        String refreshHash = jwtManager.createTokenHash(response.refreshToken());
        Optional<RefreshToken> persisted = refreshTokenDAO.findByTokenHashSecure(refreshHash);
        assertTrue(persisted.isPresent());
        assertEquals(persistedUser.getId(), persisted.get().getUser().getId());
    }

    // -------------------------
    // thin delegations
    // -------------------------

    @Test
    @Transactional
    void findFamilyIdAndFindByTokenHashSecure_andRevokeFamily_shouldWork() {
        User persistedUser = userService.register(userTemplate);

        RefreshToken token = RefreshTokenProvider.singleTemplate();
        token.setFamilyId(uuidv7Generator.generate());
        token.setUser(persistedUser);
        token.setClientId("c");
        token.setUserAgent("a");
        token.setIpAddress("127.0.0.1");
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        String jwt = jwtManager.createRefreshToken(token);
        token.setTokenHash(jwtManager.createTokenHash(jwt));
        RefreshToken saved = service.save(token);

        // findFamilyIdByTokenHash
        Optional<UUID> fam = service.findFamilyIdByTokenHash(saved.getTokenHash());
        assertTrue(fam.isPresent());
        assertEquals(saved.getFamilyId(), fam.get());

        // findByTokenHashSecure
        Optional<RefreshToken> found = service.findByTokenHashSecure(saved.getTokenHash());
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());

        // revoke family just ensures DAO call does not fail (no exception)
        service.revokeFamilyWithReason(saved.getFamilyId(), Instant.now(), "test-reason");
        // and wrapper
        service.revokeRefreshTokensAndSessionByFamilyId(
                saved.getFamilyId(), Instant.now(), "test-reason-2");
    }
}
