package com.alpaca.service.impl;

import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.entity.RefreshToken;
import com.alpaca.entity.Session;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.persistence.IGenericDAO;
import com.alpaca.persistence.IRefreshTokenDAO;
import com.alpaca.security.manager.JJwtManager;
import com.alpaca.service.IGenericService;
import com.alpaca.service.IRefreshTokenService;
import com.alpaca.service.ISessionService;
import com.alpaca.utils.UUIDv7Generator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Service layer implementation for managing {@link RefreshToken} entities. Inherits common CRUD
 * operations from {@link IGenericService}.
 *
 * <p>This service delegates persistence operations to the {@link IRefreshTokenDAO} and provides a
 * clear abstraction point for future business logic related to permissions.
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
	private final JJwtManager manager;
	private final UUIDv7Generator uuidv7Generator;

	private static final String MESSAGE_REUSE_REASON = "reuse-detected";
	private static final String REVOKE_REASON_ROTATION = "rotation";

	@Override
	protected IGenericDAO<RefreshToken, UUID> getDAO() {
		return dao;
	}

	@Override
	protected String getEntityName() {
		return "RefreshToken";
	}

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

		String oldRefreshTokenHash = manager.createRefreshTokenHash(oldRefreshToken);
		Optional<RefreshToken> optToken = dao.findByTokenHashSecure(oldRefreshTokenHash);
		if (optToken.isEmpty()) {
			dao.findFamilyIdByTokenHash(oldRefreshTokenHash)
					.ifPresent(
							familyId -> {
								revokeRefreshTokensAndSessionByFamilyId(familyId, now, MESSAGE_REUSE_REASON);
								logWhenReuseDetected(familyId.toString(), clientIp, userAgent);
							});
			throw new UnauthorizedException("Reuse Detected Refresh Token");
		}

		RefreshToken actualRefreshToken = optToken.get();
		sessionService.findSessionByFamilyId(actualRefreshToken.getFamilyId())
				.ifPresent(actualSession -> {
					if (actualSession.isRevoked() ||
                            (actualSession.getRevokedAt() != null &&
                                    actualSession.getRevokedAt().isBefore(now))) {
						throw new UnauthorizedException("Revoked Session");
					}
				});
		validateRefreshToken(clientId, actualRefreshToken, now, clientIp, userAgent);

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
		newRefreshToken.setCreatedAt(now);
		String jwtRefreshToken = manager.createRefreshToken(newRefreshToken);
		String refreshTokenHash = manager.createRefreshTokenHash(jwtRefreshToken);
		newRefreshToken.setTokenHash(refreshTokenHash);
		RefreshToken savedRefreshToken = dao.save(newRefreshToken);
		actualRefreshToken.setReplacedBy(savedRefreshToken);
		dao.save(actualRefreshToken);
		String accessToken =
				manager.createAccessToken(new UserPrincipal(newRefreshToken.getUser()));
		return new AuthResponseDTO(accessToken, jwtRefreshToken);
	}

	@Transactional(isolation = Isolation.READ_COMMITTED)
	@Override
	public AuthResponseDTO generateJWTTokens(Session session) {

		RefreshToken refreshToken = new RefreshToken(session, uuidv7Generator.generate(),
				session.getCreatedAt().plusMillis(manager.getJwtTimeExpRefresh()), session.getCreatedAt());
		String jwtRefreshToken = manager.createRefreshToken(refreshToken);
		String refreshTokenHash = manager.createRefreshTokenHash(jwtRefreshToken);
		refreshToken.setTokenHash(refreshTokenHash);
		dao.save(refreshToken);
		String accessToken = manager.createAccessToken(new UserPrincipal(refreshToken.getUser()));
		return new AuthResponseDTO(accessToken, jwtRefreshToken);
	}

	private void validateRefreshToken(
			String clientId, RefreshToken token, Instant now, String clientIp, String userAgent) {
		if (token.getFamilyId() == null) {
			throw new BadRequestException("RefreshToken without familyId");
		}
		if (token.getRevoked()) {
			if (token.getReplacedBy() != null) {
				revokeRefreshTokensAndSessionByFamilyId(token.getFamilyId(), now, MESSAGE_REUSE_REASON);
				logWhenReuseDetected(token.getFamilyId().toString(), clientIp, userAgent);
				throw new UnauthorizedException("Reuse Detected Refresh Token");
			} else {
				throw new UnauthorizedException("Revoked Refresh Token");
			}
		}
		if (token.getExpiresAt().isBefore(now)) {
			throw new UnauthorizedException("Expired Refresh Token");
		}
		if (token.getUser() != null
				&& token.getUser().getTokensInvalidBefore() != null
				&& token.getCreatedAt() != null
				&& token.getCreatedAt().isBefore(token.getUser().getTokensInvalidBefore())) {
			throw new UnauthorizedException("Refresh Token issued before tokens_invalid_before");
		}
		if (!Objects.equals(token.getClientId(), clientId)) {
			revokeRefreshTokensAndSessionByFamilyId(token.getFamilyId(), now, "client-mismatch");
			logWhenReuseDetected(token.getFamilyId().toString(), clientIp, userAgent);
			throw new UnauthorizedException("Client mismatch");
		}
		if (!Objects.equals(token.getUserAgent(), userAgent)) {
			revokeRefreshTokensAndSessionByFamilyId(token.getFamilyId(), now, "ua-mismatch");
			throw new UnauthorizedException("User-Agent mismatch");
		}
	}

	@Override
	@Transactional
	public void revokeRefreshTokensAndSessionByFamilyId(UUID familyId, Instant now, String reason) {
		dao.revokeFamilyWithReason(familyId, now, reason);
		sessionService.revokeSessionByFamilyId(familyId, now, reason);
	}

	private void logWhenReuseDetected(String familyId, String clientIp, String userAgent) {
		log.warn(
				"Refresh token reuse detected. familyId={}, ip={}, userAgent={}",
				familyId,
				clientIp,
				userAgent);
	}
}
