package com.alpaca.persistence.impl;

import com.alpaca.entity.User;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IUserDAO;
import com.alpaca.repository.GenericRepo;
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
     * @return the {@link GenericRepo} implementation specific to {@link User}
     */
    @Override
    @Generated
    protected GenericRepo<User, UUID> getRepo() {
        return repo;
    }

    /**
     * Returns the entity class managed by this DAO.
     *
     * @return the {@code Class} object for {@link User}
     */
    @Override
    @Generated
    protected Class<User> getEntity() {
        return User.class;
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
     * Updates an existing {@link User} identified by the given ID with the non-null and non-blank
     * properties provided in the supplied {@code user} object. Only fields that are different,
     * non-null, and non-blank are updated. Throws a {@link NotFoundException} if no user with the
     * specified ID exists.
     *
     * @param user user object containing updated values
     * @param id the unique identifier of the user to update
     * @return the updated and saved {@link User} instance
     * @throws NotFoundException if no existing user is found with the given ID
     */
    @Override
    public User updateById(User user, UUID id) {
        User existingUser =
                findById(id)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                String.format(
                                                        "%s with ID %s not found",
                                                        getEntity().getName(), id.toString())));

        updateTextIfExists(existingUser.getEmail(), user.getEmail(), existingUser::setEmail);
        updateTextIfExists(
                existingUser.getPassword(), user.getPassword(), existingUser::setPassword);

        if (user.getRoles() != null
                && !user.getRoles().isEmpty()
                && !existingUser.getRoles().equals(user.getRoles())) {
            existingUser.setUserRoles(user.getRoles());
        }
        if (user.getProfile() != null
                && user.getProfile().getId() != null
                && !existingUser.getProfile().getId().equals(user.getProfile().getId())) {
            existingUser.setProfile(user.getProfile());
        }
        if (user.getAdvertiser() != null
                && user.getAdvertiser().getId() != null
                && !existingUser.getAdvertiser().getId().equals(user.getAdvertiser().getId())) {
            existingUser.setAdvertiser(user.getAdvertiser());
        }
        updateIfDifferent(existingUser.isEnabled(), user.isEnabled(), existingUser::setEnabled);
        updateIfDifferent(
                existingUser.isAccountNoLocked(),
                user.isAccountNoLocked(),
                existingUser::setAccountNoLocked);
        updateIfDifferent(
                existingUser.isAccountNoExpired(),
                user.isAccountNoExpired(),
                existingUser::setAccountNoExpired);
        updateIfDifferent(
                existingUser.isCredentialNoExpired(),
                user.isCredentialNoExpired(),
                existingUser::setCredentialNoExpired);
        updateIfDifferent(
                existingUser.isEmailVerified(),
                user.isEmailVerified(),
                existingUser::setEmailVerified);
        updateIfDifferent(
                existingUser.isGoogleConnected(),
                user.isGoogleConnected(),
                existingUser::setGoogleConnected);
        return save(existingUser);
    }

    /**
     * Determines whether a user already exists in the database based on its unique properties.
     *
     * @param user the user to check; uses its email as the unique property
     * @return {@code true} if a user with the same email exists, {@code false} otherwise
     */
    @Override
    public boolean existsByUniqueProperties(User user) {
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

    @Override
    public Optional<User> findByIdWithAuthorities(UUID id) {
        return repo.findByIdWithAuthorities(id);
    }

    @Override
    public Optional<User> findByEmailWithAuthorities(String email) {
        return repo.findByEmailWithAuthorities(email);
    }

    @Override
    public Optional<User> lockFindUserById(UUID userId) {
        return repo.lockFindUserById(userId);
    }
}
