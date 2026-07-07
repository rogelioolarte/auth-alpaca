package com.alpaca.service.impl;

import com.alpaca.entity.Advertiser;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IAdvertiserDAO;
import com.alpaca.persistence.IGenericDAO;
import com.alpaca.service.IAdvertiserService;
import com.alpaca.service.IGenericService;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

/**
 * Service layer implementation for managing {@link Advertiser} entities. This class extends {@link
 * IGenericService} to inherit standard CRUD operations.
 *
 * <p>All persistence operations are delegated to the {@link IAdvertiserDAO}, providing a clean
 * separation between business logic and data access.
 *
 * @see IGenericService
 * @see IAdvertiserService
 */
@Service
@RequiredArgsConstructor
public class AdvertiserServiceImpl extends GenericServiceImpl<Advertiser, UUID>
        implements IAdvertiserService {

    private final IAdvertiserDAO dao;

    /**
     * Provides the generic DAO used by inherited service methods.
     *
     * @return the {@link IGenericDAO} implementation for {@link Advertiser}
     */
    @Override
    @Generated
    protected IGenericDAO<Advertiser, UUID> getDAO() {
        return dao;
    }

    /**
     * Supplies a human-readable name representing the entity, used in exception messages and
     * logging contexts.
     *
     * @return the string literal "Advertiser"
     */
    @Override
    @Generated
    protected String getEntityName() {
        return "Advertiser";
    }

    /**
     * Creates a new {@link Advertiser} after verifying that an advertiser with the same unique
     * properties does not already exist.
     *
     * @param advertiser the advertiser to create; must not be {@code null}
     * @return the saved {@link Advertiser} instance
     * @throws BadRequestException if an advertiser with identical unique properties already exists
     */
    @Override
    public Advertiser save(Advertiser advertiser) {
        if (dao.existsByUniqueProperties(advertiser)) {
            throw new BadRequestException(String.format("%s already exists", getEntityName()));
        }
        return super.save(advertiser);
    }

    /**
     * Retrieves a paginated subset of advertisers that are marked as indexed, useful for
     * public-facing listings where only approved/indexed advertisers should appear.
     *
     * @param pageable pagination parameters; must not be {@code null}
     * @return a paginated list of indexed advertisers
     */
    @Override
    public Page<Advertiser> findAllByIndexedTrue(Pageable pageable) {
        return dao.findAllByIndexedTrue(pageable);
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
        if (advertiser == null || id == null)
            throw new BadRequestException(
                    String.format("%s with ID %s cannot be updated", getEntityName(), id));

        Advertiser existing =
                dao.findById(id)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                String.format(
                                                        "%s with ID %s not found",
                                                        getEntityName(), id)));

        updateTextIfExists(existing.getTitle(), advertiser.getTitle(), existing::setTitle);
        updateTextIfExists(
                existing.getDescription(), advertiser.getDescription(), existing::setDescription);
        updateTextIfExists(
                existing.getAvatarUrl(), advertiser.getAvatarUrl(), existing::setAvatarUrl);
        updateTextIfExists(
                existing.getBannerUrl(), advertiser.getBannerUrl(), existing::setBannerUrl);
        updateTextIfExists(
                existing.getPublicLocation(),
                advertiser.getPublicLocation(),
                existing::setPublicLocation);
        updateTextIfExists(
                existing.getPublicUrlLocation(),
                advertiser.getPublicUrlLocation(),
                existing::setPublicUrlLocation);
        updateIfDifferent(existing.isIndexed(), advertiser.isIndexed(), existing::setIndexed);

        if (advertiser.getUser() != null && advertiser.getUser().getId() != null) {
            UUID currentUserId = existing.getUser() != null ? existing.getUser().getId() : null;
            if (!Objects.equals(advertiser.getUser().getId(), currentUserId)) {
                existing.setUser(advertiser.getUser());
            }
        }
        return super.save(existing);
    }
}
