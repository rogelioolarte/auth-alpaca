package com.alpaca.security.manager;

import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.SignatureAlgorithm;
import jakarta.validation.constraints.NotNull;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;

/**
 * {@code JJwtManager} is a Spring component responsible for managing JSON Web Tokens (JWT) using
 * asymmetric RSA key pairs for signing and verification.
 *
 * <p>This class provides methods to:
 *
 * <ul>
 *   <li>Generate signed JWT tokens from {@link UserPrincipal} instances.
 *   <li>Validate and parse tokens using the configured RSA public key.
 *   <li>Extract claims and build authentication objects compatible with Spring Security.
 * </ul>
 *
 * <p>Security is enforced using the {@code RS512} algorithm ({@link SignatureAlgorithm}) which
 * leverages RSA with SHA-512 hashing. Keys are injected via application properties and decoded
 * using the {@link KeyFactory} standard mechanism.
 *
 * <p><strong>Configuration properties required:</strong>
 *
 * <ul>
 *   <li>{@code app.jwtPrivateKey}: Base64-encoded RSA private key in PKCS#8 format.
 *   <li>{@code app.jwtPublicKey}: Base64-encoded RSA public key in X.509 format.
 *   <li>{@code app.jwtUserGenerator}: Identifier of the issuer of tokens.
 *   <li>{@code app.jwtTimeExpiration}: Token expiration time in milliseconds.
 * </ul>
 *
 * @see UserPrincipal
 * @see UnauthorizedException
 * @see io.jsonwebtoken.JwtParser
 */
@Component
public class JJwtManager {

    /** The signature algorithm used for JWT creation: RS512 (RSA + SHA-512). */
    private final SignatureAlgorithm alg = Jwts.SIG.RS512;

    /** RSA private key used to sign tokens. */
    private final RSAPrivateKey privateKey;

    /** RSA public key used to verify tokens. */
    private final RSAPublicKey publicKey;

    /** The issuer (generator) identifier included in the token payload. */
    private final String jwtUserGenerator;

    /** Token expiration time (in milliseconds), configured via properties. */
    private final String jwtTimeExpiration;

    /**
     * Constructs a {@code JJwtManager} with the necessary RSA keys and configuration.
     *
     * @param jwtPrivateKey Base64-encoded RSA private key (PKCS#8 format)
     * @param jwtPublicKey Base64-encoded RSA public key (X.509 format)
     * @param jwtUSerGenerator Issuer name to include in JWT tokens
     * @param jwtTimeExpiration Expiration time in milliseconds for tokens
     * @throws Exception if the provided keys cannot be parsed or decoded
     */
    public JJwtManager(
            @Value("${app.jwtPrivateKey}") @NotNull String jwtPrivateKey,
            @Value("${app.jwtPublicKey}") @NotNull String jwtPublicKey,
            @Value("${app.jwtUserGenerator}") @NotNull String jwtUSerGenerator,
            @Value("${app.jwtTimeExpiration}") @NotNull String jwtTimeExpiration)
            throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        this.publicKey =
                (RSAPublicKey)
                        keyFactory.generatePublic(
                                new X509EncodedKeySpec(Decoders.BASE64.decode(jwtPublicKey)));
        this.privateKey =
                (RSAPrivateKey)
                        keyFactory.generatePrivate(
                                new PKCS8EncodedKeySpec(Decoders.BASE64.decode(jwtPrivateKey)));
        this.jwtUserGenerator = jwtUSerGenerator;
        this.jwtTimeExpiration = jwtTimeExpiration;
    }

    /**
     * Creates a new signed JWT token containing information about a {@link UserPrincipal}.
     *
     * @param user the authenticated user
     * @return a signed JWT as a String
     */
    public String createToken(UserPrincipal user) {
        return Jwts.builder()
                .issuer(jwtUserGenerator)
                .subject(user.getUsername())
                .claim("authorities", authoritiesToString(user.getAuthorities()))
                .claim("userId", user.getId().toString())
                .claim(
                        "profileId",
                        user.getProfileId() != null ? user.getProfileId().toString() : "")
                .claim(
                        "advertiserId",
                        user.getAdvertiserId() != null ? user.getAdvertiserId().toString() : "")
                .issuedAt(new Date())
                .expiration(
                        new Date(System.currentTimeMillis() + Long.parseLong(jwtTimeExpiration)))
                .notBefore(new Date(System.currentTimeMillis()))
                .signWith(privateKey, alg)
                .compact();
    }

    /**
     * Validates and parses a JWT token using the public key.
     *
     * @param token the token to validate
     * @return a {@link Jws} containing the claims if valid
     * @throws UnauthorizedException if the token is invalid or expired
     */
    public Jws<Claims> validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(jwtUserGenerator)
                    .build()
                    .parseSignedClaims(token);
        } catch (Exception exception) {
            throw new UnauthorizedException("Token Invalid, Unauthorized");
        }
    }

    /**
     * Builds a Spring Security authentication object from a JWT token.
     *
     * @param token the JWT token
     * @return a populated {@link UsernamePasswordAuthenticationToken}
     */
    public UsernamePasswordAuthenticationToken manageAuthentication(String token) {
        return createAuthentication(getAllClaims(validateToken(token)));
    }

    /**
     * Creates an authentication object if the claims represent a valid token.
     *
     * @param claims the JWT claims
     * @return a {@link UsernamePasswordAuthenticationToken} or {@code null} if invalid
     */
    public UsernamePasswordAuthenticationToken createAuthentication(Claims claims) {
        return isValidToken(claims)
                ? new UsernamePasswordAuthenticationToken(
                        getUserPrincipal(claims), null, getAuthoritiesList(claims))
                : null;
    }

    /**
     * Verifies whether the claims represent a valid token.
     *
     * @param claims the token claims
     * @return {@code true} if valid, otherwise {@code false}
     */
    public boolean isValidToken(Claims claims) {
        return claims.getExpiration() != null
                && claims.getExpiration().after(new Date())
                && existString(getUsername(claims))
                && existString(getUserId(claims))
                && existString(getAuthorities(claims));
        // Optional: enforce at least profileId or advertiserId presence.
    }

    /**
     * @return the username (JWT subject).
     */
    public String getUsername(Claims claims) {
        return claims.getSubject();
    }

    /**
     * @return the raw authorities string (comma-separated).
     */
    public String getAuthorities(Claims claims) {
        return getSpecificClaim(claims, "authorities", String.class);
    }

    /**
     * @return the authorities as a {@link List} of Spring Security {@link GrantedAuthority}.
     */
    public List<? extends GrantedAuthority> getAuthoritiesList(Claims claims) {
        return AuthorityUtils.commaSeparatedStringToAuthorityList(getAuthorities(claims));
    }

    /**
     * @return the userId claim as a String.
     */
    public String getUserId(Claims claims) {
        return getSpecificClaim(claims, "userId", String.class);
    }

    /**
     * @return the profileId claim as a String.
     */
    public String getProfileId(Claims claims) {
        return getSpecificClaim(claims, "profileId", String.class);
    }

    /**
     * @return the advertiserId claim as a String.
     */
    public String getAdvertiserId(Claims claims) {
        return getSpecificClaim(claims, "advertiserId", String.class);
    }

    /**
     * Converts a claim string to a {@link UUID}.
     *
     * @param claim the string claim value
     * @return a UUID or {@code null} if blank
     */
    public UUID getUUIDFromClaim(String claim) {
        return !claim.isBlank() ? UUID.fromString(claim) : null;
    }

    /**
     * Checks whether a string is non-null and non-blank.
     *
     * @param string the string to check
     * @return {@code true} if non-empty, otherwise {@code false}
     */
    public boolean existString(String string) {
        return string != null && !string.isBlank();
    }

    /**
     * Builds a {@link UserPrincipal} from JWT claims.
     *
     * @param claims the token claims
     * @return a {@link UserPrincipal} populated with claim values
     */
    public UserPrincipal getUserPrincipal(Claims claims) {
        return new UserPrincipal(
                getUUIDFromClaim(getUserId(claims)),
                getUUIDFromClaim(getProfileId(claims)),
                getUUIDFromClaim(getAdvertiserId(claims)),
                getUsername(claims),
                null,
                getAuthoritiesList(claims),
                null);
    }

    /**
     * Retrieves a specific claim from JWT claims.
     *
     * @param claims the claims object
     * @param claimName the name of the claim
     * @param t the type to cast the claim value
     * @return the claim value cast to type {@code T}
     */
    public <T> T getSpecificClaim(Claims claims, String claimName, Class<T> t) {
        return claims.get(claimName, t);
    }

    /**
     * Extracts the payload (claims) from a signed JWS token.
     *
     * @param claims the signed claims wrapper
     * @return the {@link Claims} payload
     */
    public Claims getAllClaims(Jws<Claims> claims) {
        return claims.getPayload();
    }

    /**
     * Converts a collection of {@link GrantedAuthority} objects into a comma-separated string.
     *
     * @param grantedAuthorities the authorities collection
     * @return a string representation of authorities
     */
    public String authoritiesToString(Collection<? extends GrantedAuthority> grantedAuthorities) {
        StringBuilder result = new StringBuilder();
        for (GrantedAuthority simpleGrantedAuthority : grantedAuthorities) {
            if (!result.isEmpty()) {
                result.append(",");
            }
            result.append(simpleGrantedAuthority.getAuthority());
        }
        return result.toString();
    }
}
