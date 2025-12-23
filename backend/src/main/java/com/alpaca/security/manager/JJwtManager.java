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
import java.util.Base64;
import java.util.Date;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Central manager for issuing, validating, and parsing JSON Web Tokens (JWT) using RSA keys.
 *
 * <p>This Spring component handles both Access and Refresh tokens. Tokens are signed with RSA
 * private keys and verified using RSA public keys and the RS512 signature algorithm. It builds
 * {@code JwtParser} instances to ensure tokens are validated with the correct issuer and signature.
 *
 * <p>Typical use cases include creating tokens upon authentication, hashing refresh tokens for
 * storage, verifying incoming tokens, and creating Spring Security authentication objects from
 * valid JWTs.
 *
 * @see io.jsonwebtoken.Jwts
 * @see io.jsonwebtoken.JwtParser
 * @see SignatureAlgorithm
 * @see UserPrincipal
 * @see UnauthorizedException
 */
@Component
public class JJwtManager {

    /** The signature algorithm used for token signing (RSA with SHA-512). */
    private static final SignatureAlgorithm SIGNATURE_ALGORITHM = Jwts.SIG.RS512;

    /** RSA private key used to sign Access Tokens. */
    private final RSAPrivateKey privateKeyAccess;

    /** Expiration time in milliseconds configured for Access Tokens. */
    private final Long jwtTimeExpAccess;

    /** RSA private key used to sign Refresh Tokens. */
    private final RSAPrivateKey privateKeyRefresh;

    /** Expiration time in milliseconds configured for Refresh Tokens. */
    @Getter private final Long jwtTimeExpRefresh;

    /** Issuer identifier included in the "iss" claim of tokens. */
    private final String jwtIssuer;

    /** Claim Keys for JWT Access Token */
    private static final String CLAIM_KEY_USER_ID = "userId";

    private static final String CLAIM_KEY_AUTHORITIES = "authorities";

    /**
     * Parser configured to validate and parse signed Access Tokens.
     *
     * <p>This parser enforces the expected issuer and uses the configured Access public key to
     * verify the token signature. It is used internally by {@link #validateAccessToken(String)} and
     * related authentication methods.
     */
    private final JwtParser jwtAccessParser;

    /**
     * Parser configured to validate and parse signed Refresh Tokens.
     *
     * <p>This parser enforces the expected issuer and uses the configured Refresh public key to
     * verify the token signature. It is used by {@link #validateRefreshToken(String)} to extract
     * claims from valid refresh tokens.
     */
    private final JwtParser jwtRefreshParser;

    /**
     * Constructs a new {@code JJwtManager}, loading RSA key pairs and configuring expiration and
     * issuer information from Spring properties.
     *
     * <p>Keys must be provided as Base64-encoded PEM resources. Access and Refresh token parsers
     * are built to verify signature and issuer.
     *
     * @param accessPrivateKR resource for the Access Token RSA private key
     * @param accessPublicKR resource for the Access Token RSA public key
     * @param jwtTimeExpAccess expiration duration in ms for Access Tokens
     * @param refreshPrivateKR resource for the Refresh Token RSA private key
     * @param refreshPublicKR resource for the Refresh Token RSA public key
     * @param jwtTimeExpRefresh expiration duration in ms for Refresh Tokens
     * @param jwtIssuer the token issuer identifier applied to both token types
     * @throws NoSuchAlgorithmException if RSA algorithm is not supported
     * @throws IOException if any key resource cannot be read
     * @throws InvalidKeySpecException if key spec cannot be converted into a key
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
        RSAPublicKey publicKeyAccess = createPublicKey(accessPublicKR, keyFactory);

        this.privateKeyRefresh = createPrivateKey(refreshPrivateKR, keyFactory);
        RSAPublicKey publicKeyRefresh = createPublicKey(refreshPublicKR, keyFactory);

        this.jwtIssuer = jwtIssuer;
        this.jwtTimeExpAccess = Long.parseLong(jwtTimeExpAccess);
        this.jwtTimeExpRefresh = Long.parseLong(jwtTimeExpRefresh);

        // Build parsers that require valid issuer and key verification
        this.jwtAccessParser =
                Jwts.parser().verifyWith(publicKeyAccess).requireIssuer(jwtIssuer).build();

        this.jwtRefreshParser =
                Jwts.parser().verifyWith(publicKeyRefresh).requireIssuer(jwtIssuer).build();
    }

    /**
     * Generates a signed JWT Access token for the specified authenticated user.
     *
     * @param user authenticated user details
     * @return JWT string representing the signed Access Token
     */
    public String createAccessToken(UserPrincipal user) {
        return Jwts.builder()
                .issuer(jwtIssuer)
                .subject(user.getUsername())
                .claim(
                        CLAIM_KEY_AUTHORITIES,
                        StringUtils.collectionToCommaDelimitedString(user.getAuthorities()))
                .claim(CLAIM_KEY_USER_ID, user.getId().toString())
                .claim(
                        "profileId",
                        user.getProfileId() != null ? user.getProfileId().toString() : "")
                .claim(
                        "advertiserId",
                        user.getAdvertiserId() != null ? user.getAdvertiserId().toString() : "")
                .issuedAt(new Date())
                .notBefore(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtTimeExpAccess))
                .signWith(privateKeyAccess, SIGNATURE_ALGORITHM)
                .compact();
    }

    /**
     * Creates a signed Refresh JWT including refresh token metadata.
     *
     * @param refreshToken entity containing family and JTI identifiers
     * @return JWT string representing the signed Refresh Token
     */
    public String createRefreshToken(RefreshToken refreshToken) {
        return Jwts.builder()
                .issuer(jwtIssuer)
                .subject(refreshToken.getUser().getId().toString())
                .claim(CLAIM_KEY_USER_ID, refreshToken.getUser().getId())
                .claim("jti", refreshToken.getTokenJti())
                .claim("familyId", refreshToken.getFamilyId())
                .issuedAt(Date.from(refreshToken.getCreatedAt()))
                .notBefore(Date.from(refreshToken.getCreatedAt()))
                .expiration(Date.from(refreshToken.getExpiresAt()))
                .claim("clientId", refreshToken.getClientId())
                .signWith(privateKeyRefresh, SIGNATURE_ALGORITHM)
                .compact();
    }

    /**
     * Computes the SHA-256 hash of a refresh token string.
     *
     * <p>Useful for safely storing refresh tokens in a database without retaining the raw token.
     *
     * @param refreshToken the raw refresh token
     * @return hex-encoded SHA-256 hash
     * @throws IllegalArgumentException if the token string is empty
     * @throws IllegalStateException if SHA-256 algorithm is unavailable
     */
    public String createRefreshTokenHash(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new IllegalArgumentException("refreshToken must not be empty");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(md.digest(refreshToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Validates the given Access token and returns its JWT claims.
     *
     * @param token signed JWT
     * @return parsed claims if token is valid
     * @throws UnauthorizedException when verification fails
     */
    public Claims validateAccessToken(String token) {
        try {
            return jwtAccessParser.parseSignedClaims(token).getPayload();
        } catch (Exception _) {
            throw new UnauthorizedException("Token Invalid, Unauthorized");
        }
    }

    /**
     * Validates the given Refresh token and returns its JWT claims.
     *
     * @param token signed JWT
     * @return parsed claims if token is valid
     * @throws UnauthorizedException when verification fails
     */
    public Claims validateRefreshToken(String token) {
        try {
            return jwtRefreshParser.parseSignedClaims(token).getPayload();
        } catch (Exception _) {
            throw new UnauthorizedException("Token Invalid, Unauthorized");
        }
    }

    /**
     * Determines if Access token claims represent a valid access token.
     *
     * @param claims parsed JWT claims
     * @return true if basic validity checks pass
     */
    public boolean isValidAccessToken(Claims claims) {
        return claims.getExpiration() != null
                && claims.getExpiration().after(new Date())
                && StringUtils.hasText(claims.getSubject())
                && StringUtils.hasText(claims.get(CLAIM_KEY_USER_ID, String.class))
                && StringUtils.hasText(claims.get(CLAIM_KEY_AUTHORITIES, String.class));
    }

    /**
     * Determines if Refresh token claims represent a valid refresh token.
     *
     * @param claims parsed JWT claims
     * @return true if basic validity checks pass
     */
    public boolean isValidRefreshToken(Claims claims) {
        return claims.getExpiration() != null
                && claims.getExpiration().after(new Date())
                && StringUtils.hasText(claims.getSubject())
                && StringUtils.hasText(claims.get(CLAIM_KEY_USER_ID, String.class))
                && StringUtils.hasText(claims.get("jti", String.class))
                && StringUtils.hasText(claims.get("familyId", String.class))
                && StringUtils.hasText(claims.get("clientId", String.class));
    }

    /**
     * Builds a Spring Security authentication object from a validated Access token.
     *
     * @param token signed JWT
     * @return {@code UsernamePasswordAuthenticationToken} or null if invalid
     */
    public UsernamePasswordAuthenticationToken manageAuthentication(String token) {
        return createAuthentication(validateAccessToken(token));
    }

    /**
     * Creates a Spring Security authentication object based on token claims.
     *
     * @param claims JWT claims to extract authorities and principal info
     * @return authentication object or null if invalid
     */
    public UsernamePasswordAuthenticationToken createAuthentication(Claims claims) {
        return isValidAccessToken(claims)
                ? new UsernamePasswordAuthenticationToken(
                        new UserPrincipal(claims),
                        null,
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
