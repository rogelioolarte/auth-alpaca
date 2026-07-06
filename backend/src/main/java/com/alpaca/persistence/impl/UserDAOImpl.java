package com.alpaca.persistence.impl;

import com.alpaca.entity.User;
import com.alpaca.persistence.IUserDAO;
import com.alpaca.repository.CustomRepo;
import com.alpaca.repository.UserRepo;
import java.util.Optional;
import java.util.UUID;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementation of the {@link IUserDAO} interface for managing {@link User} entities. Extends the
 * generic DAO implementation ({@link GenericDAOImpl}) to provide CRUD and custom user-related
 * persistence operations.
 */
@Component
@RequiredArgsConstructor
public class UserDAOImpl extends GenericDAOImpl<User, UUID> implements IUserDAO {

    private final UserRepo repo;

    /**
     * Provides the repository used by the generic DAO operations.
     *
     * @return the {@link CustomRepo} implementation specific to {@link User}
     */
    @Override
    @Generated
    protected CustomRepo<User, UUID> getRepo() {
        return repo;
    }

    /**
     * Searches for a {@link User} by email.
     *
     * @param email the email address to search for; may be {@code null} or blank
     * @return an {@link Optional} containing the found user, or empty if not found
     */
    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return repo.findByEmail(email);
    }

    /**
     * Determines whether a user already exists in the database based on its unique properties.
     *
     * @param user the user to check; uses its email as the unique property
     * @return {@code true} if a user with the same email exists, {@code false} otherwise
     */
    @Override
    public boolean existsByUniqueProperties(User user) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return false;
        }
        return existsByEmail(user.getEmail());
    }

    /**
     * Checks whether a user exists with the given email.
     *
     * @param email the email to check; may be {@code null} or blank
     * @return {@code true} if a user exists with the specified email, {@code false} otherwise
     */
    @Override
    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return repo.existsByEmail(email);
    }

    /**
     * Retrieves and pessimistically locks the user row for concurrency-safe updates.
     *
     * <p>Delegates to {@link com.alpaca.repository.UserRepo#lockFindUserById(UUID)} which acquires
     * a {@code PESSIMISTIC_WRITE} lock on the matching row, preventing concurrent transactions from
     * reading or modifying it until the lock is released.
     *
     * @param userId the user UUID to lock and retrieve
     * @return An {@link Optional} containing the locked user if found, otherwise empty
     */
    @Override
    public Optional<User> lockFindUserById(UUID userId) {
        return repo.lockFindUserById(userId);
    }
}
