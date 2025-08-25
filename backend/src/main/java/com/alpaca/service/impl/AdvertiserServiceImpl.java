package com.alpaca.service.impl;

import com.alpaca.entity.Advertiser;
import com.alpaca.persistence.IAdvertiserDAO;
import com.alpaca.persistence.IGenericDAO;
import com.alpaca.service.IAdvertiserService;
import com.alpaca.service.IGenericService;
import java.util.UUID;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
