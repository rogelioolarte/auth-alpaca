package com.alpaca.persistence.impl;

import com.alpaca.entity.Advertiser;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IAdvertiserDAO;
import com.alpaca.repository.AdvertiserRepo;
import com.alpaca.repository.GenericRepo;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementation of the {@link IAdvertiserDAO} interface for managing {@link Advertiser} entities.
 * Extends the generic DAO implementation ({@link GenericDAOImpl}) to provide standard CRUD
 * operations and advertiser-specific persistence logic.
 *
 * <p>Registered as a Spring component, with constructor injection of the {@link AdvertiserRepo}.
 */
@Component
@RequiredArgsConstructor
public class AdvertiserDAOImpl extends GenericDAOImpl<Advertiser, UUID> implements IAdvertiserDAO {

    private final AdvertiserRepo repo;

    /**
     * Provides the repository used by the generic DAO framework.
     *
     * @return the {@link GenericRepo} for {@link Advertiser}
     */
    @Override
    protected GenericRepo<Advertiser, UUID> getRepo() {
        return repo;
    }

    /**
     * Returns the {@link Advertiser} entity class managed by this DAO.
     *
     * @return {@code Advertiser.class}
     */
    @Override
    protected Class<Advertiser> getEntity() {
        return Advertiser.class;
    }

    /**
     * Updates an existing {@link Advertiser} identified by the given ID with non-null and non-blank
     * values from the provided {@code advertiser} object. Only changed fields are applied. Throws a
     * {@link NotFoundException} if no matching entity is found.
     *
     * @param advertiser the advertiser object containing updated values
     * @param id the unique identifier of the advertiser to update
     * @return the updated and saved {@link Advertiser} instance
     * @throws NotFoundException if no advertiser exists with the specified ID
     */
    @Override
    public Advertiser updateById(Advertiser advertiser, UUID id) {
        Advertiser existing =
                findById(id)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                String.format(
                                                        "%s with ID %s not found",
                                                        getEntity().getName(), id.toString())));

        if (advertiser.getTitle() != null && !advertiser.getTitle().isBlank()) {
            existing.setTitle(advertiser.getTitle());
        }
        if (advertiser.getDescription() != null && !advertiser.getDescription().isBlank()) {
            existing.setDescription(advertiser.getDescription());
        }
        if (advertiser.getAvatarUrl() != null && !advertiser.getAvatarUrl().isBlank()) {
            existing.setAvatarUrl(advertiser.getAvatarUrl());
        }
        if (advertiser.getBannerUrl() != null && !advertiser.getBannerUrl().isBlank()) {
            existing.setBannerUrl(advertiser.getBannerUrl());
        }
        if (advertiser.getPublicLocation() != null && !advertiser.getPublicLocation().isBlank()) {
            existing.setPublicLocation(advertiser.getPublicLocation());
        }
        if (advertiser.getPublicUrlLocation() != null
                && !advertiser.getPublicUrlLocation().isBlank()) {
            existing.setPublicUrlLocation(advertiser.getPublicUrlLocation());
        }
        if (advertiser.isIndexed() != existing.isIndexed()) {
            existing.setIndexed(advertiser.isIndexed());
        }
        if (advertiser.getUser() != null && advertiser.getUser().getId() != null) {
            existing.setUser(advertiser.getUser());
        }

        return repo.save(existing);
    }

    /**
     * Determines whether an advertiser already exists based on the associated user ID.
     *
     * @param advertiser the advertiser object to check; its user must be non-null and have a
     *     non-null ID
     * @return {@code true} if an advertiser exists for the given user ID; {@code false} otherwise
     */
    @Override
    public boolean existsByUniqueProperties(Advertiser advertiser) {
        if (advertiser.getUser() == null || advertiser.getUser().getId() == null) {
            return false;
        }
        return repo.countByUserId(advertiser.getUser().getId()) > 0L;
    }
}
