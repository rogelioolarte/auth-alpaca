package com.alpaca.security.manager;

import com.alpaca.entity.RefreshToken;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.SignatureAlgorithm;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Central manager for issuing, validating, and parsing JSON Web Tokens (JWT) using RSA keys.
 *
 * <p>This Spring-managed component handles both Access and Refresh tokens. Tokens are signed
 * with RSA private keys and verified with RSA public keys using the RS512 signature algorithm
 * (RSA with SHA-512). Claims such as issuer, subject, authorities, and custom fields are
 * automatically set when tokens are created.
 *
 * <p>The class maintains separate keys and expiration times for Access and Refresh tokens, which
 * must be provided via Spring configuration properties. It builds {@code JwtParser} instances
 * for validating incoming tokens, ensuring that only tokens with the correct issuer and
 * signature are accepted.
 *
 * <p>Typical usage includes creating tokens upon successful authentication, hashing refresh
 * tokens for persistence, validating incoming tokens, and building Spring Security
 * authentication objects from valid JWTs.
 *
 * @see io.jsonwebtoken.Jwts
 * @see io.jsonwebtoken.JwtParser
 * @see SignatureAlgorithm
 * @see UserPrincipal
 * @see UnauthorizedException
 */
@Component
public class JJwtManager {

    /** The signature algorithm used for token signing: RS512 (RSA with SHA-512). */
    private static final SignatureAlgorithm alg = Jwts.SIG.RS512;

    /** RSA private key used to sign Access Tokens. */
    private final RSAPrivateKey privateKeyAccess;

    /** RSA public key used to verify Access Tokens. */
    private final RSAPublicKey publicKeyAccess;

    /** Expiration time in milliseconds for Access Tokens. */
    private final Long jwtTimeExpAccess;

    /** RSA private key used to sign Refresh Tokens. */
    private final RSAPrivateKey privateKeyRefresh;

    /** RSA public key used to verify Refresh Tokens. */
    private final RSAPublicKey publicKeyRefresh;

    /** Expiration time in milliseconds for Refresh Tokens. */
    private final Long jwtTimeExpRefresh;

    /** Issuer identifier to include in the “iss” claim of tokens. */
    private final String jwtIssuer;

    private static final String CLAIM_KEY_USER_ID= "userId";
    private static final String CLAIM_KEY_AUTHORITIES = "authorities";

    private final JwtParser jwtAccessParser;
    private final JwtParser jwtRefreshParser;

    /**
     * Constructs a new {@code JJwtManager}, loading RSA key pairs for access and refresh tokens,
     * and configuring the issuer and expiration settings.
     *
     * <p>Both private and public keys must be provided in Base64-encoded format via Spring
     * {@code Resource} locations. Keys must follow standard PKCS8 (private) or X.509 (public)
     * PEM formatting.
     *
     * @param accessPrivateKR resource representing the access token RSA private key
     * @param accessPublicKR resource representing the access token RSA public key
     * @param jwtTimeExpAccess string value representing access token expiration in ms
     * @param refreshPrivateKR resource representing the refresh token RSA private key
     * @param refreshPublicKR resource representing the refresh token RSA public key
     * @param jwtTimeExpRefresh string value representing refresh token expiration in ms
     * @param jwtIssuer token issuer identifier to be included in all generated tokens
     * @throws NoSuchAlgorithmException if RSA key instance cannot be created
     * @throws IOException if any of the supplied resources cannot be read
     * @throws InvalidKeySpecException if any key spec cannot be parsed into a key
     */
    public JJwtManager(
            @Value("${security.jwt.access.private-key-path}") Resource accessPrivateKR,
            @Value("${security.jwt.access.public-key-path}") Resource accessPublicKR,
            @Value("${security.jwt.access.expiration}") @NotNull String jwtTimeExpAccess,
            @Value("${security.jwt.refresh.private-key-path}") Resource refreshPrivateKR,
            @Value("${security.jwt.refresh.public-key-path}") Resource refreshPublicKR,
            @Value("${security.jwt.refresh.expiration}") @NotNull String jwtTimeExpRefresh,
            @Value("${security.jwt.issuer}") @NotNull String jwtIssuer)
            throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        this.privateKeyAccess = createPrivateKey(accessPrivateKR, keyFactory);
        this.publicKeyAccess = createPublicKey(accessPublicKR, keyFactory);

        this.privateKeyRefresh = createPrivateKey(refreshPrivateKR, keyFactory);
        this.publicKeyRefresh = createPublicKey(refreshPublicKR, keyFactory);

        this.jwtIssuer = jwtIssuer;
        this.jwtTimeExpAccess = Long.parseLong(jwtTimeExpAccess);
        this.jwtTimeExpRefresh = Long.parseLong(jwtTimeExpRefresh);

        // Build parsers that require valid issuer and key verification
        this.jwtAccessParser = Jwts.parser()
                .verifyWith(publicKeyAccess)
                .requireIssuer(jwtIssuer)
                .build();

        this.jwtRefreshParser = Jwts.parser()
                .verifyWith(publicKeyRefresh)
                .requireIssuer(jwtIssuer)
                .build();
    }

    /**
     * Generates a signed JWT Access token for the given authenticated user.
     *
     * @param user authenticated user information
     * @return a signed Access JWT
     */
    public String createAccessToken(UserPrincipal user) {
        return Jwts.builder()
                .issuer(jwtIssuer)
                .subject(user.getUsername())
                .claim(CLAIM_KEY_AUTHORITIES,
                        StringUtils.collectionToCommaDelimitedString(user.getAuthorities()))
                .claim(CLAIM_KEY_USER_ID, user.getId().toString())
                .claim("profileId",
                        user.getProfileId() != null ? user.getProfileId().toString() : "")
                .claim("advertiserId",
                        user.getAdvertiserId() != null ? user.getAdvertiserId().toString() : "")
                .issuedAt(new Date())
                .notBefore(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtTimeExpAccess))
                .signWith(privateKeyAccess, alg)
                .compact();
    }

    /**
     * Creates a signed Refresh token embedding refresh token metadata.
     *
     * @param refreshToken refresh token entity
     * @param user authenticated user information
     * @return a signed Refresh JWT
     */
    public String createRefreshToken(RefreshToken refreshToken,
                                     UserPrincipal user) {
        return Jwts.builder()
                .issuer(jwtIssuer)
                .subject(refreshToken.getUser().getEmail())
                .claim(CLAIM_KEY_USER_ID, user.getId())
                .claim("jti", refreshToken.getTokenJti())
                .claim("familyId", refreshToken.getFamilyId())
                .issuedAt(new Date())
                .notBefore(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtTimeExpRefresh))
                .claim("typ", "refresh")
                .claim("clientId", refreshToken.getClientId())
                .signWith(privateKeyRefresh, alg)
                .compact();
    }

    /**
     * Computes the SHA-256 hash of a refresh token string.
     *
     * <p>Useful for storing refresh tokens securely in a database without storing raw tokens.
     *
     * @param refreshToken the raw refresh token
     * @return a hex-encoded SHA-256 digest, or empty string if algorithm unavailable
     */
    public String createRefreshTokenHash(String refreshToken) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            StringBuilder hexString = new StringBuilder();
            for (byte b : md.digest(refreshToken.getBytes(StandardCharsets.UTF_8))) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException _) {
            return "";
        }
    }

    /**
     * Validates the given Access token and returns its claims if valid.
     *
     * @param token the signed JWT to validate
     * @return claims extracted from the valid token
     * @throws UnauthorizedException if token validation fails
     */
    public Claims validateAccessToken(String token) {
        try {
            return jwtAccessParser.parseSignedClaims(token).getPayload();
        } catch (Exception _) {
            throw new UnauthorizedException("Token Invalid, Unauthorized");
        }
    }

    /**
     * Validates the given Refresh token and returns its claims if valid.
     *
     * @param token the signed JWT to validate
     * @return claims extracted from the valid token
     * @throws UnauthorizedException if token validation fails
     */
    public Claims validateRefreshToken(String token) {
        try {
            return jwtRefreshParser.parseSignedClaims(token).getPayload();
        } catch (Exception _) {
            throw new UnauthorizedException("Token Invalid, Unauthorized");
        }
    }

    /**
     * Checks essential conditions to determine if the token represents a valid refresh token.
     *
     * @param claims the parsed JWT claims
     * @return {@code true} if token appears valid, otherwise {@code false}
     */
    public boolean isValidAccessToken(Claims claims) {
        return claims.getExpiration() != null
                && claims.getExpiration().after(new Date())
                && StringUtils.hasText(claims.getSubject())
                && StringUtils.hasText(claims.get(CLAIM_KEY_USER_ID, String.class))
                && StringUtils.hasText(claims.get("jti", String.class))
                && StringUtils.hasText(claims.get("familyId", String.class))
                && StringUtils.hasText(claims.get("clientId", String.class));
    }

    /**
     * Checks essential conditions to determine if the token represents a valid access token.
     *
     * @param claims the parsed JWT claims
     * @return {@code true} if token appears valid, otherwise {@code false}
     */
    public boolean isValidRefreshToken(Claims claims) {
        return claims.getExpiration() != null
                && claims.getExpiration().after(new Date())
                && StringUtils.hasText(claims.getSubject())
                && StringUtils.hasText(claims.get(CLAIM_KEY_USER_ID, String.class))
                && StringUtils.hasText(claims.get(CLAIM_KEY_AUTHORITIES, String.class));
    }

    /**
     * Constructs Spring Security authentication from a validated JWT.
     *
     * @param token the signed JWT
     * @return an authentication token suitable for Spring Security context
     */
    public UsernamePasswordAuthenticationToken manageAuthentication(String token) {
        return createAuthentication(validateAccessToken(token));
    }

    /**
     * Builds a {@code UsernamePasswordAuthenticationToken} if claims represent a valid access token.
     *
     * @param claims parsed JWT claims
     * @return authentication object or {@code null} if claims are invalid
     */
    public UsernamePasswordAuthenticationToken createAuthentication(Claims claims) {
        return isValidAccessToken(claims)
                ? new UsernamePasswordAuthenticationToken(
                new UserPrincipal(claims), null,
                AuthorityUtils.commaSeparatedStringToAuthorityList(
                        claims.get(CLAIM_KEY_AUTHORITIES, String.class)))
                : null;
    }

    /** Internal helper to generate a private key from a PEM resource. */
    private RSAPrivateKey createPrivateKey(Resource keyResource, KeyFactory keyfactory)
            throws IOException, InvalidKeySpecException {
        return (RSAPrivateKey)
                keyfactory.generatePrivate(
                        new PKCS8EncodedKeySpec(Decoders.BASE64.decode(readPEM(keyResource))));
    }

    /** Internal helper to generate a public key from a PEM resource. */
    private RSAPublicKey createPublicKey(Resource keyResource, KeyFactory keyfactory)
            throws IOException, InvalidKeySpecException {
        return (RSAPublicKey)
                keyfactory.generatePublic(
                        new X509EncodedKeySpec(Decoders.BASE64.decode(readPEM(keyResource))));
    }

    /** Reads a PEM file and strips header/footer and whitespace to return Base64 content. */
    private String readPEM(Resource resource) throws IOException {
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("-{5}(BEGIN|END).+?-{5}", "")
                    .replaceAll("\\s+", "");
        }
    }
}