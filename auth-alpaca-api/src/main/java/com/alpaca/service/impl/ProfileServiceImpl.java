package com.alpaca.service.impl;

import com.alpaca.entity.Profile;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IGenericDAO;
import com.alpaca.persistence.IProfileDAO;
import com.alpaca.service.IGenericService;
import com.alpaca.service.IProfileService;
import java.util.Objects;
import java.util.UUID;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer implementation for managing {@link Profile} entities. Inherits common CRUD
 * operations from {@link IGenericService}.
 *
 * <p>This service delegates persistence operations to the {@link IProfileDAO} and provides a clear
 * abstraction for any future business logic related to profiles.
 *
 * @see IGenericService
 * @see IProfileService
 */
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl extends GenericServiceImpl<Profile, UUID>
        implements IProfileService {

    private final IProfileDAO dao;

    /**
     * Provides the generic DAO used by inherited service methods.
     *
     * @return the {@link IGenericDAO} implementation for {@link Profile}
     */
    @Override
    @Generated
    protected IGenericDAO<Profile, UUID> getDAO() {
        return dao;
    }

    /**
     * Supplies a human-readable name representing the entity, used in exception messages and
     * logging.
     *
     * @return the string literal "Profile"
     */
    @Override
    @Generated
    protected String getEntityName() {
        return "Profile";
    }

    /**
     * Creates a new {@link Profile} after verifying that a profile with the same unique properties
     * does not already exist.
     *
     * @param profile the profile to create; must not be {@code null}
     * @return the saved {@link Profile} instance
     * @throws BadRequestException if a profile with identical unique properties already exists
     */
    @Override
    @Transactional
    public Profile save(Profile profile) {
        if (dao.existsByUniqueProperties(profile)) {
            throw new BadRequestException(String.format("%s already exists", getEntityName()));
        }
        return super.save(profile);
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
    @Transactional
    public Profile updateById(Profile profile, UUID id) {
        if (profile == null || id == null)
            throw new BadRequestException(
                    String.format("%s with ID %s cannot be updated", getEntityName(), id));

        Profile existingProfile =
                dao.findById(id)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                String.format(
                                                        "%s with ID %s not found",
                                                        getEntityName(), id)));

        if (profile.getUser() != null && profile.getUser().getId() != null) {
            UUID currentUserId =
                    existingProfile.getUser() != null ? existingProfile.getUser().getId() : null;
            if (!Objects.equals(profile.getUser().getId(), currentUserId)) {
                existingProfile.setUser(profile.getUser());
            }
        }

        updateTextIfExists(
                existingProfile.getFirstName(),
                profile.getFirstName(),
                existingProfile::setFirstName);
        updateTextIfExists(
                existingProfile.getLastName(), profile.getLastName(), existingProfile::setLastName);
        updateTextIfExists(
                existingProfile.getAddress(), profile.getAddress(), existingProfile::setAddress);
        updateTextIfExists(
                existingProfile.getAvatarUrl(),
                profile.getAvatarUrl(),
                existingProfile::setAvatarUrl);

        return super.save(existingProfile);
    }
}
