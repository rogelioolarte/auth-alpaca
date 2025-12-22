package com.alpaca.service.impl;

import com.alpaca.entity.Profile;
import com.alpaca.persistence.IGenericDAO;
import com.alpaca.persistence.IProfileDAO;
import com.alpaca.service.IGenericService;
import com.alpaca.service.IProfileService;
import java.util.UUID;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
