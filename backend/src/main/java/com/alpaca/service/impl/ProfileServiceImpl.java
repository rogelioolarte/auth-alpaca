package com.alpaca.service.impl;

import com.alpaca.entity.Profile;
import com.alpaca.persistence.IGenericDAO;
import com.alpaca.persistence.IProfileDAO;
import com.alpaca.service.IProfileService;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl extends GenericServiceImpl<Profile, UUID>
    implements IProfileService {

  private final IProfileDAO dao;

  @Override
  @Generated
  protected IGenericDAO<Profile, UUID> getDAO() {
    return dao;
  }

  @Override
  @Generated
  protected String getEntityName() {
    return "Profile";
  }
}
