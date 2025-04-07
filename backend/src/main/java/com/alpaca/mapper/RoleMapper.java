package com.alpaca.mapper;

import com.alpaca.dto.request.RoleRequestDTO;
import com.alpaca.dto.response.RoleResponseDTO;
import com.alpaca.entity.Role;

public interface RoleMapper extends GenericMapper<Role, RoleResponseDTO, RoleRequestDTO> {

  RoleResponseDTO toResponseDTO(Role entity);

  Role toEntity(RoleRequestDTO requestDTO);
}
