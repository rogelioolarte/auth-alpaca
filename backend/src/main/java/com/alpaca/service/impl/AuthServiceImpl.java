package com.alpaca.service.impl;

import com.alpaca.dto.request.AuthLoginRequestDTO;
import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.security.manager.PasswordManager;
import com.alpaca.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

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
	private final PasswordManager passwordManager;

	/**
	 * Sets the Spring Security context with the provided authentication object.
	 *
	 * @param authentication the authentication object; must not be {@code null}
	 * @throws UnauthorizedException if the authentication object is {@code null}
	 */
	public void setSecurityContextBefore(Authentication authentication) {
		if (authentication == null) {
			throw new UnauthorizedException("The account has been deactivated or blocked");
		}
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	/**
	 * Authenticates a user using email and password and returns a JWT token wrapped in DTO.
	 *
	 * @return {@link AuthResponseDTO} containing the JWT token
	 * @throws NotFoundException if the email is not registered
	 */
	@Override
	public AuthResponseDTO login(UUID userId, AuthLoginRequestDTO requestDTO) {
		return refreshTokenService.generateJWTTokens(
				sessionService.createSession(
						userId,
						requestDTO.userAgent(),
						requestDTO.clientId(),
						requestDTO.clientIp()));
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
        return login(userService.register(
                        new User(requestDTO.email(),
                                passwordManager.encodePassword(requestDTO.password()),
                                roleService.getUserRoles())).getId(), requestDTO);
    }

	/**
	 * Loads a user by username (email), required by Spring Security.
	 *
	 * @param username user's email
	 * @return {@link UserDetails} for authentication
	 * @throws UsernameNotFoundException if user is not found
	 */
	@Override
	public UserDetails loadUserByUsername(String username) {
		return new UserPrincipal(userService.findByEmail(username));
	}

	/**
	 * Validates raw password against stored user details and enforces account status checks.
	 *
	 * @param rawPassword the entered password
	 * @param userDetails stored user details
	 * @return authenticated {@link UserDetails}
	 * @throws BadRequestException   if validation fails
	 * @throws UnauthorizedException if account is disabled or locked
	 */
	public UserDetails validateUserDetails(String rawPassword, UserDetails userDetails) {
		if (userDetails == null) {
			throw new BadRequestException("Invalid Username or Password");
		}
		if (rawPassword == null
				|| rawPassword.isBlank()
				|| !passwordManager.matches(rawPassword, userDetails.getPassword())) {
			throw new BadRequestException("Invalid Password");
		}
		if (!(userDetails.isEnabled()
				&& userDetails.isAccountNonLocked()
				&& userDetails.isAccountNonExpired()
				&& userDetails.isCredentialsNonExpired())) {
			throw new UnauthorizedException("The account has been deactivated or blocked");
		}
		return userDetails;
	}

}
