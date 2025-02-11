package com.example.mapper;

import com.example.dto.request.ProfileRequestDTO;
import com.example.dto.response.ProfileResponseDTO;
import com.example.entity.Profile;
import com.example.qualifier.FindEntity;
import com.example.qualifier.MainService;
import com.example.service.Impl.UserServiceImpl;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserServiceImpl.class})
public interface ProfileMapper
        extends GenericMapper<Profile, ProfileResponseDTO, ProfileRequestDTO> {

    @Override
    @Mapping(source = "id", target = "id")
    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "lastName", target = "lastName")
    @Mapping(source = "address", target = "address")
    @Mapping(source = "avatarUrl", target = "avatarUrl")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.email", target = "email")
    ProfileResponseDTO toResponseDTO(Profile profile);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "lastName", target = "lastName")
    @Mapping(source = "address", target = "address")
    @Mapping(source = "avatarUrl", target = "avatarUrl")
    @Mapping(source = "userId", target = "user",
            qualifiedBy = { MainService.class, FindEntity.class })
    Profile toEntity(ProfileRequestDTO dto);

}