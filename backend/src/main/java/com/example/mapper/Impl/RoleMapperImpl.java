package com.example.mapper.impl;

import com.example.dto.request.RoleRequestDTO;
import com.example.dto.response.RoleResponseDTO;
import com.example.entity.Role;
import com.example.mapper.PermissionMapper;
import com.example.mapper.RoleMapper;
import com.example.service.IPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RoleMapperImpl implements RoleMapper {

    private final PermissionMapper permissionMapper;
    private final IPermissionService permissionService;

    @Override
    public RoleResponseDTO toResponseDTO(Role entity) {
        if(entity == null) return null;
        return new RoleResponseDTO(entity.getId(), entity.getRoleName(), entity.getRoleDescription(),
                permissionMapper.toListResponseDTO(entity.getPermissions()));
    }

    @Override
    public Role toEntity(RoleRequestDTO requestDTO) {
        if(requestDTO == null) return null;
        return new Role(requestDTO.getRoleName(), requestDTO.getRoleDescription(),
                permissionService.findAllByIdsToSet(requestDTO.getPermissions()));
    }

    @Override
    public List<RoleResponseDTO> toListResponseDTO(Collection<Role> entities) {
        if(entities == null || entities.isEmpty()) return Collections.emptyList();
        List<RoleResponseDTO> roleResponseDTOS = new ArrayList<>(entities.size());
        if(entities.size() == 1){
            roleResponseDTOS.add(toResponseDTO(entities.iterator().next()));
        } else {
            for(Role role : entities) {
                roleResponseDTOS.add(toResponseDTO(role));
            }
        }
        return roleResponseDTOS;
    }
}
