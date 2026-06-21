package com.alpaca.service.impl;

import com.alpaca.dto.request.AuthLoginRequestDTO;
import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.entity.RefreshToken;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.AuthCode;
import com.alpaca.model.UserPrincipal;
import com.alpaca.security.manager.JJwtManager;
import com.alpaca.security.manager.TokenExchangeManager;
import com.alpaca.service.*;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Implementation of {@link IAuthService}, handling authentication, user registration, and OAuth2
 * login flows within a Spring Security context.
 *
 * @see IAuthService
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final IRoleService roleService;
    private final IUserService userService;
    private final ISessionService sessionService;
    private final IRefreshTokenService refreshTokenService;
    private final JJwtManager manager;
    private final TokenExchangeManager exchangeManager;
    private final JJwtManager jJwtManager;

    /**
     * Authenticates a user using email and password and returns a JWT token wrapped in DTO.
     *
     * @return {@link AuthResponseDTO} containing the JWT token
     * @throws NotFoundException if the email is not registered
     */
    @Override
    public AuthResponseDTO login(UserPrincipal userPrincipal, AuthLoginRequestDTO requestDTO) {
        return refreshTokenService.generateJWTTokens(
                userPrincipal,
                sessionService.createSession(
                        userPrincipal.getUserId(),
                        requestDTO.userAgent(),
                        requestDTO.clientId(),
                        requestDTO.clientIp()));
    }

    @Override
    public AuthResponseDTO login(AuthCode authCode) {
        if (authCode.getCode() == null || authCode.getCode().isEmpty()) {
            throw new UnauthorizedException("Exchange code is required");
        }
        if (!authCode.getCodeVerifier().matches("^[A-Za-z0-9\\-._~]{43,128}$")) {
            throw new BadRequestException("Invalid code-verifier format");
        }
        AuthCode savedAuthCode = exchangeManager.consumeCode(authCode.getCode()).orElse(null);

        if (savedAuthCode == null) {
            throw new UnauthorizedException("Code Invalid or Expired");
        }
        String newCodeChallenge = jJwtManager.createTokenHash(authCode.getCodeVerifier());
        if (!savedAuthCode.getCodeChallenge().equals(newCodeChallenge)) {
            throw new UnauthorizedException("Code Invalid or Expired");
        }
        if (savedAuthCode.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Code Invalid or Expired");
        }
        if (!savedAuthCode.getRedirectUri().equals(authCode.getRedirectUri())) {
            throw new UnauthorizedException("Code Invalid or Expired");
        }

        // Optional additional validation userAgent, clientId, ClientIp
         if (!savedAuthCode.getClientId().equals(authCode.getClientId())) {
             throw new UnauthorizedException("Code Invalid or Expired");
         }
         if (!savedAuthCode.getUserAgent().equals(authCode.getUserAgent())) {
             throw new UnauthorizedException("Code Invalid or Expired");
         }
         if (!savedAuthCode.getClientIp().equals(authCode.getClientIp())) {
             throw new UnauthorizedException("Code Invalid or Expired");
         }

        return refreshTokenService.generateJWTTokens(savedAuthCode);
    }

    /**
     * Registers a new user, assigns default role, and logs them in returning a JWT token.
     *
     * @return {@link AuthResponseDTO} for the newly registered user
     * @throws BadRequestException if the email is already registered
     */
    @Override
    public AuthResponseDTO register(AuthLoginRequestDTO requestDTO) {
        if (userService.existsByEmail(requestDTO.email())) {
            throw new BadRequestException("Email already registered");
        }
        User user =
                userService.save(
                        new User(
                                requestDTO.email(),
                                requestDTO.password(),
                                roleService.getUserRoles()));
        return login(new UserPrincipal(user), requestDTO);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Override
    public void logout(String refreshToken, String clientId, String userAgent, String ipAddress) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BadRequestException("Invalid Refresh Token");
        }
        Instant now = Instant.now();

        RefreshToken actualrefreshToken =
                refreshTokenService
                        .findByTokenHashSecure(manager.createTokenHash(refreshToken))
                        .orElseThrow(() -> new NotFoundException("Refresh Token Not Found"));

        refreshTokenService.validateRefreshToken(
                actualrefreshToken, clientId, now, ipAddress, userAgent);

        refreshTokenService.revokeRefreshTokensAndSessionByFamilyId(
                actualrefreshToken.getFamilyId(), now, "logout-session");
        SecurityContextHolder.clearContext();
    }

    /**
     * Loads a user by username (email), required by Spring Security.
     *
     * @param username user's email
     * @return {@link UserDetails} for authentication
     * @throws UsernameNotFoundException if user is not found
     */
    @Override
    @NonNull
    public UserDetails loadUserByUsername(@NonNull String username) {
        return new UserPrincipal(userService.findByEmail(username));
    }
}
