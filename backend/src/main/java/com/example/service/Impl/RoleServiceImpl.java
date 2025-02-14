package com.example.service.impl;

import com.example.entity.Role;
import com.example.exception.BadRequestException;
import com.example.exception.NotFoundException;
import com.example.persistence.IGenericDAO;
import com.example.persistence.IRoleDAO;
import com.example.service.IRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends GenericServiceImpl<Role, UUID> implements IRoleService {

    private final IRoleDAO dao;
    private Set<Role> userRoles;

    @Override
    protected IGenericDAO<Role, UUID> getDAO() {
        return dao;
    }

    @Override
    protected String getEntityName() {
        return "Role";
    }

    @Override
    public Set<Role> getUserRoles() {
        return Set.of(findByRoleName("USER"));
    }

    @Transactional
    @Override
    public Role findByRoleName(String roleName) {
        if (roleName == null) throw new BadRequestException(
                String.format("%s cannot be found", getEntityName()));
        return dao.findByRoleName(roleName).orElseThrow(() -> new NotFoundException(
                String.format("%s with Name %s not found", getEntityName(), roleName)));
    }

}
