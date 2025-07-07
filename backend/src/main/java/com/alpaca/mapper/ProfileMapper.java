package com.alpaca.mapper;

import com.alpaca.dto.request.ProfileRequestDTO;
import com.alpaca.dto.response.ProfileResponseDTO;
import com.alpaca.entity.Profile;

public interface ProfileMapper
        extends GenericMapper<Profile, ProfileResponseDTO, ProfileRequestDTO> {

    ProfileResponseDTO toResponseDTO(Profile entity);

    Profile toEntity(ProfileRequestDTO requestDTO);
}
