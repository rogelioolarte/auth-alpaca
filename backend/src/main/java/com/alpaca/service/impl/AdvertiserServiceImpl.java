package com.alpaca.service.impl;

import com.alpaca.entity.Advertiser;
import com.alpaca.persistence.IAdvertiserDAO;
import com.alpaca.persistence.IGenericDAO;
import com.alpaca.service.IAdvertiserService;
import java.util.UUID;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdvertiserServiceImpl extends GenericServiceImpl<Advertiser, UUID>
    implements IAdvertiserService {

  private final IAdvertiserDAO dao;

  @Override
  @Generated
  protected IGenericDAO<Advertiser, UUID> getDAO() {
    return dao;
  }

  @Override
  @Generated
  protected String getEntityName() {
    return "Advertiser";
  }
}
