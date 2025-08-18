package com.alpaca.unit.security.manager;

import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.security.manager.JJwtManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for {@link JJwtManager} */
@DisplayName("JJwtManager Unit Tests")
class JJwtManagerTest {

    private static final String ISSUER = "testIssuer";
    private static final long EXPIRATION_MILLIS = 2_000; // 2s
    private JJwtManager jwtManager;

    @BeforeEach
    void setUp() throws Exception {
        // Generate an RSA key pair for signing/verifying
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        String base64PrivateKey = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        String base64PublicKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());

        // Instantiate JJwtManager with our generated keys, issuer, and expiration
        jwtManager =
                new JJwtManager(
                        base64PrivateKey,
                        base64PublicKey,
                        ISSUER,
                        String.valueOf(EXPIRATION_MILLIS));
    }

    @Test
    @DisplayName("createToken should produce a valid JWT containing the expected claims")
    void createTokenProducesValidJwt() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID advertiserId = UUID.randomUUID();
        String username = "testUser";
        List<GrantedAuthority> authorities =
                AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN");

        UserPrincipal principal =
                new UserPrincipal(
                        userId, profileId, advertiserId, username, null, authorities, null);

        String token = jwtManager.createToken(principal);
        assertNotNull(token, "Token should not be null or empty");
        assertFalse(token.isBlank(), "Token should not be blank");

        // Parse the token directly to inspect claims
        Claims claims = jwtManager.validateToken(token).getPayload();

        assertEquals(ISSUER, claims.getIssuer(), "Issuer claim must match");
        assertEquals(username, claims.getSubject(), "Subject must be the username");
        assertEquals(
                userId.toString(), claims.get("userId", String.class), "userId claim must match");
        assertEquals(
                profileId.toString(),
                claims.get("profileId", String.class),
                "profileId claim must match");
        assertEquals(
                advertiserId.toString(),
                claims.get("advertiserId", String.class),
                "advertiserId claim must match");

        // The authorities claim is a comma-separated string
        String authClaim = claims.get("authorities", String.class);
        assertNotNull(authClaim);
        assertTrue(authClaim.contains("ROLE_USER"));
        assertTrue(authClaim.contains("ROLE_ADMIN"));

        Date now = new Date();
        assertTrue(
                claims.getIssuedAt().before(new Date(now.getTime() + 100)),
                "IssuedAt should be very close to now");
        assertTrue(
                claims.getNotBefore().before(new Date(now.getTime() + 100)),
                "NotBefore should be very close to now");
        assertTrue(claims.getExpiration().after(now), "Expiration must be in the future");
    }

    @Test
    @DisplayName("validateToken should throw UnauthorizedException on invalid token")
    void validateTokenThrowsOnInvalidToken() {
        String badToken = "this.is.not.a.valid.jwt";
        assertThrows(UnauthorizedException.class, () -> jwtManager.validateToken(badToken));
    }

    @Test
    @DisplayName("manageAuthentication should return a valid Authentication when token is valid")
    void manageAuthenticationReturnsValidAuthentication() {
        UUID userId = UUID.randomUUID();
        UUID profileId = null;
        UUID advertiserId = null;
        String username = "anotherUser";
        List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("ROLE_X", "ROLE_Y");

        // allow advertiserId to be null in this test
        UserPrincipal principal =
                new UserPrincipal(
                        userId, profileId, advertiserId, username, null, authorities, null);

        String token = jwtManager.createToken(principal);
        UsernamePasswordAuthenticationToken auth = jwtManager.manageAuthentication(token);

        assertNotNull(auth, "Authentication token should not be null");
        assertTrue(
                auth.isAuthenticated(),
                "Authentication should be marked as authenticated by default");

        // The principal inside Authentication should be a UserPrincipal with same values
        Object authPrincipal = auth.getPrincipal();
        assertInstanceOf(UserPrincipal.class, authPrincipal, "Principal should be a UserPrincipal");
        UserPrincipal up = (UserPrincipal) authPrincipal;

        assertEquals(userId, up.getId(), "UserPrincipal.id must match");
        assertNull(up.getProfileId(), "UserPrincipal.profileId should be null");
        assertNull(up.getAdvertiserId(), "UserPrincipal.advertiserId should be null");
        assertEquals(username, up.getUsername(), "UserPrincipal.username must match");

        // Authorities in the Authentication should match those in UserPrincipal
        Collection<? extends GrantedAuthority> authList = auth.getAuthorities();
        assertEquals(2, authList.size(), "Should have two authorities");
        assertTrue(authList.stream().anyMatch(a -> a.getAuthority().equals("ROLE_X")));
        assertTrue(authList.stream().anyMatch(a -> a.getAuthority().equals("ROLE_Y")));
    }

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
    @DisplayName("authoritiesToString should join authority names with commas")
    void authoritiesToStringJoinsCorrectly() {
        List<GrantedAuthority> list = AuthorityUtils.createAuthorityList("A", "B", "C");
        String joined = jwtManager.authoritiesToString(list);
        assertEquals("A,B,C", joined);
    }

    @Test
    @DisplayName("existString returns false for null or blank, true otherwise")
    void existStringBehaviour() {
        assertFalse(jwtManager.existString(null), "null should return false");
        assertFalse(jwtManager.existString(""), "empty string should return false");
        assertFalse(jwtManager.existString("   "), "blank string should return false");
        assertTrue(jwtManager.existString("hello"), "non-empty non-blank should return true");
    }

    @Test
    @DisplayName("getSpecificClaim retrieves the correct value from Claims")
    void getSpecificClaimRetrievesCorrectly() {
        Claims claims = Jwts.claims().add("foo", 12345).build();
        Integer val = jwtManager.getSpecificClaim(claims, "foo", Integer.class);
        assertEquals(12345, val);
    }

    @Test
    @DisplayName("isValidToken returns false when expiration is null")
    void isValidTokenFalseWhenExpirationNull() {
        Claims claims =
                Jwts.claims()
                        .subject("validUser")
                        .add("userId", UUID.randomUUID().toString())
                        .add("authorities", "ROLE_TEST")
                        .build();

        assertFalse(jwtManager.isValidToken(claims), "Should return false if expiration is null");
    }

    @Test
    @DisplayName("isValidToken returns false when expiration is in the past")
    void isValidTokenFalseWhenExpirationInPast() {
        Claims claims =
                Jwts.claims()
                        .subject("validUser")
                        .expiration(new Date(System.currentTimeMillis() - 10_000)) // expired
                        .add("userId", UUID.randomUUID().toString())
                        .add("authorities", "ROLE_TEST")
                        .build();

        assertFalse(
                jwtManager.isValidToken(claims),
                "Should return false if the expiration is in the past");
    }

    @Test
    @DisplayName("isValidToken returns false when subject (username) is blank or null")
    void isValidTokenFalseWhenSubjectBlankOrNull() {
        // Case: subject null
        Claims noSub =
                Jwts.claims()
                        .expiration(new Date(System.currentTimeMillis() + 10_000))
                        .add("userId", UUID.randomUUID().toString())
                        .add("authorities", "ROLE_TEST")
                        .build();

        assertFalse(
                jwtManager.isValidToken(noSub),
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
                jwtManager.isValidToken(blankSub),
                "Should return false if the subject (username) is blank");
    }

    @Test
    @DisplayName("isValidToken returns false when userId claim is blank or null")
    void isValidTokenFalseWhenUserIdBlankOrNull() {
        // userId null
        Claims noUserId =
                Jwts.claims()
                        .subject("someUser")
                        .expiration(new Date(System.currentTimeMillis() + 10_000))
                        .add("userId", null)
                        .add("authorities", "ROLE_TEST")
                        .build();
        assertFalse(jwtManager.isValidToken(noUserId), "Must return false if userId is null");

        // userId is blank
        Claims blankUserId =
                Jwts.claims()
                        .subject("someUser")
                        .expiration(new Date(System.currentTimeMillis() + 10_000))
                        .add("userId", "   ")
                        .add("authorities", "ROLE_TEST")
                        .build();

        assertFalse(jwtManager.isValidToken(blankUserId), "Should return false if userId is blank");
    }

    @Test
    @DisplayName("isValidToken returns false when authorities claim is blank or null")
    void isValidTokenFalseWhenAuthoritiesBlankOrNull() {
        // authorities null
        Claims noAuth =
                Jwts.claims()
                        .subject("someUser")
                        .expiration(new Date(System.currentTimeMillis() + 10_000))
                        .add("userId", UUID.randomUUID().toString())
                        .add("authorities", null)
                        .build();

        assertFalse(jwtManager.isValidToken(noAuth), "Should return false if authorities is null");

        // authorities are blank
        Claims blankAuth =
                Jwts.claims()
                        .subject("someUser")
                        .expiration(new Date(System.currentTimeMillis() + 10_000))
                        .add("userId", UUID.randomUUID().toString())
                        .add("authorities", "   ")
                        .build();

        assertFalse(
                jwtManager.isValidToken(blankAuth), "Should return false if authorities is blank");
    }

    @Test
    @DisplayName(
            "isValidToken returns true when all required fields are set and expiration is in the"
                    + " future")
    void isValidTokenTrueForValidClaims() {
        Claims valid =
                Jwts.claims()
                        .subject("goodUser")
                        .expiration(new Date(System.currentTimeMillis() + 10_000)) // futuro
                        .add("userId", UUID.randomUUID().toString())
                        .add("authorities", "ROLE_ONE,ROLE_TWO")
                        .build();

        assertTrue(
                jwtManager.isValidToken(valid),
                "Should return true if expiration > now, subject, userId and authorities are"
                        + " correctly set");
    }
}
