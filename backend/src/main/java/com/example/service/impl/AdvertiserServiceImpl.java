package com.example.service.impl;

import com.example.entity.Advertiser;
import com.example.persistence.IAdvertiserDAO;
import com.example.persistence.IGenericDAO;
import com.example.service.IAdvertiserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdvertiserServiceImpl extends GenericServiceImpl<Advertiser, UUID>
        implements IAdvertiserService {

    private final IAdvertiserDAO dao;

    @Override
    protected IGenericDAO<Advertiser, UUID> getDAO() {
        return dao;
    }

    @Override
    protected String getEntityName() {
        return "Advertiser";
    }
}
