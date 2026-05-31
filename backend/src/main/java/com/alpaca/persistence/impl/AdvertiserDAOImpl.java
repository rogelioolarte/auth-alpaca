package com.alpaca.persistence.impl;

import com.alpaca.entity.Advertiser;
import com.alpaca.persistence.IAdvertiserDAO;
import com.alpaca.repository.AdvertiserRepo;
import com.alpaca.repository.CustomRepo;
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
     * @return the {@link CustomRepo} for {@link Advertiser}
     */
    @Override
    @Generated
    protected CustomRepo<Advertiser, UUID> getRepo() {
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
        if (advertiser == null
                || advertiser.getUser() == null
                || advertiser.getUser().getId() == null) {
            return false;
        }
        return repo.countByUserId(advertiser.getUser().getId()) > 0L;
    }

    @Override
    public Page<Advertiser> findAllPageByIndexedTrue(Pageable pageable) {
        return repo.findAllPageByIndexedTrue(pageable);
    }
}
