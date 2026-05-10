package com.alpaca.persistence.impl;

import com.alpaca.entity.User;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IUserDAO;
import com.alpaca.repository.GenericRepo;
import com.alpaca.repository.UserRepo;
import com.alpaca.security.manager.PasswordManager;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Implementation of the {@link IUserDAO} interface for managing {@link User} entities. Extends the
 * generic DAO implementation ({@link GenericDAOImpl}) to provide CRUD and custom user-related
 * persistence operations.
 */
@Component
@RequiredArgsConstructor
public class UserDAOImpl extends GenericDAOImpl<User, UUID> implements IUserDAO {

    private final UserRepo repo;
    private final PasswordManager passwordManager;

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
        if (StringUtils.hasText(user.getPassword())
                && !passwordManager.matches(user.getPassword(), existingUser.getPassword())) {
            existingUser.setPassword(passwordManager.encodePassword(user.getPassword()));
        }

        if (user.getRoles() != null && !user.getRoles().equals(existingUser.getRoles())) {
            existingUser.setUserRoles(user.getRoles());
        }

        if (user.getProfile() != null && user.getProfile().getId() != null) {
            UUID currentUserId =
                    existingUser.getProfile() != null ? existingUser.getProfile().getId() : null;
            if (!Objects.equals(user.getProfile().getId(), currentUserId)) {
                existingUser.setProfile(user.getProfile());
            }
        }

        if (user.getAdvertiser() != null && user.getAdvertiser().getId() != null) {
            UUID currentUserId =
                    existingUser.getAdvertiser() != null
                            ? existingUser.getAdvertiser().getId()
                            : null;
            if (!Objects.equals(user.getAdvertiser().getId(), currentUserId)) {
                existingUser.setAdvertiser(user.getAdvertiser());
            }
        }

        updateIfDifferent(existingUser.isEnabled(), user.isEnabled(), existingUser::setEnabled);
        updateIfDifferent(
                existingUser.isAccountNonLocked(),
                user.isAccountNonLocked(),
                existingUser::setAccountNonLocked);
        updateIfDifferent(
                existingUser.isAccountNonExpired(),
                user.isAccountNonExpired(),
                existingUser::setAccountNonExpired);
        updateIfDifferent(
                existingUser.isCredentialNonExpired(),
                user.isCredentialNonExpired(),
                existingUser::setCredentialNonExpired);
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
    public Optional<User> lockFindUserById(UUID userId) {
        return repo.lockFindUserById(userId);
    }

    /**
     * Verifies whether all entities corresponding to the provided identifiers exist.
     *
     * @param is the collection of IDs to check; may be {@code null}
     * @return {@code true} if the count of matching entities equals the number of IDs provided;
     *     {@code false} otherwise
     */
    @Override
    public boolean existsAllByIds(Collection<UUID> is) {
        return (is.size()) == repo.countByIds(is);
    }
}
