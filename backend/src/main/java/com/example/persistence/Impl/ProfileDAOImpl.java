package com.example.persistence.Impl;

import com.example.entity.Profile;
import com.example.exception.NotFoundException;
import com.example.persistence.IProfileDAO;
import com.example.repository.GenericRepo;
import com.example.repository.ProfileRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProfileDAOImpl extends GenericDAOImpl<Profile, UUID> implements IProfileDAO {

    private final ProfileRepo repo;

    @Override
    protected GenericRepo<Profile, UUID> getRepo() {
        return repo;
    }

    @Override
    protected Class<Profile> getEntity() {
        return Profile.class;
    }

    @Override
    public Profile updateById(Profile profile, UUID id) {
        Profile existingProfile = findById(id).orElseThrow(() ->
                new NotFoundException(String.format("%s with ID %s not found",
                        getEntity().getName(), id.toString())));
        if (profile.getFirstName() != null && !profile.getFirstName().isBlank()) {
            existingProfile.setFirstName(profile.getFirstName());
        }
        if (profile.getLastName() != null && !profile.getLastName().isBlank()) {
            existingProfile.setLastName(profile.getLastName());
        }
        if (profile.getAddress() != null && !profile.getAddress().isBlank()) {
            existingProfile.setAddress(profile.getAddress());
        }
        if (profile.getAvatarUrl() != null && !profile.getAvatarUrl().isBlank()) {
            existingProfile.setAvatarUrl(profile.getAvatarUrl());
        }
        if (profile.getUser() != null && !profile.getUser().getId().toString().isBlank()) {
            existingProfile.setUser(profile.getUser());
        }
        return save(existingProfile);
    }

    @Override
    public boolean existsByUniqueProperties(Profile profile) {
        if (profile.getUser() == null || profile.getUser().getId() == null) return false;
        return repo.countByUserId(profile.getUser().getId()) > 0L;
    }
}
