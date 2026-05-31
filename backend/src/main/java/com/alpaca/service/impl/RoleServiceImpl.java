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

    @Override
    public Role save(Role role) {
        if (dao.existsByUniqueProperties(role)) {
            throw new BadRequestException(String.format("%s already exists", getEntityName()));
        }
        return super.save(role);
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
        Set<Role> roles = HashSet.newHashSet(1);
        Role userRole =
                dao.findByRoleName("USER")
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                String.format(
                                                        "%s with Name %s not found",
                                                        getEntityName(), "USER")));
        roles.add(userRole);
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

    /**
     * Updates an existing {@link Role} identified by the given ID with the non-null, non-blank
     * properties from the supplied {@code role} object. Only changed fields are applied. Throws
     * {@link NotFoundException} if no existing entity is found.
     *
     * @param role the role object containing updated values
     * @param id the unique identifier of the role to update
     * @return the updated and saved {@link Role} instance
     * @throws NotFoundException if no role exists with the specified ID
     */
    @Override
    public Role updateById(Role role, UUID id) {
        if (role == null || id == null)
            throw new BadRequestException(
                    String.format("%s with ID %s cannot be updated", getEntityName(), id));

        Role existingRole =
                dao.findById(id)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                String.format(
                                                        "%s with ID %s not found",
                                                        getEntityName(), id)));

        updateTextIfExists(existingRole.getName(), role.getName(), existingRole::setName);
        updateTextIfExists(
                existingRole.getDescription(), role.getDescription(), existingRole::setDescription);
        if (role.getRolePermissions() != null
                && !role.getRolePermissions().equals(existingRole.getRolePermissions())) {
            existingRole.setRolePermissions(role.getPermissions());
        }
        return super.save(existingRole);
    }
}
