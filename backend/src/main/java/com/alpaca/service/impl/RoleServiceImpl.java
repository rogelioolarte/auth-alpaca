package com.alpaca.service.impl;

import com.alpaca.entity.Role;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IGenericDAO;
import com.alpaca.persistence.IRoleDAO;
import com.alpaca.service.IGenericService;
import com.alpaca.service.IRoleService;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer implementation for managing {@link Role} entities and encapsulating business logic
 * specific to roles while inheriting basic CRUD operations from {@link IGenericService}.
 *
 * <p>Includes specialized methods for fetching user-default roles and retrieving roles by name,
 * with validation and error handling for invalid inputs or missing entities.
 *
 * @see IGenericService
 * @see IRoleService
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends GenericServiceImpl<Role, UUID> implements IRoleService {

    private final IRoleDAO dao;

    /**
     * Provides the generic DAO used by inherited service methods.
     *
     * @return the {@link IGenericDAO} implementation for {@link Role}
     */
    @Override
    @Generated
    protected IGenericDAO<Role, UUID> getDAO() {
        return dao;
    }

    /**
     * Supplies a user-friendly name representing the entity, used in exception messages and logs.
     *
     * @return the string literal "Role"
     */
    @Override
    @Generated
    protected String getEntityName() {
        return "Role";
    }

    /**
     * Retrieves the default set of roles assigned to users. Currently configured to always return a
     * set containing the "USER" role.
     *
     * @return a {@link Set} containing the "USER" {@link Role}
     * @throws BadRequestException if the default role cannot be found
     */
    @Override
    public Set<Role> getUserRoles() {
        Set<Role> roles = new HashSet<>();
        roles.add(findByRoleName("USER"));
        return roles;
    }

    /**
     * Finds a {@link Role} by its name.
     *
     * @param roleName the name of the role to find; must not be {@code null} or blank
     * @return the found {@link Role} instance
     * @throws BadRequestException if the provided role name is {@code null} or blank
     * @throws NotFoundException if no role is found with the given name
     */
    @Transactional
    @Override
    public Role findByRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            throw new BadRequestException(String.format("%s cannot be found", getEntityName()));
        }
        return dao.findByRoleName(roleName)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        String.format(
                                                "%s with Name %s not found",
                                                getEntityName(), roleName)));
    }
}
