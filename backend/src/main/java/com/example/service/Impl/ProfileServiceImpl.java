package com.example.service.Impl;

import com.example.entity.Profile;
import com.example.persistence.IGenericDAO;
import com.example.persistence.IProfileDAO;
import com.example.service.IProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl extends GenericServiceImpl<Profile, UUID> implements IProfileService {

    private final IProfileDAO dao;

    @Override
    protected IGenericDAO<Profile, UUID> getDAO() {
        return dao;
    }

    @Override
    protected String getEntityName() {
        return "Profile";
    }
}
