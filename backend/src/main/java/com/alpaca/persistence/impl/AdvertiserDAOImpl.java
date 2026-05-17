package com.alpaca.persistence.impl;

import com.alpaca.entity.Advertiser;
import com.alpaca.persistence.IAdvertiserDAO;
import com.alpaca.repository.AdvertiserRepo;
import com.alpaca.repository.GenericRepo;
import java.util.Collection;
import java.util.UUID;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    @Generated
    protected GenericRepo<Advertiser, UUID> getRepo() {
        return repo;
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

    @Override
    public Page<Advertiser> findAllPageByIndexedTrue(Pageable pageable) {
        return repo.findAllPageByIndexedTrue(pageable);
    }
}
