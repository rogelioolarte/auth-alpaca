package com.alpaca.persistence.impl;

import com.alpaca.entity.Profile;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IProfileDAO;
import com.alpaca.repository.GenericRepo;
import com.alpaca.repository.ProfileRepo;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementation of the {@link IProfileDAO} interface for managing {@link Profile} entities.
 * Extends the generic DAO implementation ({@link GenericDAOImpl}) to provide standard CRUD
 * operations and custom persistence logic specific to Profile entities.
 */
@Component
@RequiredArgsConstructor
public class ProfileDAOImpl extends GenericDAOImpl<Profile, UUID> implements IProfileDAO {

    private final ProfileRepo repo;

    /**
     * Provides the repository used by the generic DAO framework.
     *
     * @return the {@link GenericRepo} for {@link Profile}
     */
    @Override
    protected GenericRepo<Profile, UUID> getRepo() {
        return repo;
    }

    /**
     * Returns the class object representing the {@link Profile} entity managed by this DAO.
     *
     * @return {@code Profile.class}
     */
    @Override
    protected Class<Profile> getEntity() {
        return Profile.class;
    }

    /**
     * Updates an existing {@link Profile} identified by the given ID using non-null and non-blank
     * values from the provided {@code profile} object. Only fields that differ are updated. Throws
     * a {@link NotFoundException} if no matching profile is found.
     *
     * @param profile the profile object containing updated values
     * @param id the unique identifier of the profile to update
     * @return the updated and saved {@link Profile} instance
     * @throws NotFoundException if no profile exists with the specified ID
     */
    @Override
    public Profile updateById(Profile profile, UUID id) {
        Profile existingProfile =
                findById(id)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                String.format(
                                                        "%s with ID %s not found",
                                                        getEntity().getName(), id)));

        if (profile.getFirstName() != null && !profile.getFirstName().isBlank()) {
            existingProfile.setFirstName(profile.getFirstName());
        }
        if (profile.getLastName() != null && !profile.getLastName().isBlank()) {
            existingProfile.setLastName(profile.getLastName());
        }
        if (profile.getAddress() != null && !profile.getAddress().isBlank()) {
            existingProfile.setAddress(profile.getAddress());
        }
        if (profile.getAvatarUrl() != null && !profile.getAvatarUrl().isBlank()) {
            existingProfile.setAvatarUrl(profile.getAvatarUrl());
        }
        if (profile.getUser() != null && profile.getUser().getId() != null) {
            existingProfile.setUser(profile.getUser());
        }

        return save(existingProfile);
    }

    /**
     * Determines whether a profile already exists based on its unique property: the associated user
     * ID.
     *
     * @param profile the profile to check; must include a user with a non-null ID
     * @return {@code true} if a profile exists for the given user ID; {@code false} otherwise
     */
    @Override
    public boolean existsByUniqueProperties(Profile profile) {
        if (profile.getUser() == null || profile.getUser().getId() == null) {
            return false;
        }
        return repo.countByUserId(profile.getUser().getId()) > 0L;
    }
}
