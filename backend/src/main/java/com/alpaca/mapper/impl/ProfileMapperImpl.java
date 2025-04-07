package com.alpaca.mapper.impl;

import com.alpaca.dto.request.ProfileRequestDTO;
import com.alpaca.dto.response.ProfileResponseDTO;
import com.alpaca.entity.Profile;
import com.alpaca.mapper.ProfileMapper;
import com.alpaca.service.IUserService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfileMapperImpl implements ProfileMapper {

  private final IUserService userService;

  @Override
  public ProfileResponseDTO toResponseDTO(Profile entity) {
    if (entity == null) return null;
    return new ProfileResponseDTO(
        entity.getId(),
        entity.getFirstName(),
        entity.getLastName(),
        entity.getAddress(),
        entity.getAvatarUrl(),
        entity.getUser().getId(),
        entity.getUser().getEmail());
  }

  @Override
  public Profile toEntity(ProfileRequestDTO requestDTO) {
    if (requestDTO == null) return null;
    return new Profile(
        requestDTO.getFirstName(),
        requestDTO.getLastName(),
        requestDTO.getAddress(),
        requestDTO.getAvatarUrl(),
        userService.findById(UUID.fromString(requestDTO.getUserId())));
  }

  @Override
  public List<ProfileResponseDTO> toListResponseDTO(Collection<Profile> entities) {
    if (entities == null || entities.isEmpty()) return Collections.emptyList();
    List<ProfileResponseDTO> profileResponseDTOS = new ArrayList<>(entities.size());
    for (Profile profile : entities) {
      profileResponseDTOS.add(toResponseDTO(profile));
    }
    return profileResponseDTOS;
  }
}
