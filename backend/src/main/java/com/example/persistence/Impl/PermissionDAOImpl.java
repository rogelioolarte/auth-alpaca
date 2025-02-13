package com.example.persistence.Impl;

import com.example.entity.Permission;
import com.example.exception.NotFoundException;
import com.example.persistence.IPermissionDAO;
import com.example.repository.GenericRepo;
import com.example.repository.PermissionRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

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
        if (permission.getPermissionName() == null || permission.getPermissionName().isBlank())
            return null;
        Permission existingPermission = findById(id).orElseThrow(() -> new NotFoundException(
                String.format("%s with ID %s not found",
                        getEntity().getName(), id.toString())));
        if(permission.getPermissionName() != null && !permission.getPermissionName().isBlank()) {
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

}
