package com.alpaca.service.impl;

import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.entity.RefreshToken;
import com.alpaca.entity.Session;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.AuthCode;
import com.alpaca.model.UserPrincipal;
import com.alpaca.persistence.IGenericDAO;
import com.alpaca.persistence.IRefreshTokenDAO;
import com.alpaca.security.manager.JJwtManager;
import com.alpaca.service.IGenericService;
import com.alpaca.service.IRefreshTokenService;
import com.alpaca.service.ISessionService;
import com.alpaca.service.IUserService;
import com.alpaca.utils.UUIDv7Generator;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Service layer implementation for managing {@link RefreshToken} entities. Inherits common CRUD
 * operations from {@link IGenericService}.
 *
 * <p>This service delegates persistence operations to the {@link IRefreshTokenDAO} and handles
 * token rotation, refresh token validation with automatic reuse-detection revocation, and JWT
 * issuance in coordination with {@link ISessionService} and {@link JJwtManager}.
 *
 * @see IGenericService
 * @see IRefreshTokenService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl extends GenericServiceImpl<RefreshToken, UUID>
        implements IRefreshTokenService {

    private final IRefreshTokenDAO dao;
    private final ISessionService sessionService;
    private final IUserService userService;
    private final JJwtManager manager;
    private final UUIDv7Generator uuidv7Generator;

    private static final String MESSAGE_REUSE_REASON = "reuse-detected";
    private static final String REVOKE_REASON_ROTATION = "rotation";

    /**
     * Provides the generic DAO used by inherited service methods.
     *
     * @return the {@link IGenericDAO} implementation for {@link RefreshToken}
     */
    @Override
    @Generated
    protected IGenericDAO<RefreshToken, UUID> getDAO() {
        return dao;
    }

    /**
     * Supplies a human-readable name representing the entity, used in exception messages and
     * logging.
     *
     * @return the string literal "RefreshToken"
     */
    @Override
    @Generated
    protected String getEntityName() {
        return "RefreshToken";
    }

    /**
     * Rotates a refresh token: validates and revokes the old token, then issues a new access and
     * refresh token pair.
     *
     * <p><b>Validation sequence:</b>
     *
     * <ol>
     *   <li>All input parameters (token, clientId, userAgent, clientIp) are checked for blank or
     *       null values.
     *   <li>The incoming token is hashed and looked up in the database.
     *   <li>The found token undergoes full validation via {@link #validateRefreshToken}, which
     *       rejects revoked, expired, or reused tokens and revokes the entire family on detection
     *       of reuse.
     *   <li>The associated session is checked — revoked sessions cause rejection.
     * </ol>
     *
     * <p><b>Rotation:</b> The old token is marked revoked with reason {@code "rotation"}. A new
     * {@link RefreshToken} is created with a fresh {@code familyId} and linked back to the original
     * via {@code replacedBy}.
     *
     * <p>Isolation {@link Isolation#REPEATABLE_READ} prevents phantom reads during the
     * validate-and-rotate sequence so that concurrent reuse of the same token is reliably detected.
     *
     * @param oldRefreshToken the raw refresh token string to rotate
     * @param clientId the OAuth2 client identifier for origin validation
     * @param userAgent the HTTP User-Agent for fingerprinting
     * @param clientIp the request IP address for audit
     * @return an {@link AuthResponseDTO} containing the new access and refresh tokens
     * @throws BadRequestException if any input parameter is blank
     * @throws UnauthorizedException if the token is invalid, revoked, or failed validation
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @Override
    public AuthResponseDTO rotateRefreshToken(
            String oldRefreshToken, String clientId, String userAgent, String clientIp) {

        if (!StringUtils.hasText(oldRefreshToken)) {
            throw new BadRequestException("Invalid Refresh Token");
        }
        if (!StringUtils.hasText(clientId)) {
            throw new BadRequestException("Invalid Client ID");
        }
        if (!StringUtils.hasText(userAgent)) {
            throw new BadRequestException("Invalid User Agent");
        }
        if (!StringUtils.hasText(clientIp)) {
            throw new BadRequestException("Invalid Client IP");
        }

        Instant now = Instant.now();

        String oldRefreshTokenHash = manager.createTokenHash(oldRefreshToken);
        RefreshToken actualRefreshToken =
                dao.findByTokenHashSecure(oldRefreshTokenHash)
                        .orElseThrow(() -> new UnauthorizedException("Invalid Refresh Token"));

        validateRefreshToken(actualRefreshToken, clientId, now, clientIp, userAgent);

        sessionService
                .findSessionByFamilyId(actualRefreshToken.getFamilyId())
                .ifPresent(
                        actualSession -> {
                            if (actualSession.isRevoked()
                                    || (actualSession.getRevokedAt() != null
                                            && actualSession.getRevokedAt().isBefore(now))) {
                                throw new UnauthorizedException("Revoked Session");
                            }
                        });

        actualRefreshToken.setRevoked(true);
        actualRefreshToken.setRevokedAt(now);
        actualRefreshToken.setRevokeReason(REVOKE_REASON_ROTATION);
        actualRefreshToken.setLastUsedAt(now);
        actualRefreshToken.setIpAddress(clientIp);
        actualRefreshToken.setUserAgent(userAgent);

        RefreshToken newRefreshToken =
                new RefreshToken(
                        actualRefreshToken,
                        uuidv7Generator.generate(),
                        now.plusMillis(manager.getJwtTimeExpRefresh()),
                        now,
                        clientId,
                        userAgent,
                        clientIp);
        String jwtRefreshToken = manager.createRefreshToken(newRefreshToken);
        String refreshTokenHash = manager.createTokenHash(jwtRefreshToken);
        newRefreshToken.setTokenHash(refreshTokenHash);
        RefreshToken savedRefreshToken = super.save(newRefreshToken);
        actualRefreshToken.setReplacedBy(savedRefreshToken);
        super.save(actualRefreshToken);
        String accessToken =
                manager.createAccessToken(new UserPrincipal(newRefreshToken.getUser()), now);
        return new AuthResponseDTO(accessToken, jwtRefreshToken);
    }

    /**
     * Generates a full JWT token pair (access + refresh) for the given user principal and session.
     * A new {@link RefreshToken} entity is persisted with a hashed representation of the refresh
     * JWT; the raw token string is returned only in the response DTO and is never stored.
     *
     * @param userPrincipal the authenticated user's principal used as the access token subject
     * @param session the session to associate the refresh token with
     * @return an {@link AuthResponseDTO} containing the access token and the raw refresh token
     *     string
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Override
    public AuthResponseDTO generateJWTTokens(UserPrincipal userPrincipal, Session session) {
        RefreshToken refreshToken =
                new RefreshToken(
                        session,
                        uuidv7Generator.generate(),
                        session.getLastSeenAt().plusMillis(manager.getJwtTimeExpRefresh()),
                        session.getLastSeenAt());
        String jwtRefreshToken = manager.createRefreshToken(refreshToken);
        String refreshTokenHash = manager.createTokenHash(jwtRefreshToken);
        refreshToken.setTokenHash(refreshTokenHash);
        super.save(refreshToken);
        String accessToken = manager.createAccessToken(userPrincipal, session.getLastSeenAt());
        return new AuthResponseDTO(accessToken, jwtRefreshToken);
    }

    /**
     * Generates a full JWT token pair using an authorization code. This is the terminal step of the
     * OAuth2 authorization code flow: the code is exchanged for tokens by first looking up the
     * user, creating a session for the device fingerprint embedded in the code, and then issuing
     * the JWT pair.
     *
     * @param authCode the consumed authorization code containing user and device context
     * @return an {@link AuthResponseDTO} containing the access token and raw refresh token string
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Override
    public AuthResponseDTO generateJWTTokens(AuthCode authCode) {
        User user = userService.findById(authCode.getUserId());
        Session session =
                sessionService.createSession(
                        authCode.getUserId(),
                        authCode.getUserAgent(),
                        authCode.getClientId(),
                        authCode.getClientIp());
        RefreshToken refreshToken =
                new RefreshToken(
                        session,
                        uuidv7Generator.generate(),
                        session.getLastSeenAt().plusMillis(manager.getJwtTimeExpRefresh()),
                        session.getLastSeenAt());
        String jwtRefreshToken = manager.createRefreshToken(refreshToken);
        String refreshTokenHash = manager.createTokenHash(jwtRefreshToken);
        refreshToken.setTokenHash(refreshTokenHash);
        super.save(refreshToken);
        String accessToken =
                manager.createAccessToken(new UserPrincipal(user), session.getLastSeenAt());
        return new AuthResponseDTO(accessToken, jwtRefreshToken);
    }

    /**
     * Validates a refresh token against all security checks. On any failure, the entire token
     * family is revoked as a reuse-detection measure, and the event is logged.
     *
     * <p><b>Validation rules (in order):</b>
     *
     * <ol>
     *   <li>Token is not already revoked.
     *   <li>Token has not been {@code replacedBy} another token (reuse indicator).
     *   <li>Token has not expired.
     *   <li>User's {@code tokensInvalidBefore} timestamp does not predate token creation (global
     *       token invalidation).
     *   <li>{@code clientId} matches the token's recorded client.
     *   <li>{@code userAgent} matches the token's recorded user agent.
     * </ol>
     *
     * <p><b>Critical side effect:</b> Each failed check revokes all tokens and sessions sharing the
     * same {@code familyId}. This is an intentional security measure — any deviation from the
     * expected token context is treated as potential token theft, and the entire family is
     * invalidated immediately.
     *
     * @param token the refresh token entity to validate
     * @param clientId the expected OAuth2 client identifier
     * @param now the reference timestamp for expiry comparison
     * @param clientIp the request IP (used for audit logging on failure)
     * @param userAgent the expected HTTP User-Agent header value
     * @throws UnauthorizedException if any validation check fails
     */
    @Override
    public void validateRefreshToken(
            RefreshToken token, String clientId, Instant now, String clientIp, String userAgent)
            throws UnauthorizedException {
        if (token.isRevoked()) {
            revokeRefreshTokensAndSessionByFamilyId(token.getFamilyId(), now, MESSAGE_REUSE_REASON);
            logWhenReuseDetected(token.getFamilyId().toString(), clientIp, userAgent);
            throw new UnauthorizedException("Refresh Token already revoked");
        }
        if (token.getReplacedBy() != null) {
            revokeRefreshTokensAndSessionByFamilyId(token.getFamilyId(), now, MESSAGE_REUSE_REASON);
            logWhenReuseDetected(token.getFamilyId().toString(), clientIp, userAgent);
            throw new UnauthorizedException("Reuse Detected Refresh Token");
        }
        if (token.getExpiresAt().isBefore(now)) {
            revokeRefreshTokensAndSessionByFamilyId(token.getFamilyId(), now, MESSAGE_REUSE_REASON);
            logWhenReuseDetected(token.getFamilyId().toString(), clientIp, userAgent);
            throw new UnauthorizedException("Reuse Detected Refresh Token");
        }
        if (token.getUser().getTokensInvalidBefore() != null
                && token.getCreatedAt().isBefore(token.getUser().getTokensInvalidBefore())) {
            revokeRefreshTokensAndSessionByFamilyId(token.getFamilyId(), now, MESSAGE_REUSE_REASON);
            logWhenReuseDetected(token.getFamilyId().toString(), clientIp, userAgent);
            throw new UnauthorizedException("Refresh Token already revoked");
        }
        if (!Objects.equals(token.getClientId(), clientId)) {
            revokeRefreshTokensAndSessionByFamilyId(token.getFamilyId(), now, "client-mismatch");
            logWhenReuseDetected(token.getFamilyId().toString(), clientIp, userAgent);
            throw new UnauthorizedException("Client mismatch");
        }
        if (!Objects.equals(token.getUserAgent(), userAgent)) {
            revokeRefreshTokensAndSessionByFamilyId(token.getFamilyId(), now, "ua-mismatch");
            logWhenReuseDetected(token.getFamilyId().toString(), clientIp, userAgent);
            throw new UnauthorizedException("User-Agent mismatch");
        }
    }

    /**
     * Revokes all refresh tokens sharing the given {@code familyId} and cascades the revocation to
     * the associated session. This is the terminal operation when token theft or reuse is detected.
     *
     * @param familyId the token family to revoke
     * @param now the revocation timestamp
     * @param reason a human-readable reason recorded for audit
     */
    @Override
    public void revokeRefreshTokensAndSessionByFamilyId(UUID familyId, Instant now, String reason) {
        revokeFamilyWithReason(familyId, now, reason);
        sessionService.revokeSessionByFamilyId(familyId, now, reason);
    }

    /**
     * Revokes every refresh token that belongs to the given family.
     *
     * @param familyId the token family identifier to revoke
     * @param revokedAt the timestamp of revocation
     * @param reason a human-readable reason for audit trail
     */
    @Override
    public void revokeFamilyWithReason(UUID familyId, Instant revokedAt, String reason) {
        dao.revokeFamilyWithReason(familyId, revokedAt, reason);
    }

    /**
     * Finds the token family identifier associated with the given hash.
     *
     * @param hash the hashed refresh token value
     * @return an {@link Optional} containing the family identifier if found
     */
    @Override
    public Optional<UUID> findFamilyIdByTokenHash(String hash) {
        return dao.findFamilyIdByTokenHash(hash);
    }

    /**
     * Finds a refresh token by its hashed value. The lookup uses a constant-time comparison to
     * prevent timing attacks.
     *
     * @param hash the hashed refresh token value
     * @return an {@link Optional} containing the matching {@link RefreshToken} if found
     */
    @Override
    public Optional<RefreshToken> findByTokenHashSecure(String hash) {
        return dao.findByTokenHashSecure(hash);
    }

    private void logWhenReuseDetected(String familyId, String clientIp, String userAgent) {
        log.warn(
                "Refresh token reuse detected. familyId={}, ip={}, userAgent={}",
                familyId,
                clientIp,
                userAgent);
    }

    /**
     * Updates an existing {@link RefreshToken} identified by {@code id} with values supplied in
     * {@code refreshToken}. This method applies selective updates:
     *
     * <ul>
     *   <li>Associations such as {@code user} and {@code replacedBy} are updated only if the
     *       incoming association is non-null, has an identifier, and differs from the stored one.
     *   <li>Scalar and timestamp fields are updated through helper methods that check for
     *       non-nullity and inequality (e.g. {@code updateIfNotNull}, {@code updateIfDifferent}).
     *   <li>Textual fields are updated only when incoming text is present and differs from the
     *       existing value (see {@code updateTextIfExists}).
     * </ul>
     *
     * <p>The specific fields that may be updated include: {@code user}, {@code replacedBy}, {@code
     * tokenJti}, {@code familyId}, {@code revoked}, {@code revokedAt}, {@code expiresAt}, {@code
     * lastUsedAt}, {@code tokenHash}, {@code clientId}, {@code ipAddress}, {@code userAgent}, and
     * {@code revokeReason}.
     *
     * @param refreshToken the {@link RefreshToken} containing new values to apply; may include
     *     nulls for fields that should remain unchanged
     * @param id the unique identifier of the persisted {@link RefreshToken} to update
     * @return the updated and saved {@link RefreshToken} instance
     * @throws NotFoundException if a {@link RefreshToken} with the supplied {@code id} does not
     *     exist
     */
    @Override
    @Transactional
    public RefreshToken updateById(RefreshToken refreshToken, UUID id) {
        if (refreshToken == null || id == null)
            throw new BadRequestException(
                    String.format("%s with ID %s cannot be updated", getEntityName(), id));

        RefreshToken existingRefreshToken =
                dao.findById(id)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                String.format(
                                                        "%s with ID %s not found",
                                                        getEntityName(), id)));

        if (refreshToken.getUser() != null && refreshToken.getUser().getId() != null) {
            UUID currentUserId =
                    existingRefreshToken.getUser() != null
                            ? existingRefreshToken.getUser().getId()
                            : null;
            if (!Objects.equals(refreshToken.getUser().getId(), currentUserId)) {
                existingRefreshToken.setUser(refreshToken.getUser());
            }
        }

        if (refreshToken.getReplacedBy() != null && refreshToken.getReplacedBy().getId() != null) {
            UUID currentUserId =
                    existingRefreshToken.getReplacedBy() != null
                            ? existingRefreshToken.getReplacedBy().getId()
                            : null;
            if (!Objects.equals(refreshToken.getReplacedBy().getId(), currentUserId)) {
                existingRefreshToken.setReplacedBy(refreshToken.getReplacedBy());
            }
        }

        updateIfNotNull(
                existingRefreshToken.getTokenJti(),
                refreshToken.getTokenJti(),
                existingRefreshToken::setTokenJti);
        updateIfNotNull(
                existingRefreshToken.getFamilyId(),
                refreshToken.getFamilyId(),
                existingRefreshToken::setFamilyId);
        updateIfDifferent(
                existingRefreshToken.isRevoked(),
                refreshToken.isRevoked(),
                existingRefreshToken::setRevoked);
        updateIfNotNull(
                existingRefreshToken.getRevokedAt(),
                refreshToken.getRevokedAt(),
                existingRefreshToken::setRevokedAt);
        updateIfNotNull(
                existingRefreshToken.getExpiresAt(),
                refreshToken.getExpiresAt(),
                existingRefreshToken::setExpiresAt);
        updateIfNotNull(
                existingRefreshToken.getLastUsedAt(),
                refreshToken.getLastUsedAt(),
                existingRefreshToken::setLastUsedAt);

        updateTextIfExists(
                existingRefreshToken.getTokenHash(),
                refreshToken.getTokenHash(),
                existingRefreshToken::setTokenHash);
        updateTextIfExists(
                existingRefreshToken.getClientId(),
                refreshToken.getClientId(),
                existingRefreshToken::setClientId);
        updateTextIfExists(
                existingRefreshToken.getIpAddress(),
                refreshToken.getIpAddress(),
                existingRefreshToken::setIpAddress);
        updateTextIfExists(
                existingRefreshToken.getUserAgent(),
                refreshToken.getUserAgent(),
                existingRefreshToken::setUserAgent);
        updateTextIfExists(
                existingRefreshToken.getRevokeReason(),
                refreshToken.getRevokeReason(),
                existingRefreshToken::setRevokeReason);
        return super.save(existingRefreshToken);
    }
}
