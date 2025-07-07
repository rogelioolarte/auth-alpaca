package com.alpaca.persistence.impl;

import com.alpaca.entity.Permission;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IPermissionDAO;
import com.alpaca.repository.GenericRepo;
import com.alpaca.repository.PermissionRepo;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PermissionDAOImpl extends GenericDAOImpl<Permission, UUID> implements IPermissionDAO {

    private final PermissionRepo repo;

    @Override
    protected GenericRepo<Permission, UUID> getRepo() {
        return repo;
    }

    @Override
    protected Class<Permission> getEntity() {
        return Permission.class;
    }

    @Override
    public Permission updateById(Permission permission, UUID id) {
        Permission existingPermission =
                findById(id)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                String.format(
                                                        "%s with ID %s not found",
                                                        getEntity().getName(), id.toString())));
        if (permission.getPermissionName() != null && !permission.getPermissionName().isBlank()) {
            existingPermission.setPermissionName(permission.getPermissionName());
        }
        return save(existingPermission);
    }

    @Override
    public boolean existsByUniqueProperties(Permission permission) {
        if (permission.getPermissionName() == null || permission.getPermissionName().isBlank())
            return false;
        return repo.existsByPermissionName(permission.getPermissionName());
    }

    @Override
    public Optional<Permission> findByPermissionName(String permissionName) {
        return repo.findByPermissionName(permissionName);
    }
}
