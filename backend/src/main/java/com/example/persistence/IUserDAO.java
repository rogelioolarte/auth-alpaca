package com.example.persistence;

import com.example.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data Access Object (DAO) interface for managing {@link User} entities.
 * <p>
 * Extends {@link IGenericDAO} to inherit common CRUD operations and
 * defines additional queries specific to {@code User} management.
 * </p>
 *
 * @see IGenericDAO
 */
public interface IUserDAO extends IGenericDAO<User, UUID> {

    /**
     * Retrieves a list of users associated with a specific role.
     *
     * @param id The unique identifier of the role - must not be null.
     * @return A list of users who have the specified role. Returns an empty list if no users are found.
     */
    List<User> findUsersByRoleId(UUID id);

    /**
     * Finds a user by their email address.
     *
     * @param email The email address of the user - must not be null.
     * @return An {@link Optional} containing the user if found, otherwise an empty {@link Optional}.
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether a user with the specified email exists.
     *
     * @param email The email address to check - must not be null.
     * @return {@code true} if a user with the given email exists, {@code false} otherwise.
     */
    boolean existsByEmail(String email);
}
