package com.example.persistence;

import com.example.entity.Role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IRoleDAO extends IGenericDAO<Role, UUID> {

    List<Role> findRolesByPermissionId(UUID permissionId);
    Optional<Role> findByRoleName(String roleName);
}
