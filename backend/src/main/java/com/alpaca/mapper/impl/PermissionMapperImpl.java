package com.alpaca.mapper.impl;

import com.alpaca.dto.request.PermissionRequestDTO;
import com.alpaca.dto.response.PermissionResponseDTO;
import com.alpaca.entity.Permission;
import com.alpaca.mapper.PermissionMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PermissionMapperImpl implements PermissionMapper {

    @Override
    public PermissionResponseDTO toResponseDTO(Permission entity) {
        if (entity == null) return null;
        return new PermissionResponseDTO(entity.getId(), entity.getPermissionName());
    }

    @Override
    public Permission toEntity(PermissionRequestDTO requestDTO) {
        if (requestDTO == null) return null;
        return new Permission(requestDTO.getPermissionName());
    }

    @Override
    public List<PermissionResponseDTO> toListResponseDTO(Collection<Permission> entities) {
        if (entities == null || entities.isEmpty()) return Collections.emptyList();
        List<PermissionResponseDTO> permissionResponseDTOS = new ArrayList<>(entities.size());
        for (Permission permission : entities) {
            permissionResponseDTOS.add(toResponseDTO(permission));
        }
        return permissionResponseDTOS;
    }
}
