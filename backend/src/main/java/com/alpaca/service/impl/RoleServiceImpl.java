package com.alpaca.service.impl;

import com.alpaca.entity.Role;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IGenericDAO;
import com.alpaca.persistence.IRoleDAO;
import com.alpaca.service.IRoleService;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends GenericServiceImpl<Role, UUID> implements IRoleService {

  private final IRoleDAO dao;

  @Override
  @Generated
  protected IGenericDAO<Role, UUID> getDAO() {
    return dao;
  }

  @Override
  @Generated
  protected String getEntityName() {
    return "Role";
  }

  @Override
  public Set<Role> getUserRoles() {
    Set<Role> roles = new HashSet<>();
    roles.add(findByRoleName("USER"));
    return roles;
  }

  @Transactional
  @Override
  public Role findByRoleName(String roleName) {
    if (roleName == null || roleName.isBlank())
      throw new BadRequestException(String.format("%s cannot be found", getEntityName()));
    return dao.findByRoleName(roleName)
        .orElseThrow(
            () ->
                new NotFoundException(
                    String.format("%s with Name %s not found", getEntityName(), roleName)));
  }
}
