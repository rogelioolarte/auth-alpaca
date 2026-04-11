package com.alpaca.unit.security.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alpaca.entity.RefreshToken;
import com.alpaca.entity.User;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.resources.RefreshTokenProvider;
import com.alpaca.security.manager.JJwtManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

/** Unit tests for {@link JJwtManager} */
@DisplayName("JJwtManager Unit Tests")
class JJwtManagerTest {

    private static final String ISSUER = "testIssuer";
    private static final long EXPIRATION_MILLIS_ACCESS = 20_000; // 20s to be safe in tests
    private static final long EXPIRATION_MILLIS_REFRESH = 600_000; // 10 min
    private JJwtManager jwtManager;

    @BeforeEach
    void setUp() throws Exception {
        ClassPathResource privateKeyResourceAccess =
                new ClassPathResource("keys/access_private.pem");
        ClassPathResource publicKeyResourceAccess = new ClassPathResource("keys/access_public.pem");
        ClassPathResource privateKeyResourceRefresh =
                new ClassPathResource("keys/refresh_private.pem");
        ClassPathResource publicKeyResourceRefresh =
                new ClassPathResource("keys/refresh_public.pem");

        assertTrue(
                privateKeyResourceAccess.exists(),
                "access_private.pem must be present in test resources");
        assertTrue(
                publicKeyResourceAccess.exists(),
                "access_public.pem must be present in test resources");
        assertTrue(
                privateKeyResourceRefresh.exists(),
                "refresh_private.pem must be present in test resources");
        assertTrue(
                publicKeyResourceRefresh.exists(),
                "refresh_public.pem must be present in test resources");

        jwtManager =
                new JJwtManager(
                        privateKeyResourceAccess,
                        publicKeyResourceAccess,
                        String.valueOf(EXPIRATION_MILLIS_ACCESS),
                        privateKeyResourceRefresh,
                        publicKeyResourceRefresh,
                        String.valueOf(EXPIRATION_MILLIS_REFRESH),
                        ISSUER);
    }

    @Test
    @DisplayName(
            "createRefreshTokenHash: throws on empty / blank input and returns non-empty for valid"
                    + " input")
    void createRefreshTokenHash_validAndInvalidCases() {
        // invalid: null or blank -> IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> jwtManager.createRefreshTokenHash(""));
        assertThrows(
                IllegalArgumentException.class, () -> jwtManager.createRefreshTokenHash("   "));

        // valid: returns a Base64-URL string (non-empty)
        String sample = "some-refresh-token-string";
        String hash = jwtManager.createRefreshTokenHash(sample);
        assertNotNull(hash);
        assertFalse(hash.isBlank());
        // it should be base64-url (no padding)
        assertFalse(hash.contains("="));
        // ensure decoder doesn't throw (URL-safe base64)
        byte[] decoded = Base64.getUrlDecoder().decode(hash);
        assertTrue(decoded.length > 0);
    }

    @Test
    @DisplayName("validateAccessToken throws UnauthorizedException for malformed token")
    void validateTokenThrowsOnInvalidAccessToken() {
        String badToken = "this.is.not.a.valid.jwt";
        assertThrows(UnauthorizedException.class, () -> jwtManager.validateAccessToken(badToken));
    }

    @Test
    @DisplayName("validateRefreshToken throws UnauthorizedException for malformed token")
    void validateRefreshTokenThrowsOnInvalidRefreshToken() {
        String badToken = "also.not.valid";
        assertThrows(UnauthorizedException.class, () -> jwtManager.validateRefreshToken(badToken));
    }

    @Test
    @DisplayName("createAccessToken -> validateAccessToken -> isValidAccessToken happy path")
    void createAccessTokenProducesValidJwt_andClaimsAreCorrect() {
        // Prepare a mock UserPrincipal (we only need getters used by manager)
        UserPrincipal mockPrincipal = mock(UserPrincipal.class);
        UUID userId = UUID.randomUUID();
        when(mockPrincipal.getUsername()).thenReturn("testUser");
        when(mockPrincipal.getId()).thenReturn(userId);
        when(mockPrincipal.getProfileId()).thenReturn(null);
        when(mockPrincipal.getAdvertiserId()).thenReturn(null);
        // authorities: 2 roles
        Collection<GrantedAuthority> auths =
                AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN");
        when(mockPrincipal.getAuthorities()).thenReturn(auths);

        Instant now = Instant.now();
        String token = jwtManager.createAccessToken(mockPrincipal, now);
        assertNotNull(token);
        assertFalse(token.isBlank());

        // validate and inspect claims
        Claims claims = jwtManager.validateAccessToken(token);
        assertEquals(ISSUER, claims.getIssuer());
        assertEquals("testUser", claims.getSubject());
        assertEquals(userId.toString(), claims.get("userId", String.class));
        assertNotNull(claims.get("authorities", String.class));
        assertTrue(jwtManager.isValidAccessToken(claims));
    }

    @Test
    @DisplayName(
            "manageAuthentication returns Authentication with correct principal and authorities")
    void manageAuthenticationReturnsValidAuthentication() {
        // Build an actual valid token via createAccessToken using a real principal
        UserPrincipal mockPrincipal = mock(UserPrincipal.class);
        UUID userId = UUID.randomUUID();
        when(mockPrincipal.getUsername()).thenReturn("anotherUser");
        when(mockPrincipal.getId()).thenReturn(userId);
        when(mockPrincipal.getProfileId()).thenReturn(null);
        when(mockPrincipal.getAdvertiserId()).thenReturn(null);
        when(mockPrincipal.getAuthorities())
                .thenReturn(AuthorityUtils.createAuthorityList("ROLE_X", "ROLE_Y"));

        Instant now = Instant.now();
        String token = jwtManager.createAccessToken(mockPrincipal, now);

        UsernamePasswordAuthenticationToken auth = jwtManager.manageAuthentication(token);
        assertNotNull(auth);
        assertTrue(auth.isAuthenticated());
        Object principalObj = auth.getPrincipal();
        assertNotNull(principalObj);
        assertInstanceOf(UserPrincipal.class, principalObj, "principal should be a UserPrincipal");
        com.alpaca.model.UserPrincipal up = (com.alpaca.model.UserPrincipal) principalObj;
        assertEquals("anotherUser", up.getUsername());
        // verify authorities present
        assertNotNull(auth.getAuthorities());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_X")));
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_Y")));
    }

    @Test
    @DisplayName("createAuthentication returns null for expired claims")
    void createAuthenticationReturnsNullWhenExpired() {
        Claims expiredClaims =
                Jwts.claims()
                        .subject("ghostUser")
                        .expiration(new Date(System.currentTimeMillis() - 1_000))
                        .add("authorities", "ROLE_GHOST")
                        .add("userId", UUID.randomUUID().toString())
                        .add("profileId", "")
                        .add("advertiserId", "")
                        .build();

        UsernamePasswordAuthenticationToken auth = jwtManager.createAuthentication(expiredClaims);
        assertNull(auth, "Authentication should be null for expired claims");
    }

    @Test
    @DisplayName("isValidAccessToken handles missing/blank fields correctly")
    void isValidAccessTokenNegativeScenarios() {
        // missing expiration
        Claims noExp =
                Jwts.claims()
                        .subject("user")
                        .add("userId", UUID.randomUUID().toString())
                        .add("authorities", "ROLE_TEST")
                        .build();
        assertFalse(jwtManager.isValidAccessToken(noExp));

        // expired in past
        Claims past =
                Jwts.claims()
                        .subject("user")
                        .expiration(new Date(System.currentTimeMillis() - 10_000))
                        .add("userId", UUID.randomUUID().toString())
                        .add("authorities", "ROLE_TEST")
                        .build();
        assertFalse(jwtManager.isValidAccessToken(past));

        // blank subject
        Claims blankSub =
                Jwts.claims()
                        .subject("   ")
                        .expiration(new Date(System.currentTimeMillis() + 10_000))
                        .add("userId", UUID.randomUUID().toString())
                        .add("authorities", "ROLE_TEST")
                        .build();
        assertFalse(jwtManager.isValidAccessToken(blankSub));

        // missing userId
        Claims noUserId =
                Jwts.claims()
                        .subject("user")
                        .expiration(new Date(System.currentTimeMillis() + 10_000))
                        .add("authorities", "ROLE_TEST")
                        .build();
        assertFalse(jwtManager.isValidAccessToken(noUserId));

        // missing authorities
        Claims noAuth =
                Jwts.claims()
                        .subject("user")
                        .expiration(new Date(System.currentTimeMillis() + 10_000))
                        .add("userId", UUID.randomUUID().toString())
                        .add("authorities", null)
                        .build();
        assertFalse(jwtManager.isValidAccessToken(noAuth));
    }

    @Test
    @DisplayName("createRefreshToken -> validateRefreshToken -> isValidRefreshToken happy path")
    void createRefreshTokenAndValidateAndIsValidRefreshToken() {
        // Use provider to create a RefreshToken entity (provider must set all required fields)
        RefreshToken refresh = RefreshTokenProvider.singleEntity();
        // sanity checks (provider ensures user present)
        User u = refresh.getUser();
        assertNotNull(u);
        assertNotNull(u.getId());

        String jwtRefresh = jwtManager.createRefreshToken(refresh);
        assertNotNull(jwtRefresh);
        assertFalse(jwtRefresh.isBlank());

        Claims claims = jwtManager.validateRefreshToken(jwtRefresh);
        assertEquals(u.getId().toString(), claims.getSubject());
        assertNotNull(claims.get("jti", String.class));
        assertNotNull(claims.get("familyId", String.class));
        assertNotNull(claims.get("clientId", String.class));
        assertTrue(jwtManager.isValidRefreshToken(claims));
    }

    @Test
    @DisplayName("isValidRefreshToken returns false when important claims are missing")
    void isValidRefreshTokenNegativeCases() {
        Claims missingJti =
                Jwts.claims()
                        .subject("sub")
                        .expiration(new Date(System.currentTimeMillis() + 10_000))
                        .add("userId", UUID.randomUUID().toString())
                        .add("familyId", UUID.randomUUID().toString())
                        // missing jti
                        .add("clientId", "c")
                        .build();
        assertFalse(jwtManager.isValidRefreshToken(missingJti));

        Claims missingFamily =
                Jwts.claims()
                        .subject("sub")
                        .expiration(new Date(System.currentTimeMillis() + 10_000))
                        .add("userId", UUID.randomUUID().toString())
                        .add("jti", UUID.randomUUID().toString())
                        // missing familyId
                        .add("clientId", "c")
                        .build();
        assertFalse(jwtManager.isValidRefreshToken(missingFamily));

        Claims missingClientId =
                Jwts.claims()
                        .subject("sub")
                        .expiration(new Date(System.currentTimeMillis() + 10_000))
                        .add("userId", UUID.randomUUID().toString())
                        .add("jti", UUID.randomUUID().toString())
                        .add("familyId", UUID.randomUUID().toString())
                        // missing clientId
                        .build();
        assertFalse(jwtManager.isValidRefreshToken(missingClientId));
    }
}
