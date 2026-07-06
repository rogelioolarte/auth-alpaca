package com.alpaca.service;

import com.alpaca.dto.request.PasswordRequestDTO;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.model.UserPrincipal;
import java.util.UUID;

/**
 * Service interface for managing {@link User} entities. Extends {@link IGenericService} to inherit
 * common CRUD operations.
 *
 * @see IGenericService
 */
public interface IUserService extends IGenericService<User, UUID> {

    /**
     * Finds a user by their email address.
     *
     * @param email The email address of the user - must not be null.
     * @return The {@code User} entity if found.
     * @throws BadRequestException if the email is null.
     * @throws NotFoundException if the entity is not found.
     */
    User findByEmail(String email);

    /**
     * Registers a new User entity in the system.
     *
     * <p>This method should be used only after verifying that no existing user has the same email
     * by calling {@link #existsByEmail(String email)}. If a user with the given email already
     * exists, an exception must be thrown before calling this method.
     *
     * @param user The user entity to register - must not be null.
     * @return The registered user entity.
     * @throws BadRequestException If the provided user data is null.
     */
    @Override
    User save(User user);

    /**
     * Checks if a user exists by their email address.
     *
     * @param email The email address to check - must not be null.
     * @return {@code true} if a user with the given email exists, otherwise {@code false}.
     */
    boolean existsByEmail(String email);

    /**
     * Changes the password for the authenticated user.
     *
     * <p>Validates the current password before applying the new one. This operation also revokes
     * existing sessions for the user, forcing re-authentication after a password change.
     *
     * @param principal the currently authenticated user principal
     * @param requestDTO the DTO containing current and new password data
     * @throws BadRequestException if the current password is null, empty, or incorrect
     */
    void changePassword(UserPrincipal principal, PasswordRequestDTO requestDTO);
}
