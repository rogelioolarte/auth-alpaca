package com.example.mapper.Impl;

import com.example.dto.request.ProfileRequestDTO;
import com.example.dto.response.ProfileResponseDTO;
import com.example.entity.Profile;
import com.example.mapper.ProfileMapper;
import com.example.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProfileMapperImpl implements ProfileMapper {

    private final IUserService userService;

    @Override
    public ProfileResponseDTO toResponseDTO(Profile entity) {
        if(entity == null) return null;
        return new ProfileResponseDTO(entity.getId(), entity.getFirstName(),
                entity.getLastName(), entity.getAddress(), entity.getAvatarUrl(),
                entity.getUser().getId(), entity.getUser().getEmail());
    }

    @Override
    public Profile toEntity(ProfileRequestDTO requestDTO) {
        if(requestDTO == null) return null;
        return new Profile(requestDTO.getFirstName(), requestDTO.getLastName(),
                requestDTO.getAddress(), requestDTO.getAvatarUrl(),
                userService.findById(UUID.fromString(requestDTO.getUserId())));
    }

    @Override
    public List<ProfileResponseDTO> toListResponseDTO(Collection<Profile> entities) {
        List<ProfileResponseDTO> profileResponseDTOS = new ArrayList<>(entities.size());
        for(Profile profile : entities) {
            profileResponseDTOS.add(toResponseDTO(profile));
        }
        return profileResponseDTOS;
    }
}
