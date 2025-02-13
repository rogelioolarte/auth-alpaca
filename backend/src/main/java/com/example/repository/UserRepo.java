package com.example.repository;

import com.example.entity.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link User} entities.
 * <p>
 * Extends {@link GenericRepo} to inherit common CRUD operations and
 * defines additional queries for user-specific operations.
 * </p>
 *
 * @see GenericRepo
 */
@Repository
public interface UserRepo extends GenericRepo<User, UUID> {

    /**
     * Retrieves a user by their email address.
     *
     * @param email The email address of the user - must not be null.
     * @return An {@link Optional} containing the user if found, otherwise empty.
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether a user with the specified email address exists.
     *
     * @param email The email address to check - must not be null.
     * @return {@code true} if a user with the given email exists, {@code false} otherwise.
     */
    boolean existsByEmail(String email);

}
