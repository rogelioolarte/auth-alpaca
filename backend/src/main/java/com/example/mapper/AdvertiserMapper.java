package com.example.mapper;

import com.example.dto.request.AdvertiserRequestDTO;
import com.example.dto.response.AdvertiserResponseDTO;
import com.example.entity.Advertiser;

public interface AdvertiserMapper
        extends GenericMapper<Advertiser, AdvertiserResponseDTO, AdvertiserRequestDTO> {

    AdvertiserResponseDTO toResponseDTO(Advertiser entity);

    Advertiser toEntity(AdvertiserRequestDTO requestDTO);

}
