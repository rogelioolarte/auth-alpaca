package com.example.mapper;

import com.example.dto.request.ProfileRequestDTO;
import com.example.dto.response.ProfileResponseDTO;
import com.example.entity.Profile;

public interface ProfileMapper
        extends GenericMapper<Profile, ProfileResponseDTO, ProfileRequestDTO> {

    ProfileResponseDTO toResponseDTO(Profile entity);

    Profile toEntity(ProfileRequestDTO requestDTO);

}