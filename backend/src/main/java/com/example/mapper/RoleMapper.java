package com.example.mapper;

import com.example.dto.request.RoleRequestDTO;
import com.example.dto.response.RoleResponseDTO;
import com.example.entity.Role;

public interface RoleMapper
        extends GenericMapper<Role, RoleResponseDTO, RoleRequestDTO> {

    RoleResponseDTO toResponseDTO(Role entity);

    Role toEntity(RoleRequestDTO requestDTO);

}
