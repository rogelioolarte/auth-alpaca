package com.alpaca.model;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents an OAuth2 authorization code with optional PKCE support.
 *
 * <p>This model (not a JPA entity) captures the parameters of an authorization code during the
 * OAuth2 authorization code flow. The code is short-lived and may carry a PKCE challenge or
 * verifier depending on the flow variant (S256 challenge vs. plain verifier).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthCode {

    /** The opaque authorization code value issued to the client. */
    String code;

    /**
     * PKCE code challenge (S256 hash of the verifier). Present when the client uses the
     * authorization code flow with PKCE and the {@code code_challenge_method} is {@code S256}.
     */
    String codeChallenge;

    /**
     * PKCE code verifier (raw secret). Present in flows where the server manages the verifier
     * directly rather than receiving a challenge from the client.
     */
    String codeVerifier;

    /** The redirect URI that was used in the original authorization request. */
    String redirectUri;

    /** OAuth2 client identifier that requested the authorization code. */
    String clientId;

    /** User-agent header from the client's authorization request, for audit purposes. */
    String userAgent;

    /** IP address from which the authorization request originated, for audit purposes. */
    String clientIp;

    /** The authenticated user ID that approved the authorization request. */
    UUID userId;

    /** Instant after which this authorization code is no longer valid. */
    Instant expiresAt;

    /**
     * Full constructor for the authorization code flow with PKCE (S256 challenge).
     *
     * <p>Sets the TTL to 60 seconds from now. The authenticated {@code userId} is recorded so the
     * server can resolve the user when the code is exchanged.
     *
     * @param code the opaque authorization code
     * @param codeChallenge the PKCE S256 code challenge from the client
     * @param clientId the OAuth2 client identifier
     * @param userAgent the user-agent header from the authorization request
     * @param clientIp the client IP from the authorization request
     * @param userId the authenticated user that approved the code
     * @param redirectUri the redirect URI bound to this authorization code
     */
    public AuthCode(
            String code,
            String codeChallenge,
            String clientId,
            String userAgent,
            String clientIp,
            UUID userId,
            String redirectUri) {
        this.code = code;
        this.codeChallenge = codeChallenge;
        this.clientId = clientId;
        this.userAgent = userAgent;
        this.clientIp = clientIp;
        this.userId = userId;
        this.redirectUri = redirectUri;
        this.expiresAt = Instant.now().plusSeconds(60);
    }

    /**
     * Constructor for flows where the server issues the code with a raw PKCE verifier rather than a
     * challenge. No user ID or expiry is set — caller should populate those separately if needed.
     *
     * @param code the opaque authorization code
     * @param codeVerifier the raw PKCE code verifier
     * @param redirectUri the redirect URI bound to this code
     * @param clientId the OAuth2 client identifier
     * @param userAgent the user-agent header from the authorization request
     * @param clientIp the client IP from the authorization request
     */
    public AuthCode(
            String code,
            String codeVerifier,
            String redirectUri,
            String clientId,
            String userAgent,
            String clientIp) {
        this.code = code;
        this.codeVerifier = codeVerifier;
        this.redirectUri = redirectUri;
        this.clientId = clientId;
        this.userAgent = userAgent;
        this.clientIp = clientIp;
    }

    /**
     * Minimal constructor with just the essential authorization code fields.
     *
     * <p>Suitable for simple or legacy flows where PKCE is not used and audit metadata is tracked
     * elsewhere.
     *
     * @param code the opaque authorization code
     * @param codeVerifier the raw PKCE code verifier (or empty/non-null for non-PKCE flows)
     * @param redirectUri the redirect URI bound to this code
     */
    public AuthCode(String code, String codeVerifier, String redirectUri) {
        this.code = code;
        this.codeVerifier = codeVerifier;
        this.redirectUri = redirectUri;
    }
}
