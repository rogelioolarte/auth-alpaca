package com.example.service;

import com.example.entity.User;
import com.example.exception.BadRequestException;
import com.example.exception.NotFoundException;

import java.util.UUID;

/**
 * Service interface for managing {@code User} entities.
 * Extends {@link IGenericService} to inherit common CRUD operations.
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
     * @throws NotFoundException   if the entity is not found.
     */
    User findByEmail(String email);

    /**
     * Registers a new User entity in the system.
     * <p>
     * This method should be used only after verifying that no existing user
     * has the same email by calling {@link #existsByEmail(String email)}.
     * If a user with the given email already exists, an exception must be thrown
     * before calling this method.
     * </p>
     *
     * @param user The user entity to register - must not be null.
     * @return The registered user entity.
     * @throws BadRequestException If the provided user data is null.
     */
    User register(User user);

    /**
     * Checks if a user exists by their email address.
     *
     * @param email The email address to check - must not be null.
     * @return {@code true} if a user with the given email exists, otherwise {@code false}.
     */
    boolean existsByEmail(String email);
}
