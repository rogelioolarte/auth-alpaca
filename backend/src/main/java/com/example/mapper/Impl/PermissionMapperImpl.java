package com.example.mapper.Impl;

import com.example.dto.request.PermissionRequestDTO;
import com.example.dto.response.PermissionResponseDTO;
import com.example.entity.Permission;
import com.example.mapper.PermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PermissionMapperImpl implements PermissionMapper {

    @Override
    public PermissionResponseDTO toResponseDTO(Permission entity) {
        if(entity == null) return null;
        return new PermissionResponseDTO(entity.getId(), entity.getPermissionName());
    }

    @Override
    public Permission toEntity(PermissionRequestDTO requestDTO) {
        if(requestDTO == null) return null;
        return new Permission(requestDTO.getPermissionName());
    }

    @Override
    public List<PermissionResponseDTO> toListResponseDTO(Collection<Permission> entities) {
        List<PermissionResponseDTO> permissionResponseDTOS = new ArrayList<>(entities.size());
        for(Permission permission : entities) {
            permissionResponseDTOS.add(toResponseDTO(permission));
        }
        return permissionResponseDTOS;
    }
}
