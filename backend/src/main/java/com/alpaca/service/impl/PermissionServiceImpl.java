package com.alpaca.service.impl;

import com.alpaca.entity.Permission;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IGenericDAO;
import com.alpaca.persistence.IPermissionDAO;
import com.alpaca.service.IGenericService;
import com.alpaca.service.IPermissionService;
import java.util.UUID;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Updates an existing {@link Permission} identified by the given ID with the non-null and
     * non-blank values from the provided {@code permission} object. Only changed fields are
     * applied. Throws a {@link NotFoundException} if no matching entity is found.
     *
     * @param permission the permission object containing updated values
     * @param id the unique identifier of the permission to update
     * @return the updated and saved {@link Permission} instance
     * @throws NotFoundException if no permission exists with the specified ID
     */
    @Transactional
    @Override
    public Permission updateById(Permission permission, UUID id) {
        if (permission == null || id == null)
            throw new BadRequestException(
                    String.format("%s with ID %s cannot be updated", getEntityName(), id));

        Permission existingPermission =
                dao.findById(id)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                String.format(
                                                        "%s with ID %s not found",
                                                        getEntityName(), id)));

        updateTextIfExists(
                existingPermission.getName(), permission.getName(), existingPermission::setName);
        return dao.save(existingPermission);
    }
}
