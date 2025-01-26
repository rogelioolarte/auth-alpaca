package com.example.security.manager;

import com.example.config.AppProperties;
import com.example.exception.UnauthorizedException;
import com.example.model.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.SignatureAlgorithm;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JJwtManager {

    private final SignatureAlgorithm alg = Jwts.SIG.RS512;

    private final AppProperties app;
    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;

    public JJwtManager(AppProperties app)
            throws Exception {
        this.app = app;
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        this.publicKey = (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec
                (Decoders.BASE64.decode(app.jwtPublicKey)));
        this.privateKey = (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec
                (Decoders.BASE64.decode(app.jwtPrivateKey)));

    }

    public String createToken(UserPrincipal user) {
        return Jwts.builder()
                .issuer(app.jwtUserGenerator)
                .subject(user.getUsername())
                .claim("authorities", authoritiesToString(user.getAuthorities()))
                .claim("userId", user.getId().toString())
                .claim("profileId", user.getProfileId() != null ?
                        user.getProfileId().toString() : "")
                .claim("advertiserId", user.getAdvertiserId() != null ?
                        user.getAdvertiserId().toString() : "")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() +
                        Long.parseLong(app.jwtTimeExpiration)))
                .notBefore(new Date(System.currentTimeMillis()))
                .signWith(privateKey, alg).compact();
    }

    public Jws<Claims> validateToken(String token) {
        try {
            return Jwts.parser().verifyWith(publicKey)
                    .requireIssuer(app.jwtUserGenerator)
                    .build()
                    .parseSignedClaims(token);
        } catch (Exception exception){
            throw new UnauthorizedException("Token Invalid, Unauthorized");
        }
    }

    public UsernamePasswordAuthenticationToken manageAuthentication(String token) {
        return createAuthentication(getAllClaims(validateToken(token)));
    }

    public UsernamePasswordAuthenticationToken createAuthentication(Claims claims){
        return isValidToken(claims) ? new UsernamePasswordAuthenticationToken(getUserPrincipal(
                claims), null, getAuthoritiesList(claims)) : null;
    }

    public boolean isValidToken(Claims claims) {
        return claims.getExpiration() != null && claims.getExpiration().after(new Date()) &&
                existString(getUsername(claims)) && existString(getUserId(claims)) &&
                existString(getAuthorities(claims));
//                && (existString(getProfileId(claims)) || existString(getAdvertiserId(claims)))
    }

    public String getUsername(Claims claims) {
        return claims.getSubject();
    }

    public String getAuthorities(Claims claims) {
        return getSpecificClaim(claims, "authorities", String.class);
    }
    public List<? extends GrantedAuthority> getAuthoritiesList(Claims claims) {
        return AuthorityUtils.commaSeparatedStringToAuthorityList(getAuthorities(claims));
    }

    public String getUserId(Claims claims) {
        return getSpecificClaim(claims, "userId", String.class);
    }

    public String getProfileId(Claims claims) {
        return getSpecificClaim(claims, "profileId", String.class);
    }

    public String getAdvertiserId(Claims claims) {
        return getSpecificClaim(claims, "advertiserId", String.class);
    }

    public boolean existString(String string) {
        return string != null && !string.isBlank();
    }

    public UserPrincipal getUserPrincipal(Claims claims) {
        return new UserPrincipal( UUID.fromString(getUserId(claims)),
                existString(getProfileId(claims)) ?
                        UUID.fromString(getProfileId(claims)) : null,
                existString(getAdvertiserId(claims)) ?
                        UUID.fromString(getAdvertiserId(claims)) : null,
                getUsername(claims), null, getAuthoritiesList(claims), null);
    }

    public <T> T getSpecificClaim(Claims claims, String claimName, Class<T> t) {
        return claims.get(claimName, t);
    }

    public Claims getAllClaims(Jws<Claims> claims) {
        return claims.getPayload();
    }

    public String authoritiesToString(Collection<? extends GrantedAuthority>
                                              grantedAuthorities) {
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
