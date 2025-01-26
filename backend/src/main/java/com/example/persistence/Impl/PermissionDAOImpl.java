package com.example.persistence.Impl;

import com.example.entity.Permission;
import com.example.persistence.IPermissionDAO;
import com.example.repository.GenericRepo;
import com.example.repository.PermissionRepo;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PermissionDAOImpl extends GenericDAOImpl<Permission, UUID> implements IPermissionDAO {

    private final PermissionRepo repo;
    private final EntityManager entityManager;

    @Override
    protected GenericRepo<Permission, UUID> getRepo() {
        return repo;
    }

    @Override
    protected EntityManager getEntityManager() {
        return entityManager;
    }

    @Override
    protected Class<Permission> getEntity() {
        return Permission.class;
    }

    @Override
    public void deleteById(UUID id) {
        entityManager.createNativeQuery(
                "DELETE FROM role_permissions WHERE permission_id = :permissionId")
                .setParameter("permissionId", id).executeUpdate();
        entityManager.createNativeQuery(
                "DELETE FROM permissions WHERE permission_id = :permissionId")
                .setParameter("permissionId", id).executeUpdate();
    }

    @Override
    public Permission updateById(Permission permission, UUID id) {
        if (permission.getPermissionName() == null || permission.getPermissionName().isBlank())
            return null;
        entityManager.createNativeQuery("""
                            UPDATE permissions
                            SET permission_name = :permissionName
                            WHERE permission_id = :permissionId""")
                .setParameter("permissionName", permission.getPermissionName())
                .setParameter("permissionId", id).executeUpdate();
        return findById(id).orElse(null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean existsByUniqueProperties(Permission permission) {
        if (permission.getPermissionName() == null || permission.getPermissionName().isBlank())
            return false;
        return ((Long) entityManager.createNativeQuery("SELECT COUNT(*) FROM permissions" +
                        " WHERE permission_name = :permissionName")
                .setParameter("permissionName", permission.getPermissionName())
                .getResultList().stream().findFirst().orElse(0L)) > 0;
    }

}
