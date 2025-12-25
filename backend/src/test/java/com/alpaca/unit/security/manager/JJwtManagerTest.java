package com.alpaca.unit.security.manager;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.exception.UnauthorizedException;
import com.alpaca.security.manager.JJwtManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/** Unit tests for {@link JJwtManager} */
@DisplayName("JJwtManager Unit Tests")
class JJwtManagerTest {

    private static final String ISSUER = "testIssuer";
    private static final String USER_GENERATOR = "testGenerator";
    private static final long EXPIRATION_MILLIS_ACCESS = 2_000; // 2s
    private static final long EXPIRATION_MILLIS_REFRESH = 600000; // 10 min
    private JJwtManager jwtManager;

    @BeforeEach
    void setUp() throws Exception {
        // Load keys from classpath test resources
        ClassPathResource privateKeyResourceAccess =
                new ClassPathResource("keys/access_private.pem");
        ClassPathResource publicKeyResourceAccess = new ClassPathResource("keys/access_public.pem");
        ClassPathResource privateKeyResourceRefresh =
                new ClassPathResource("keys/refresh_private.pem");
        ClassPathResource publicKeyResourceRefresh =
                new ClassPathResource("keys/refresh_public.pem");

        assertTrue(
                privateKeyResourceAccess.exists(),
                "access_private.pem should be on test classpath");
        assertTrue(
                publicKeyResourceAccess.exists(), "access_public.pem should be on test classpath");

        // Instantiate manager using Resource instances carried from classpath
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

    //    @Test
    //    @DisplayName("createToken should produce a valid JWT containing the expected claims")
    //    void createAccessTokenProducesValidJwt() {
    //        UUID userId = UUID.randomUUID();
    //        UUID profileId = UUID.randomUUID();
    //        UUID advertiserId = UUID.randomUUID();
    //        String username = "testUser";
    //        List<GrantedAuthority> authorities =
    //                AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN");
    //
    //        UserPrincipal principal =
    //                new UserPrincipal(
    //                        userId, profileId, advertiserId, username, null, authorities, null);
    //
    //        String token = jwtManager.createAccessToken(principal);
    //        assertNotNull(token, "Token should not be null or empty");
    //        assertFalse(token.isBlank(), "Token should not be blank");
    //
    //        // Parse the token directly to inspect claims
    //        Claims claims = jwtManager.validateAccessToken(token);
    //
    //        assertEquals(ISSUER, claims.getIssuer(), "Issuer claim must match");
    //        assertEquals(username, claims.getSubject(), "Subject must be the username");
    //        assertEquals(
    //                userId.toString(), claims.get("userId", String.class), "userId claim must
    // match");
    //        assertEquals(
    //                profileId.toString(),
    //                claims.get("profileId", String.class),
    //                "profileId claim must match");
    //        assertEquals(
    //                advertiserId.toString(),
    //                claims.get("advertiserId", String.class),
    //                "advertiserId claim must match");
    //
    //        // The authorities claim is a comma-separated string
    //        String authClaim = claims.get("authorities", String.class);
    //        assertNotNull(authClaim);
    //        assertTrue(authClaim.contains("ROLE_USER"));
    //        assertTrue(authClaim.contains("ROLE_ADMIN"));
    //
    //        Date now = new Date();
    //        assertTrue(
    //                claims.getIssuedAt().before(new Date(now.getTime() + 100)),
    //                "IssuedAt should be very close to now");
    //        assertTrue(
    //                claims.getNotBefore().before(new Date(now.getTime() + 100)),
    //                "NotBefore should be very close to now");
    //        assertTrue(claims.getExpiration().after(now), "Expiration must be in the future");
    //    }

    @Test
    @DisplayName("validateToken should throw UnauthorizedException on invalid token")
    void validateTokenThrowsOnInvalidAccessToken() {
        String badToken = "this.is.not.a.valid.jwt";
        assertThrows(UnauthorizedException.class, () -> jwtManager.validateAccessToken(badToken));
    }

    //    @Test
    //    @DisplayName("manageAuthentication should return a valid Authentication when token is
    // valid")
    //    void manageAuthenticationReturnsValidAuthentication() {
    //        UUID userId = UUID.randomUUID();
    //        String username = "anotherUser";
    //        List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("ROLE_X",
    // "ROLE_Y");
    //
    //        // allow advertiserId to be null in this test
    //        UserPrincipal principal =
    //                new UserPrincipal(userId, null, null, username, null, authorities, null);
    //
    //        String token = jwtManager.createAccessToken(principal);
    //        UsernamePasswordAuthenticationToken auth = jwtManager.manageAuthentication(token);
    //
    //        assertNotNull(auth, "Authentication token should not be null");
    //        assertTrue(
    //                auth.isAuthenticated(),
    //                "Authentication should be marked as authenticated by default");
    //
    //        // The principal inside Authentication should be a UserPrincipal with same values
    //        Object authPrincipal = auth.getPrincipal();
    //        assertInstanceOf(UserPrincipal.class, authPrincipal, "Principal should be a
    // UserPrincipal");
    //        UserPrincipal up = (UserPrincipal) authPrincipal;
    //
    //        assertEquals(userId, up.getId(), "UserPrincipal.id must match");
    //        assertNull(up.getProfileId(), "UserPrincipal.profileId should be null");
    //        assertNull(up.getAdvertiserId(), "UserPrincipal.advertiserId should be null");
    //        assertEquals(username, up.getUsername(), "UserPrincipal.username must match");
    //
    //        // Authorities in the Authentication should match those in UserPrincipal
    //        Collection<? extends GrantedAuthority> authList = auth.getAuthorities();
    //        assertEquals(2, authList.size(), "Should have two authorities");
    //        assertTrue(authList.stream().anyMatch(a -> a.getAuthority().equals("ROLE_X")));
    //        assertTrue(authList.stream().anyMatch(a -> a.getAuthority().equals("ROLE_Y")));
    //    }

    @Test
    @DisplayName("createAuthentication returns null for claims with expired expiration")
    void createAuthenticationReturnsNullWhenExpired() {
        // Build a Claims object manually
        Claims expiredClaims =
                Jwts.claims()
                        .subject("ghostUser")
                        .expiration(new Date(System.currentTimeMillis() - 1_000))
                        .add("authorities", "ROLE_GHOST")
                        .add("userId", UUID.randomUUID().toString())
                        .add("profileId", "")
                        .add("advertiserId", "")
                        .build();

        // createAuthentication should detect expiration and return null
        UsernamePasswordAuthenticationToken auth = jwtManager.createAuthentication(expiredClaims);
        assertNull(auth, "Authentication should be null for expired claims");
    }

    @Test
    @DisplayName("isValidToken returns false when expiration is null")
    void isValidAccessTokenFalseWhenExpirationNull() {
        Claims claims =
                Jwts.claims()
                        .subject("validUser")
                        .add("userId", UUID.randomUUID().toString())
                        .add("authorities", "ROLE_TEST")
                        .build();

        assertFalse(
                jwtManager.isValidAccessToken(claims), "Should return false if expiration is null");
    }

    @Test
    @DisplayName("isValidToken returns false when expiration is in the past")
    void isValidAccessTokenFalseWhenExpirationInPast() {
        Claims claims =
                Jwts.claims()
                        .subject("validUser")
                        .expiration(new Date(System.currentTimeMillis() - 10_000)) // expired
                        .add("userId", UUID.randomUUID().toString())
                        .add("authorities", "ROLE_TEST")
                        .build();

        assertFalse(
                jwtManager.isValidAccessToken(claims),
                "Should return false if the expiration is in the past");
    }

    @Test
    @DisplayName("isValidToken returns false when subject (username) is blank or null")
    void isValidAccessTokenFalseWhenSubjectBlankOrNull() {
        // Case: subject null
        Claims noSub =
                Jwts.claims()
                        .expiration(new Date(System.currentTimeMillis() + 10_000))
                        .add("userId", UUID.randomUUID().toString())
                        .add("authorities", "ROLE_TEST")
                        .build();

        assertFalse(
                jwtManager.isValidAccessToken(noSub),
                "Must return false if the subject (username) is null");

        // Case: subject blank
        Claims blankSub =
                Jwts.claims()
                        .subject("   ")
                        .expiration(new Date(System.currentTimeMillis() + 10_000))
                        .add("userId", UUID.randomUUID().toString())
                        .add("authorities", "ROLE_TEST")
                        .build();

        assertFalse(
                jwtManager.isValidAccessToken(blankSub),
                "Should return false if the subject (username) is blank");
    }

    @Test
    @DisplayName("isValidToken returns false when userId claim is blank or null")
    void isValidAccessTokenFalseWhenUserIdBlankOrNull() {
        // userId null
        Claims noUserId =
                Jwts.claims()
                        .subject("someUser")
                        .expiration(new Date(System.currentTimeMillis() + 10_000))
                        .add("userId", null)
                        .add("authorities", "ROLE_TEST")
                        .build();
        assertFalse(jwtManager.isValidAccessToken(noUserId), "Must return false if userId is null");

        // userId is blank
        Claims blankUserId =
                Jwts.claims()
                        .subject("someUser")
                        .expiration(new Date(System.currentTimeMillis() + 10_000))
                        .add("userId", "   ")
                        .add("authorities", "ROLE_TEST")
                        .build();

        assertFalse(
                jwtManager.isValidAccessToken(blankUserId),
                "Should return false if userId is blank");
    }

    @Test
    @DisplayName("isValidToken returns false when authorities claim is blank or null")
    void isValidAccessTokenFalseWhenAuthoritiesBlankOrNull() {
        // authorities null
        Claims noAuth =
                Jwts.claims()
                        .subject("someUser")
                        .expiration(new Date(System.currentTimeMillis() + 10_000))
                        .add("userId", UUID.randomUUID().toString())
                        .add("authorities", null)
                        .build();

        assertFalse(
                jwtManager.isValidAccessToken(noAuth),
                "Should return false if authorities is null");

        // authorities are blank
        Claims blankAuth =
                Jwts.claims()
                        .subject("someUser")
                        .expiration(new Date(System.currentTimeMillis() + 10_000))
                        .add("userId", UUID.randomUUID().toString())
                        .add("authorities", "   ")
                        .build();

        assertFalse(
                jwtManager.isValidAccessToken(blankAuth),
                "Should return false if authorities is blank");
    }

    @Test
    @DisplayName(
            "isValidToken returns true when all required fields are set and expiration is in the"
                    + " future")
    void isValidTokenTrueForValidAccessClaims() {
        Claims valid =
                Jwts.claims()
                        .subject("goodUser")
                        .expiration(new Date(System.currentTimeMillis() + 10_000)) // future
                        .add("userId", UUID.randomUUID().toString())
                        .add("authorities", "ROLE_ONE,ROLE_TWO")
                        .build();

        assertTrue(
                jwtManager.isValidAccessToken(valid),
                "Should return true if expiration > now, subject, userId and authorities are"
                        + " correctly set");
    }
}
