package com.alpaca.service.impl;

import com.alpaca.entity.Permission;
import com.alpaca.persistence.IGenericDAO;
import com.alpaca.persistence.IPermissionDAO;
import com.alpaca.service.IGenericService;
import com.alpaca.service.IPermissionService;
import java.util.UUID;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service layer implementation for managing {@link Permission} entities. Inherits common CRUD
 * operations from {@link IGenericService}.
 *
 * <p>This service delegates persistence operations to the {@link IPermissionDAO} and provides a
 * clear abstraction point for future business logic related to permissions.
 *
 * @see IGenericService
 * @see IPermissionService
 */
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl extends GenericServiceImpl<Permission, UUID>
        implements IPermissionService {

    private final IPermissionDAO dao;

    /**
     * Provides the generic DAO used by inherited service methods.
     *
     * @return the {@link IGenericDAO} implementation for {@link Permission}
     */
    @Override
    @Generated
    protected IGenericDAO<Permission, UUID> getDAO() {
        return dao;
    }

    /**
     * Supplies a human-readable name representing the entity, used in exception messages and
     * logging.
     *
     * @return the string literal "Permission"
     */
    @Override
    @Generated
    protected String getEntityName() {
        return "Permission";
    }
}
