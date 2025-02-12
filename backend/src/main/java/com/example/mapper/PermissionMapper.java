package com.example.mapper;

import com.example.dto.request.PermissionRequestDTO;
import com.example.dto.response.PermissionResponseDTO;
import com.example.entity.Permission;

public interface PermissionMapper
        extends GenericMapper<Permission, PermissionResponseDTO, PermissionRequestDTO> {

    PermissionResponseDTO toResponseDTO(Permission entity);

    Permission toEntity(PermissionRequestDTO requestDTO);

}
