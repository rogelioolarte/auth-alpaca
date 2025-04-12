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

@Component
public class JJwtManager {

  private final SignatureAlgorithm alg = Jwts.SIG.RS512;

  private final RSAPrivateKey privateKey;
  private final RSAPublicKey publicKey;
  private final String jwtUserGenerator;
  private final String jwtTimeExpiration;

  public JJwtManager(
      @Value("${app.jwtPrivateKey}") @NotNull String jwtPrivateKey,
      @Value("${app.jwtPublicKey}") @NotNull String jwtPublicKey,
      @Value("${app.jwtUserGenerator}") @NotNull String jwtUSerGenerator,
      @Value("${app.jwtTimeExpiration}") @NotNull String jwtTimeExpiration)
      throws Exception {
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    this.publicKey =
        (RSAPublicKey)
            keyFactory.generatePublic(new X509EncodedKeySpec(Decoders.BASE64.decode(jwtPublicKey)));
    this.privateKey =
        (RSAPrivateKey)
            keyFactory.generatePrivate(
                new PKCS8EncodedKeySpec(Decoders.BASE64.decode(jwtPrivateKey)));
    this.jwtUserGenerator = jwtUSerGenerator;
    this.jwtTimeExpiration = jwtTimeExpiration;
  }

  public String createToken(UserPrincipal user) {
    return Jwts.builder()
        .issuer(jwtUserGenerator)
        .subject(user.getUsername())
        .claim("authorities", authoritiesToString(user.getAuthorities()))
        .claim("userId", user.getId().toString())
        .claim("profileId", user.getProfileId() != null ? user.getProfileId().toString() : "")
        .claim(
            "advertiserId", user.getAdvertiserId() != null ? user.getAdvertiserId().toString() : "")
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + Long.parseLong(jwtTimeExpiration)))
        .notBefore(new Date(System.currentTimeMillis()))
        .signWith(privateKey, alg)
        .compact();
  }

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

  public UsernamePasswordAuthenticationToken manageAuthentication(String token) {
    return createAuthentication(getAllClaims(validateToken(token)));
  }

  public UsernamePasswordAuthenticationToken createAuthentication(Claims claims) {
    return isValidToken(claims)
        ? new UsernamePasswordAuthenticationToken(
            getUserPrincipal(claims), null, getAuthoritiesList(claims))
        : null;
  }

  public boolean isValidToken(Claims claims) {
    return claims.getExpiration() != null
        && claims.getExpiration().after(new Date())
        && existString(getUsername(claims))
        && existString(getUserId(claims))
        && existString(getAuthorities(claims));
    //                && (existString(getProfileId(claims)) ||
    // existString(getAdvertiserId(claims)))
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
    return new UserPrincipal(
        UUID.fromString(getUserId(claims)),
        existString(getProfileId(claims)) ? UUID.fromString(getProfileId(claims)) : null,
        existString(getAdvertiserId(claims)) ? UUID.fromString(getAdvertiserId(claims)) : null,
        getUsername(claims),
        null,
        getAuthoritiesList(claims),
        null);
  }

  public <T> T getSpecificClaim(Claims claims, String claimName, Class<T> t) {
    return claims.get(claimName, t);
  }

  public Claims getAllClaims(Jws<Claims> claims) {
    return claims.getPayload();
  }

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
