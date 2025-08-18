package com.alpaca.persistence.impl;

import com.alpaca.entity.Role;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IRoleDAO;
import com.alpaca.repository.GenericRepo;
import com.alpaca.repository.RoleRepo;
import com.alpaca.repository.intermediate.RolePermissionRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RoleDAOImpl extends GenericDAOImpl<Role, UUID> implements IRoleDAO {

    private final RoleRepo repo;
    private final RolePermissionRepo rolePermissionRepo;

    @Override
    protected GenericRepo<Role, UUID> getRepo() {
        return repo;
    }

    @Override
    protected Class<Role> getEntity() {
        return Role.class;
    }

    @Override
    public Optional<Role> findByRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) return Optional.empty();
        return repo.findByRoleName(roleName);
    }

    @Override
    public Role updateById(Role role, UUID id) {
        Role existingRole =
                findById(id)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                String.format(
                                                        "%s with ID %s not found",
                                                        getEntity().getName(), id.toString())));
        if (role.getRoleName() != null && !role.getRoleName().isBlank()) {
            existingRole.setRoleName(role.getRoleName());
        }
        if (role.getRoleDescription() != null && !role.getRoleDescription().isBlank()) {
            existingRole.setRoleDescription(role.getRoleDescription());
        }
        if (role.getRolePermissions() != null && !role.getRolePermissions().isEmpty()) {
            existingRole.setRolePermissions(role.getPermissions());
        }
        return save(existingRole);
    }

    @Override
    public boolean existsByUniqueProperties(Role role) {
        if (role.getRoleName() == null
                || role.getRoleName().isBlank()
                || role.getRoleDescription() == null
                || role.getRoleDescription().isBlank()) return false;
        return repo.existsByRoleName(role.getRoleName());
    }
}
