package com.alpaca.service;

import com.alpaca.dto.response.AuthResponseDTO;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import org.springframework.security.core.userdetails.UserDetailsService;

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
     * @param email The authentication request containing email credential - must not be null.
     * @param password The authentication request containing password credential - must not be null.
     * @return The authentication response containing the token.
     * @throws BadRequestException if the credentials of the user are invalid.
     * @throws NotFoundException if the user is not found.
     */
    AuthResponseDTO login(String email, String password);

    /**
     * Registers a new user in the system.
     *
     * <p>In this method should be verifying that no existing user has the same email by calling the
     * appropriate validation method. If a user with the given email already exists, an exception
     * must be thrown within this method.
     *
     * @param email The authentication request containing email credential - must not be null.
     * @param password The authentication request containing password credential - must not be null.
     * @return The authentication response containing the token.
     * @throws BadRequestException If a user with their unique field already exists.
     */
    AuthResponseDTO register(String email, String password);
}
