package com.example.mapper;

import com.example.dto.request.PermissionRequestDTO;
import com.example.dto.response.PermissionResponseDTO;
import com.example.entity.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PermissionMapper
        extends GenericMapper<Permission, PermissionResponseDTO, PermissionRequestDTO> {

    @Override
    @Mapping(source = "id", target = "id")
    @Mapping(source = "permissionName", target = "permissionName")
    PermissionResponseDTO toResponseDTO(Permission entity);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "permissionName", target = "permissionName")
    Permission toEntity(PermissionRequestDTO requestDTO);

}
