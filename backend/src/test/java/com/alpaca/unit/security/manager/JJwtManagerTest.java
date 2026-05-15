package com.alpaca.unit.security.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alpaca.entity.RefreshToken;
import com.alpaca.entity.User;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.security.manager.JJwtManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

@DisplayName("JJwtManager Unit Tests")
class JJwtManagerTest {

    private JJwtManager jwtManager;
    private final String issuer = "alpaca-auth-service";

    @BeforeEach
    void setUp() throws Exception {
        String expAccess = "3600000";
        String expRefresh = "86400000";
        jwtManager =
                new JJwtManager(
                        new ClassPathResource("keys/access_private.pem"),
                        new ClassPathResource("keys/access_public.pem"),
                        expAccess,
                        new ClassPathResource("keys/refresh_private.pem"),
                        new ClassPathResource("keys/refresh_public.pem"),
                        expRefresh,
                        issuer);
    }

    @Test
    @DisplayName("createAccessToken: Should produce a valid verifiable JWT for a UserPrincipal")
    void createAccessToken_ShouldBeVerifiable() {
        UserPrincipal principal = mock(UserPrincipal.class);
        UUID userId = UUID.randomUUID();
        when(principal.getUsername()).thenReturn("rogelio.olarte");
        when(principal.getUserId()).thenReturn(userId);
        when(principal.getAuthorities())
                .thenReturn(AuthorityUtils.createAuthorityList("ROLE_DEVELOPER"));
        when(principal.getProfileId()).thenReturn(UUID.randomUUID());

        String token = jwtManager.createAccessToken(principal, Instant.now());
        Claims claims = jwtManager.validateAccessToken(token);

        assertEquals(issuer, claims.getIssuer());
        assertEquals(userId.toString(), claims.get("userId"));
        assertTrue(jwtManager.isValidAccessToken(claims));
    }

    @Test
    @DisplayName(
            "createRefreshToken: Should produce a verifiable JWT with family and client details")
    void createRefreshToken_ShouldBeVerifiable() {
        RefreshToken entity = mock(RefreshToken.class);
        User user = mock(User.class);
        UUID userId = UUID.randomUUID();
        UUID jti = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        String clientId = "web-client-01";

        when(entity.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(userId);
        when(entity.getTokenJti()).thenReturn(jti);
        when(entity.getFamilyId()).thenReturn(familyId);
        when(entity.getClientId()).thenReturn(clientId);
        when(entity.getLastUsedAt()).thenReturn(Instant.now());
        when(entity.getExpiresAt()).thenReturn(Instant.now().plus(1, ChronoUnit.DAYS));

        String token = jwtManager.createRefreshToken(entity);
        Claims claims = jwtManager.validateRefreshToken(token);

        assertEquals(jti.toString(), claims.get("jti"));
        assertEquals(familyId.toString(), claims.get("familyId"));
        assertEquals(clientId, claims.get("clientId"));
        assertTrue(jwtManager.isValidRefreshToken(claims));
    }

    @Test
    @DisplayName("createTokenHash: Should return Base64URL encoded SHA-256 hash")
    void createTokenHash_ShouldReturnValidHash() {
        String input = "refresh-token-data";
        String hash = jwtManager.createTokenHash(input);

        assertNotNull(hash);
        assertFalse(hash.contains("=")); // Base64URL without padding check
        assertThrows(IllegalArgumentException.class, () -> jwtManager.createTokenHash(""));
    }

    @Test
    @DisplayName(
            "validateAccessToken: Should throw UnauthorizedException for malformed or invalid"
                    + " tokens")
    void validateAccessToken_ShouldThrowOnInvalid() {
        assertThrows(
                UnauthorizedException.class,
                () -> jwtManager.validateAccessToken("invalid.token.structure"));
        assertThrows(UnauthorizedException.class, () -> jwtManager.validateAccessToken(null));
    }

    @Test
    @DisplayName("isValidAccessToken: Should return false for missing or blank critical claims")
    void isValidAccessToken_ShouldHandleEdgeCases() {
        Claims claims =
                Jwts.claims()
                        .subject("") // Blank subject
                        .expiration(new Date(System.currentTimeMillis() + 10000))
                        .add("userId", UUID.randomUUID().toString())
                        .add("authorities", "ROLE_USER")
                        .build();

        assertFalse(jwtManager.isValidAccessToken(claims));

        Claims expiredClaims =
                Jwts.claims()
                        .subject("user")
                        .expiration(new Date(System.currentTimeMillis() - 10000))
                        .build();

        assertFalse(jwtManager.isValidAccessToken(expiredClaims));
    }

    @Test
    @DisplayName("isValidRefreshToken: Should validate presence of familyId and clientId")
    void isValidRefreshToken_ShouldValidateSpecificClaims() {
        Claims incompleteClaims =
                Jwts.claims()
                        .subject("user")
                        .expiration(new Date(System.currentTimeMillis() + 10000))
                        .add("jti", UUID.randomUUID().toString())
                        // Missing familyId and clientId
                        .build();

        assertFalse(jwtManager.isValidRefreshToken(incompleteClaims));
    }

    @Test
    @DisplayName("manageAuthentication: Should return valid Spring Security token from JWT string")
    void manageAuthentication_ShouldReturnAuthToken() {
        UserPrincipal principal = mock(UserPrincipal.class);
        when(principal.getUsername()).thenReturn("tester");
        when(principal.getUserId()).thenReturn(UUID.randomUUID());
        when(principal.getAuthorities())
                .thenReturn(AuthorityUtils.createAuthorityList("ROLE_USER"));

        String token = jwtManager.createAccessToken(principal, Instant.now());
        UsernamePasswordAuthenticationToken auth = jwtManager.manageAuthentication(token);

        assertNotNull(auth);
        assertNotNull(auth.getPrincipal());
        assertEquals("tester", ((UserPrincipal) auth.getPrincipal()).getUsername());
    }

    @Test
    @DisplayName("createAuthentication: Should return null if claims are invalid")
    void createAuthentication_ShouldReturnNullOnInvalidClaims() {
        Claims invalidClaims = Jwts.claims().subject("tester").build();
        assertNull(jwtManager.createAuthentication(invalidClaims));
    }

    @Test
    @DisplayName("Token Logic: Should handle null optional IDs in claims")
    void createAccessToken_ShouldHandleNullOptionals() {
        UserPrincipal principal = mock(UserPrincipal.class);
        when(principal.getUsername()).thenReturn("no-profile-user");
        when(principal.getUserId()).thenReturn(UUID.randomUUID());
        when(principal.getProfileId()).thenReturn(null);
        when(principal.getAdvertiserId()).thenReturn(null);
        when(principal.getAuthorities()).thenReturn(Collections.emptyList());

        String token = jwtManager.createAccessToken(principal, Instant.now());
        Claims claims = jwtManager.validateAccessToken(token);

        assertEquals("", claims.get("profileId"));
        assertEquals("", claims.get("advertiserId"));
    }
}
