package com.alpaca.service.impl;

import com.alpaca.entity.Permission;
import com.alpaca.persistence.IGenericDAO;
import com.alpaca.persistence.IPermissionDAO;
import com.alpaca.service.IPermissionService;
import java.util.UUID;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl extends GenericServiceImpl<Permission, UUID>
    implements IPermissionService {

  private final IPermissionDAO dao;

  @Override
  @Generated
  protected IGenericDAO<Permission, UUID> getDAO() {
    return dao;
  }

  @Override
  @Generated
  protected String getEntityName() {
    return "Permission";
  }
}
