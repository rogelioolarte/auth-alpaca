package com.alpaca.persistence.impl;

import com.alpaca.entity.Role;
import com.alpaca.persistence.IRoleDAO;
import com.alpaca.repository.CustomRepo;
import com.alpaca.repository.RoleRepo;
import java.util.Optional;
import java.util.UUID;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementation of the {@link IRoleDAO} interface for managing {@link Role} entities. This class
 * extends the generic DAO implementation ({@link GenericDAOImpl}) to provide standard CRUD
 * operations along with custom role-specific persistence logic.
 */
@Component
@RequiredArgsConstructor
public class RoleDAOImpl extends GenericDAOImpl<Role, UUID> implements IRoleDAO {

    private final RoleRepo repo;

    /**
     * Provides the specific repository used by the generic DAO system.
     *
     * @return the {@link CustomRepo} implementation for {@link Role}
     */
    @Override
    @Generated
    protected CustomRepo<Role, UUID> getRepo() {
        return repo;
    }

    /**
     * Retrieves a {@link Role} entity by its role name.
     *
     * @param roleName the name of the role to search for; may be {@code null} or blank
     * @return an {@link Optional} containing the found role, or empty if not found or invalid input
     */
    @Override
    public Optional<Role> findByRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return Optional.empty();
        }
        return repo.findByName(roleName);
    }

    /**
     * Determines whether a role already exists based on its unique properties. Both role name and
     * description are required to be non-null and non-blank to perform the check.
     *
     * @param role the role object whose uniqueness is to be verified
     * @return {@code true} if a role with the same name exists; {@code false} otherwise or if input
     *     is invalid
     */
    @Override
    public boolean existsByUniqueProperties(Role role) {
        if (role.getName() == null
                || role.getName().isBlank()
                || role.getDescription() == null
                || role.getDescription().isBlank()) {
            return false;
        }
        return repo.existsByName(role.getName());
    }
}
