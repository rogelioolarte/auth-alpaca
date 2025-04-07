package com.alpaca.mapper.impl;

import com.alpaca.dto.request.AdvertiserRequestDTO;
import com.alpaca.dto.response.AdvertiserResponseDTO;
import com.alpaca.entity.Advertiser;
import com.alpaca.mapper.AdvertiserMapper;
import com.alpaca.service.IUserService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdvertiserMapperImpl implements AdvertiserMapper {

  private final IUserService userService;

  @Override
  public AdvertiserResponseDTO toResponseDTO(Advertiser entity) {
    if (entity == null) return null;
    return new AdvertiserResponseDTO(
        entity.getId(),
        entity.getTitle(),
        entity.getDescription(),
        entity.getBannerUrl(),
        entity.getAvatarUrl(),
        entity.getPublicLocation(),
        entity.getPublicUrlLocation(),
        entity.isIndexed(),
        entity.isPaid(),
        entity.isVerified(),
        entity.getUser().getId(),
        entity.getUser().getEmail());
  }

  @Override
  public Advertiser toEntity(AdvertiserRequestDTO requestDTO) {
    if (requestDTO == null) return null;
    return new Advertiser(
        requestDTO.getTitle(),
        requestDTO.getDescription(),
        requestDTO.getBannerUrl(),
        requestDTO.getAvatarUrl(),
        requestDTO.getPublicLocation(),
        requestDTO.getPublicUrlLocation(),
        true,
        false,
        false,
        userService.findById(UUID.fromString(requestDTO.getUserId())));
  }

  @Override
  public List<AdvertiserResponseDTO> toListResponseDTO(Collection<Advertiser> entities) {
    if (entities == null || entities.isEmpty()) return Collections.emptyList();
    List<AdvertiserResponseDTO> responseDTOS = new ArrayList<>(entities.size());
    for (Advertiser advertiser : entities) {
      responseDTOS.add(toResponseDTO(advertiser));
    }
    return responseDTOS;
  }
}
