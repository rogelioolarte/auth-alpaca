package com.alpaca.service;

import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.entity.RefreshToken;
import com.alpaca.entity.Session;
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

    AuthResponseDTO rotateRefreshToken(
            String refreshToken, String clientId, String userAgent, String clientIp);

    AuthResponseDTO generateJWTTokens(UserPrincipal userPrincipal, Session session);

    AuthResponseDTO generateJWTTokens(AuthCode authCode);

    void revokeRefreshTokensAndSessionByFamilyId(UUID familyId, Instant now, String reason);

    void revokeFamilyWithReason(UUID familyId, Instant revokedAt, String reason);

    Optional<UUID> findFamilyIdByTokenHash(String hash);

    Optional<RefreshToken> findByTokenHashSecure(String hash);
}
