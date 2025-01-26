package com.example.service;

import com.example.entity.Role;

import java.util.Set;
import java.util.UUID;

public interface IRoleService extends IGenericService<Role, UUID> {

    Role findByRoleName(String roleName);
    Set<Role> getUserRoles();
}
