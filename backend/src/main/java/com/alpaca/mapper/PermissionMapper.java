package com.alpaca.mapper;

import com.alpaca.dto.request.PermissionRequestDTO;
import com.alpaca.dto.response.PermissionResponseDTO;
import com.alpaca.entity.Permission;

public interface PermissionMapper
    extends GenericMapper<Permission, PermissionResponseDTO, PermissionRequestDTO> {

  PermissionResponseDTO toResponseDTO(Permission entity);

  Permission toEntity(PermissionRequestDTO requestDTO);
}
