package com.alpaca.service;

import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.entity.RefreshToken;
import com.alpaca.entity.Session;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.AuthCode;
import com.alpaca.model.UserPrincipal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for managing {@link RefreshToken} entities. Extends {@link IGenericService} to
 * inherit common CRUD operations.
 *
 * @see IGenericService
 */
public interface IRefreshTokenService extends IGenericService<RefreshToken, UUID> {

    /**
     * Validates the provided refresh token and issues a new token pair (access + refresh), revoking
     * the old one.
     *
     * <p>Token rotation ensures that a compromised refresh token can only be used once. If the
     * token has already been rotated (reuse detected), the entire token family is revoked.
     *
     * @param refreshToken the raw refresh token value to rotate — must not be null
     * @param clientId the OAuth2 client identifier associated with the token
     * @param userAgent the {@code User-Agent} header from the client's request
     * @param clientIp the IP address of the client making the request
     * @return a new {@code AuthResponseDTO} containing the rotated access and refresh tokens
     * @throws com.alpaca.exception.UnauthorizedException if the refresh token is invalid, expired,
     *     or revoked
     */
    AuthResponseDTO rotateRefreshToken(
            String refreshToken, String clientId, String userAgent, String clientIp);

    /**
     * Generates a JWT access and refresh token pair for the authenticated user, associated with the
     * given session.
     *
     * @param userPrincipal the authenticated user's principal
     * @param session the session to associate with the tokens
     * @return an {@code AuthResponseDTO} containing the generated access and refresh tokens
     */
    AuthResponseDTO generateJWTTokens(UserPrincipal userPrincipal, Session session);

    /**
     * Generates a JWT access and refresh token pair from an OAuth2 authorization code.
     *
     * @param authCode the authorization code obtained from the OAuth2 provider
     * @return an {@code AuthResponseDTO} containing the generated access and refresh tokens
     */
    AuthResponseDTO generateJWTTokens(AuthCode authCode);

    /**
     * Revokes all refresh tokens and the session associated with the given family.
     *
     * <p>This is called when a refresh token rotation detects token reuse, indicating a potentially
     * compromised token.
     *
     * @param familyId the family identifier whose tokens and session should be revoked
     * @param now the instant marking when the revocation occurred
     * @param reason a human-readable explanation for the revocation
     */
    void revokeRefreshTokensAndSessionByFamilyId(UUID familyId, Instant now, String reason);

    /**
     * Revokes all refresh tokens belonging to the given family without touching the parent session.
     *
     * @param familyId the family identifier whose tokens should be revoked
     * @param revokedAt the instant marking when the revocation occurred
     * @param reason a human-readable explanation for the revocation
     */
    void revokeFamilyWithReason(UUID familyId, Instant revokedAt, String reason);

    /**
     * Validates the integrity and freshness of a refresh token.
     *
     * <p>Checks that the token has not expired, belongs to the expected client, and has not been
     * used from an unexpected IP or user-agent.
     *
     * @param token the {@code RefreshToken} entity to validate — must not be null
     * @param clientId the expected OAuth2 client identifier
     * @param now the current instant for expiry comparison
     * @param clientIp the IP address of the current request for binding validation
     * @param userAgent the {@code User-Agent} header of the current request for binding validation
     * @throws UnauthorizedException if the token is expired, revoked, or bound to different client
     *     credentials
     */
    void validateRefreshToken(
            RefreshToken token, String clientId, Instant now, String clientIp, String userAgent)
            throws UnauthorizedException;

    /**
     * Looks up the family identifier associated with a given token hash.
     *
     * <p>Used during token rotation to detect when an already-rotated token is reused, which
     * signals a potential token compromise.
     *
     * @param hash the SHA-256 hash of the refresh token value
     * @return an {@code Optional} containing the family ID if a matching hash exists, or empty if
     *     not found
     */
    Optional<UUID> findFamilyIdByTokenHash(String hash);

    /**
     * Retrieves a refresh token entity by its secure hash, with timing-safe lookup.
     *
     * @param hash the secure hash of the refresh token value
     * @return an {@code Optional} containing the {@code RefreshToken} if found, or empty if not
     *     found
     */
    Optional<RefreshToken> findByTokenHashSecure(String hash);
}
