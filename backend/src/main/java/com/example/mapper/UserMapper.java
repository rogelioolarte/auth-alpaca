package com.example.mapper;

import com.example.dto.request.UserRequestDTO;
import com.example.dto.response.UserResponseDTO;
import com.example.entity.User;
import com.example.qualifier.FindEntitiesSet;
import com.example.qualifier.MainService;
import com.example.service.Impl.RoleServiceImpl;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {RoleServiceImpl.class})
public interface UserMapper extends GenericMapper<User, UserResponseDTO, UserRequestDTO> {

    @Override
    @Mapping(source = "id", target = "id")
    @Mapping(source = "username", target = "username")
    @Mapping(source = "profile", target = "profile")
    @Mapping(source = "advertiser", target = "advertiser")
    @Mapping(source = "roles", target = "roles")
    UserResponseDTO toResponseDTO(User user);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", source = "username")
    @Mapping(target = "password", source = "password")
    @Mapping(target = "roles", source = "roles",
            qualifiedBy = {MainService.class, FindEntitiesSet.class})
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "credentialNoExpired", ignore = true)
    @Mapping(target = "accountNoLocked", ignore = true)
    @Mapping(target = "accountNoExpired", ignore = true)
    @Mapping(target = "profile", ignore = true)
    @Mapping(target = "advertiser", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    User toEntity(UserRequestDTO dto);

}
