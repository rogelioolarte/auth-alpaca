package com.example.service.impl;

import com.example.entity.Profile;
import com.example.persistence.IGenericDAO;
import com.example.persistence.IProfileDAO;
import com.example.service.IProfileService;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl extends GenericServiceImpl<Profile, UUID> implements IProfileService {

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
