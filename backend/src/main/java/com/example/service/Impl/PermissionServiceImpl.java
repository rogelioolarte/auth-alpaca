package com.example.service.Impl;

import com.example.entity.Permission;
import com.example.persistence.IGenericDAO;
import com.example.persistence.IPermissionDAO;
import com.example.service.IPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl extends GenericServiceImpl<Permission, UUID> implements IPermissionService {

    private final IPermissionDAO dao;

    @Override
    protected IGenericDAO<Permission, UUID> getDAO() {
        return dao;
    }

    @Override
    protected String getEntityName() {
        return "Permission";
    }

}
