package com.alpaca.service;

import com.alpaca.dto.request.AuthLoginRequestDTO;
import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.model.AuthCode;
import com.alpaca.model.UserPrincipal;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Service interface for authentication operations.
 *
 * <p>This interface provides methods for user authentication and registration. Extends {@link
 * UserDetailsService } to loads user-specific data.
 */
public interface IAuthService extends UserDetailsService {

    /**
     * Authenticates a user based on the provided credentials.
     *
     * @return The authentication response containing the token.
     * @throws BadRequestException if the credentials of the user are invalid.
     * @throws NotFoundException if the user is not found.
     */
    AuthResponseDTO login(UserPrincipal userPrincipal, AuthLoginRequestDTO requestDTO);

    /**
     * Authenticates using an OAuth2 authorization code and issues JWT tokens.
     *
     * <p>This overload is used in the OAuth2 login flow where an {@code AuthCode} has been obtained
     * from an external identity provider. The method exchanges the code for tokens and either finds
     * an existing user or creates a new one based on the provider's profile data.
     *
     * @param authCode the authorization code obtained from the OAuth2 provider — must not be null
     * @return an {@code AuthResponseDTO} containing the issued access and refresh tokens
     * @throws BadRequestException if the authorization code is invalid or expired
     */
    AuthResponseDTO login(AuthCode authCode);

    /**
     * Registers a new user in the system.
     *
     * <p>In this method should be verifying that no existing user has the same email by calling the
     * appropriate validation method. If a user with the given email already exists, an exception
     * must be thrown within this method.
     *
     * @return The authentication response containing the token.
     * @throws BadRequestException If a user with their unique field already exists.
     */
    AuthResponseDTO register(AuthLoginRequestDTO requestDTO);

    /**
     * Logs out the user by revoking the given refresh token and its associated session.
     *
     * @param refreshToken the raw refresh token value to revoke — must not be null
     * @param clientId the OAuth2 client identifier associated with the token
     * @param userAgent the {@code User-Agent} header from the client's request
     * @param ipAddress the IP address of the client making the logout request
     */
    void logout(String refreshToken, String clientId, String userAgent, String ipAddress);

    /**
     * Locates a user by their username (email) for Spring Security authentication.
     *
     * @param username the username (email) identifying the user whose data is required — must not
     *     be null
     * @return a fully populated {@code UserDetails} instance (never {@code null})
     * @throws UsernameNotFoundException if the user could not be found or has no granted
     *     authorities
     */
    @Override
    @NonNull UserDetails loadUserByUsername(@NonNull String username)
            throws UsernameNotFoundException;
}
