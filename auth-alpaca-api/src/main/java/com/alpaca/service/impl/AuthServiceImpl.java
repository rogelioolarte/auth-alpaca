package com.alpaca.service.impl;

import com.alpaca.dto.request.AuthLoginRequestDTO;
import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.entity.Profile;
import com.alpaca.entity.RefreshToken;
import com.alpaca.entity.Role;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.AuthCode;
import com.alpaca.model.UserPrincipal;
import com.alpaca.security.manager.JJwtManager;
import com.alpaca.security.manager.TokenExchangeManager;
import com.alpaca.security.oauth2.userinfo.OAuth2UserInfo;
import com.alpaca.service.*;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
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
    private final IProfileService profileService;
    private final ISessionService sessionService;
    private final IRefreshTokenService refreshTokenService;
    private final JJwtManager manager;
    private final TokenExchangeManager exchangeManager;
    private final JJwtManager jJwtManager;

    /**
     * Authenticates a user via their pre-authenticated principal and device fingerprint, creating a
     * new session and issuing JWT tokens.
     *
     * <p>This method is called after Spring Security has already validated the user's credentials.
     * The {@link UserPrincipal} is expected to be resolved beforehand.
     *
     * @param userPrincipal the already-authenticated user's principal
     * @param requestDTO the login request containing device context (user-agent, client-id,
     *     client-ip)
     * @return {@link AuthResponseDTO} containing the JWT access and refresh tokens
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

    /**
     * Authenticates via an OAuth2 authorization code exchange (PKCE flow). Validates the code,
     * code-verifier, redirect URI, and all device-fingerprint fields before issuing tokens.
     *
     * <p>Each validation failure throws a generic {@link UnauthorizedException} with the message
     * "Code Invalid or Expired" to prevent attackers from distinguishing <em>which</em> check
     * failed.
     *
     * @param authCode the authorization code request containing code, verifier, redirect URI, and
     *     device context
     * @return {@link AuthResponseDTO} containing the JWT access and refresh tokens
     * @throws UnauthorizedException if any validation check fails
     * @throws BadRequestException if the code-verifier format is invalid
     */
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
     * <p>Before persisting, validates that no user exists with the same email address. A session is
     * created for the device fingerprint embedded in the request DTO, and JWT tokens are issued.
     *
     * @param requestDTO the registration request containing email, password, and device context —
     *     must not be null
     * @return {@link AuthResponseDTO} for the newly registered user
     * @throws BadRequestException if the email is already registered
     */
    @Override
    @Transactional
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

    /**
     * Logs out the user by revoking the refresh token and its entire token family, then clearing
     * the Spring Security context.
     *
     * <p>The refresh token is first validated (which triggers revocation on any mismatch), then the
     * entire token family and associated session are revoked with reason {@code "logout-session"}.
     *
     * @param refreshToken the raw refresh token string to revoke
     * @param clientId the client identifier for token validation
     * @param userAgent the HTTP User-Agent for token validation
     * @param ipAddress the request IP for audit logging
     * @throws BadRequestException if the refresh token is blank
     * @throws NotFoundException if the token is not found in the database
     * @throws UnauthorizedException if token validation fails
     */
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

    /**
     * Registers or updates a user based on OAuth2 provider information.
     *
     * <p>If a user with the email already exists, their OAuth2 connection and email verification
     * are updated via {@link #checkExistingUser(User, boolean)}. Otherwise, a new {@link User} and
     * associated {@link Profile} are created within the same transaction.
     *
     * @param userInfo the OAuth2 provider user information; must not be {@code null}
     * @return the existing or newly registered {@link User}
     * @throws UnauthorizedException if the existing account is disabled or locked
     */
    @Override
    @Transactional
    public User registerOAuth2User(OAuth2UserInfo userInfo) {
        String email = userInfo.getEmail();
        String firstName = userInfo.getFirstName();
        String lastName = userInfo.getLastName();
        String imageURL = Objects.requireNonNullElse(userInfo.getImageUrl(), "");
        boolean emailVerified = userInfo.getEmailVerified();
        if (userService.existsByEmail(email)) {
            return checkExistingUser(userService.findByEmail(email), emailVerified);
        } else {
            Set<Role> userRoles = roleService.getUserRoles();
            User user = new User(email, null, emailVerified, true, userRoles);
            Profile profile = new Profile(firstName, lastName, "", imageURL, null);
            User newUser = userService.save(user);
            newUser.setRoles(userRoles);
            profile.setUser(newUser);
            profileService.save(profile);
            return newUser;
        }
    }

    /**
     * Updates an existing user's OAuth2 connection status and email verification flag.
     *
     * @param user the existing user
     * @param emailVerified OAuth2-provided email verification status
     * @return updated {@link User}
     * @throws UnauthorizedException if the user account is disabled or locked
     */
    public User checkExistingUser(User user, boolean emailVerified) {
        if (!user.isAllowUser()) {
            throw new UnauthorizedException("The account has been deactivated or blocked");
        }
        if (!user.isGoogleConnected()) {
            user.setGoogleConnected(true);
            userService.save(user);
        }
        if (user.isEmailVerified() != emailVerified) {
            user.setEmailVerified(emailVerified);
            userService.save(user);
        }
        return user;
    }
}
