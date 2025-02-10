package com.example.service;

import com.example.dto.request.AuthRequestDTO;
import com.example.dto.response.AuthResponseDTO;
import com.example.exception.BadRequestException;
import com.example.exception.NotFoundException;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Service interface for authentication operations.
 * <p>
 * This interface provides methods for user authentication and registration.
 * </p>
 */
public interface IAuthService extends UserDetailsService {

    /**
     * Authenticates a user based on the provided credentials.
     *
     * @param requestDTO The authentication request containing user credentials - must not be null.
     * @return The authentication response containing the token.
     * @throws BadRequestException if the credentials of the user are invalid.
     * @throws NotFoundException   if the user is not found.
     */
    AuthResponseDTO login(AuthRequestDTO requestDTO);

    /**
     * Registers a new user in the system.
     * <p>
     * In this method should be verifying that no existing user
     * has the same email by calling the appropriate validation method.
     * If a user with the given email already exists, an exception must be thrown
     * within this method.
     * </p>
     *
     * @param requestDTO The registration request containing user details - must not be null.
     * @return The authentication response containing the token.
     * @throws BadRequestException If a user with their unique field already exists.
     */
    AuthResponseDTO register(AuthRequestDTO requestDTO);
}
