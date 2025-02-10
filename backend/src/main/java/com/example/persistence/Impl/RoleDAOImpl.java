package com.example.persistence.Impl;

import com.example.entity.Role;
import com.example.exception.NotFoundException;
import com.example.persistence.IRoleDAO;
import com.example.repository.GenericRepo;
import com.example.repository.RoleRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RoleDAOImpl extends GenericDAOImpl<Role, UUID> implements IRoleDAO {

    private final RoleRepo repo;

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
    public void deleteById(UUID id) {
        repo.deleteUserRolesByRoleId(id);
        super.deleteById(id);
    }

    @Override
    public Role updateById(Role role, UUID id) {
        Role existingRole = findById(id).orElseThrow(() ->
                new NotFoundException(String.format("%s with ID %s not found",
                        getEntity().getName(), id.toString())));
        if (role.getRoleName() != null && !role.getRoleName().isBlank()) {
            existingRole.setRoleName(role.getRoleName());
        }
        if (role.getRoleDescription() != null && !role.getRoleDescription().isBlank()) {
            existingRole.setRoleDescription(role.getRoleDescription());
        }
        if (role.getPermissions() != null && !role.getPermissions().isEmpty()) {
            existingRole.setPermissions(role.getPermissions());
        }
        return save(existingRole);
    }

    @Override
    public boolean existsByUniqueProperties(Role role) {
        if (role.getRoleName() == null || role.getRoleName().isBlank() ||
                role.getRoleDescription() == null || role.getRoleDescription().isBlank())
            return false;
        return repo.existsByRoleName(role.getRoleName());
    }

    @Override
    public List<Role> findRolesByPermissionId(UUID permissionId) {
        if (permissionId == null) return List.of();
        return repo.findRolesByPermissionId(permissionId);
    }

}
