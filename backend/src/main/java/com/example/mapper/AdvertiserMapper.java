package com.example.mapper;

import com.example.dto.request.AdvertiserRequestDTO;
import com.example.dto.response.AdvertiserResponseDTO;
import com.example.entity.Advertiser;
import com.example.qualifier.FindEntity;
import com.example.qualifier.MainService;
import com.example.service.Impl.UserServiceImpl;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserServiceImpl.class})
public interface AdvertiserMapper
        extends GenericMapper<Advertiser, AdvertiserResponseDTO, AdvertiserRequestDTO> {

    @Override
    @Mapping(source = "id", target = "id")
    @Mapping(source = "title", target = "title")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "bannerUrl", target = "bannerUrl")
    @Mapping(source = "avatarUrl", target = "avatarUrl")
    @Mapping(source = "publicLocation", target = "publicLocation")
    @Mapping(source = "publicUrlLocation", target = "publicUrlLocation")
    @Mapping(source = "indexed", target = "indexed")
    @Mapping(source = "paid", target = "paid")
    @Mapping(source = "verified", target = "verified")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.email", target = "username")
    AdvertiserResponseDTO toResponseDTO(Advertiser entity);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "title", target = "title")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "bannerUrl", target = "bannerUrl")
    @Mapping(source = "avatarUrl", target = "avatarUrl")
    @Mapping(source = "publicLocation", target = "publicLocation")
    @Mapping(source = "publicUrlLocation", target = "publicUrlLocation")
    @Mapping(target = "indexed", ignore = true)
    @Mapping(target = "paid", ignore = true)
    @Mapping(target = "verified", ignore = true)
    @Mapping(source = "userId", target = "user", qualifiedBy = { MainService.class, FindEntity.class })
    Advertiser toEntity(AdvertiserRequestDTO advertiserRequestDTO);

}
