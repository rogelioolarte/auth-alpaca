package com.alpaca.mapper;

import com.alpaca.dto.request.AdvertiserRequestDTO;
import com.alpaca.dto.response.AdvertiserResponseDTO;
import com.alpaca.entity.Advertiser;

public interface AdvertiserMapper
    extends GenericMapper<Advertiser, AdvertiserResponseDTO, AdvertiserRequestDTO> {

  AdvertiserResponseDTO toResponseDTO(Advertiser entity);

  Advertiser toEntity(AdvertiserRequestDTO requestDTO);
}
