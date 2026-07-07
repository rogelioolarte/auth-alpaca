package com.alpaca.unit.security.manager;

import com.alpaca.entity.RefreshToken;
import com.alpaca.entity.User;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.security.manager.JJwtManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Unit tests for {@link JJwtManager}. */
@DisplayName("JJwtManager Unit Tests")
class JJwtManagerTest {

    private static final String ISSUER = "alpaca-auth-service";

    private JJwtManager jwtManager;

    @BeforeEach
    void setUp() throws Exception {

        String accessExpiration = "3600000";
        String refreshExpiration = "86400000";

        jwtManager =
                new JJwtManager(
                        new ClassPathResource("keys/access_private.pem"),
                        new ClassPathResource("keys/access_public.pem"),
                        accessExpiration,
                        new ClassPathResource("keys/refresh_private.pem"),
                        new ClassPathResource("keys/refresh_public.pem"),
                        refreshExpiration,
                        ISSUER);
    }

    @Test
    @DisplayName("createAccessToken should create valid access token")
    void createAccessToken_ShouldCreateValidAccessToken() {

        UserPrincipal principal = mock(UserPrincipal.class);

        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID advertiserId = UUID.randomUUID();

        when(principal.getUsername()).thenReturn("rogelio.olarte");
        when(principal.getUserId()).thenReturn(userId);
        when(principal.getProfileId()).thenReturn(profileId);
        when(principal.getAdvertiserId()).thenReturn(advertiserId);
        when(principal.getAuthorities())
                .thenReturn(AuthorityUtils.createAuthorityList("ROLE_ADMIN", "ROLE_USER"));

        Instant now = Instant.now();

        String token = jwtManager.createAccessToken(principal, now);

        Claims claims = jwtManager.validateAccessToken(token);

        assertAll(
                () -> assertEquals(ISSUER, claims.getIssuer()),
                () -> assertEquals("rogelio.olarte", claims.getSubject()),
                () -> assertEquals(userId.toString(), claims.get("userId")),
                () -> assertEquals(profileId.toString(), claims.get("profileId")),
                () -> assertEquals(advertiserId.toString(), claims.get("advertiserId")),
                () -> assertTrue(claims.get("authorities", String.class).contains("ROLE_ADMIN")),
                () -> assertTrue(jwtManager.isValidAccessToken(claims)));
    }

    @Test
    @DisplayName("createAccessToken should handle null optional identifiers")
    void createAccessToken_ShouldHandleNullOptionalIdentifiers() {

        UserPrincipal principal = mock(UserPrincipal.class);

        UUID userId = UUID.randomUUID();

        when(principal.getUsername()).thenReturn("no-optionals");
        when(principal.getUserId()).thenReturn(userId);
        when(principal.getProfileId()).thenReturn(null);
        when(principal.getAdvertiserId()).thenReturn(null);
        when(principal.getAuthorities()).thenReturn(Collections.emptyList());

        String token = jwtManager.createAccessToken(principal, Instant.now());

        Claims claims = jwtManager.validateAccessToken(token);

        assertAll(
                () -> assertEquals("", claims.get("profileId")),
                () -> assertEquals("", claims.get("advertiserId")));
    }

    @Test
    @DisplayName("createRefreshToken should create valid refresh token")
    void createRefreshToken_ShouldCreateValidRefreshToken() {

        RefreshToken refreshToken = mock(RefreshToken.class);
        User user = mock(User.class);

        UUID userId = UUID.randomUUID();
        UUID tokenJti = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();

        Instant lastUsedAt = Instant.now();
        Instant expiresAt = lastUsedAt.plus(1, ChronoUnit.DAYS);

        String clientId = "web-client";

        when(refreshToken.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(userId);
        when(refreshToken.getTokenJti()).thenReturn(tokenJti);
        when(refreshToken.getFamilyId()).thenReturn(familyId);
        when(refreshToken.getLastUsedAt()).thenReturn(lastUsedAt);
        when(refreshToken.getExpiresAt()).thenReturn(expiresAt);
        when(refreshToken.getClientId()).thenReturn(clientId);

        String token = jwtManager.createRefreshToken(refreshToken);

        Claims claims = jwtManager.validateRefreshToken(token);

        assertAll(
                () -> assertEquals(ISSUER, claims.getIssuer()),
                () -> assertEquals(userId.toString(), claims.getSubject()),
                () -> assertEquals(userId.toString(), claims.get("userId")),
                () -> assertEquals(tokenJti.toString(), claims.get("jti")),
                () -> assertEquals(familyId.toString(), claims.get("familyId")),
                () -> assertEquals(clientId, claims.get("clientId")),
                () -> assertTrue(jwtManager.isValidRefreshToken(claims)));
    }

    @Test
    @DisplayName("createRefreshToken should handle null values")
    void createRefreshToken_ShouldHandleNullValues() {

        RefreshToken refreshToken = mock(RefreshToken.class);

        Instant lastUsedAt = Instant.now();
        Instant expiresAt = lastUsedAt.plus(1, ChronoUnit.DAYS);

        when(refreshToken.getUser()).thenReturn(null);
        when(refreshToken.getTokenJti()).thenReturn(null);
        when(refreshToken.getFamilyId()).thenReturn(null);
        when(refreshToken.getLastUsedAt()).thenReturn(lastUsedAt);
        when(refreshToken.getExpiresAt()).thenReturn(expiresAt);
        when(refreshToken.getClientId()).thenReturn(null);

        String token = jwtManager.createRefreshToken(refreshToken);

        Claims claims = jwtManager.validateRefreshToken(token);

        assertAll(
                () -> assertEquals("", claims.get("userId")),
                () -> assertEquals("", claims.get("familyId")),
                () -> assertNull(claims.getSubject()),
                () -> assertNull(claims.get("jti")),
                () -> assertNull(claims.get("clientId")));
    }

    @Test
    @DisplayName("createTokenHash should create deterministic hash")
    void createTokenHash_ShouldCreateDeterministicHash() {

        String value = "refresh-token-value";

        String hashOne = jwtManager.createTokenHash(value);

        String hashTwo = jwtManager.createTokenHash(value);

        assertAll(
                () -> assertNotNull(hashOne),
                () -> assertEquals(hashOne, hashTwo),
                () -> assertFalse(hashOne.isBlank()),
                () -> assertFalse(hashOne.contains("=")));
    }

    @Test
    @DisplayName("createTokenHash should throw exception for invalid value")
    void createTokenHash_ShouldThrowExceptionForInvalidValue() {

        assertAll(
                () ->
                        assertThrows(
                                IllegalArgumentException.class,
                                () -> jwtManager.createTokenHash(null)),
                () ->
                        assertThrows(
                                IllegalArgumentException.class,
                                () -> jwtManager.createTokenHash("")),
                () ->
                        assertThrows(
                                IllegalArgumentException.class,
                                () -> jwtManager.createTokenHash(" ")));
    }

    @Test
    @DisplayName("validateAccessToken should throw unauthorized exception for invalid token")
    void validateAccessToken_ShouldThrowUnauthorizedException() {

        assertAll(
                () ->
                        assertThrows(
                                UnauthorizedException.class,
                                () -> jwtManager.validateAccessToken("invalid.token")),
                () ->
                        assertThrows(
                                UnauthorizedException.class,
                                () -> jwtManager.validateAccessToken(null)));
    }

    @Test
    @DisplayName("validateRefreshToken should throw unauthorized exception for invalid token")
    void validateRefreshToken_ShouldThrowUnauthorizedException() {

        assertAll(
                () ->
                        assertThrows(
                                UnauthorizedException.class,
                                () -> jwtManager.validateRefreshToken("invalid.token")),
                () ->
                        assertThrows(
                                UnauthorizedException.class,
                                () -> jwtManager.validateRefreshToken(null)));
    }

    @Test
    @DisplayName("isValidAccessToken should validate claims correctly")
    void isValidAccessToken_ShouldValidateClaimsCorrectly() {

        Claims validClaims =
                Jwts.claims()
                        .subject("user")
                        .expiration(new Date(System.currentTimeMillis() + 10_000))
                        .add("userId", UUID.randomUUID().toString())
                        .add("authorities", "ROLE_USER")
                        .build();

        Claims expiredClaims =
                Jwts.claims()
                        .subject("user")
                        .expiration(new Date(System.currentTimeMillis() - 10_000))
                        .add("userId", UUID.randomUUID().toString())
                        .add("authorities", "ROLE_USER")
                        .build();

        Claims blankSubjectClaims =
                Jwts.claims()
                        .subject("")
                        .expiration(new Date(System.currentTimeMillis() + 10_000))
                        .add("userId", UUID.randomUUID().toString())
                        .add("authorities", "ROLE_USER")
                        .build();

        Claims missingAuthoritiesClaims =
                Jwts.claims()
                        .subject("user")
                        .expiration(new Date(System.currentTimeMillis() + 10_000))
                        .add("userId", UUID.randomUUID().toString())
                        .build();

        assertAll(
                () -> assertTrue(jwtManager.isValidAccessToken(validClaims)),
                () -> assertFalse(jwtManager.isValidAccessToken(expiredClaims)),
                () -> assertFalse(jwtManager.isValidAccessToken(blankSubjectClaims)),
                () -> assertFalse(jwtManager.isValidAccessToken(missingAuthoritiesClaims)));
    }

    @Test
    @DisplayName("isValidRefreshToken should validate claims correctly")
    void isValidRefreshToken_ShouldValidateClaimsCorrectly() {

        Claims validClaims =
                Jwts.claims()
                        .subject("user")
                        .expiration(new Date(System.currentTimeMillis() + 10_000))
                        .add("userId", UUID.randomUUID().toString())
                        .add("jti", UUID.randomUUID().toString())
                        .add("familyId", UUID.randomUUID().toString())
                        .add("clientId", "web-client")
                        .build();

        Claims invalidClaims =
                Jwts.claims()
                        .subject("user")
                        .expiration(new Date(System.currentTimeMillis() + 10_000))
                        .add("userId", UUID.randomUUID().toString())
                        .build();

        assertAll(
                () -> assertTrue(jwtManager.isValidRefreshToken(validClaims)),
                () -> assertFalse(jwtManager.isValidRefreshToken(invalidClaims)));
    }

    @Test
    @DisplayName("manageAuthentication should create authentication from token")
    void manageAuthentication_ShouldCreateAuthenticationFromToken() {

        UserPrincipal principal = mock(UserPrincipal.class);

        UUID userId = UUID.randomUUID();

        when(principal.getUsername()).thenReturn("tester");
        when(principal.getUserId()).thenReturn(userId);
        when(principal.getAuthorities())
                .thenReturn(AuthorityUtils.createAuthorityList("ROLE_USER"));

        String token = jwtManager.createAccessToken(principal, Instant.now());

        UsernamePasswordAuthenticationToken authentication = jwtManager.manageAuthentication(token);

        UserPrincipal authenticatedPrincipal = (UserPrincipal) authentication.getPrincipal();

        assertNotNull(authenticatedPrincipal);
        assertAll(
                () -> assertNotNull(authentication),
                () -> assertEquals("tester", authenticatedPrincipal.getUsername()),
                () -> assertEquals(userId, authenticatedPrincipal.getUserId()));
    }

    @Test
    @DisplayName("createAuthentication should return null for invalid claims")
    void createAuthentication_ShouldReturnNullForInvalidClaims() {

        Claims claims = Jwts.claims().subject("user").build();

        UsernamePasswordAuthenticationToken authentication =
                jwtManager.createAuthentication(claims);

        assertNull(authentication);
    }

    @Test
    @DisplayName("constructor should expose configured expiration values")
    void constructor_ShouldExposeConfiguredExpirationValues() {

        assertAll(
                () -> assertEquals(3600000L, jwtManager.getJwtTimeExpAccess()),
                () -> assertEquals(86400000L, jwtManager.getJwtTimeExpRefresh()));
    }

    @Test
    @DisplayName("constructor should throw exception for invalid key content")
    void constructor_ShouldThrowExceptionForInvalidKeyContent() {

        Resource invalidResource = new ByteArrayResource("invalid-key".getBytes());

        assertThrows(
                Exception.class,
                () ->
                        new JJwtManager(
                                invalidResource,
                                invalidResource,
                                "1000",
                                invalidResource,
                                invalidResource,
                                "1000",
                                ISSUER));
    }
}
