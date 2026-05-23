package com.alpaca.persistence.impl;

import com.alpaca.entity.Profile;
import com.alpaca.persistence.IProfileDAO;
import com.alpaca.repository.GenericRepo;
import com.alpaca.repository.ProfileRepo;
import java.util.Collection;
import java.util.UUID;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
    @Generated
    protected GenericRepo<Profile, UUID> getRepo() {
        return repo;
    }

    /**
     * Determines whether a profile already exists based on its unique property: the associated user
     * email.
     *
     * @param profile the profile to check; must include a user with a non-null email
     * @return {@code true} if a profile exists for the given user ID; {@code false} otherwise
     */
    @Override
    public boolean existsByUniqueProperties(Profile profile) {
        if (profile.getUser() == null || !StringUtils.hasText(profile.getUser().getEmail())) {
            return false;
        }
        return repo.countByUserEmail(profile.getUser().getEmail()) > 0L;
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
