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

/**
 * Implementation of {@link PermissionMapper} focused on converting between {@link Permission}
 * entities and their associated request and response DTOs.
 *
 * <p>Responsibilities include:
 *
 * <ul>
 *   <li>Mapping a single {@link Permission} entity to a {@link PermissionResponseDTO}.
 *   <li>Converting a {@link PermissionRequestDTO} into a {@link Permission} entity.
 *   <li>Handling collections of Permission entities to produce lists of PermissionResponseDTOs.
 *   <li>Gracefully handling null or empty inputs by returning null or empty collections as
 *       appropriate.
 * </ul>
 *
 * <p>This class is annotated with {@link Component} to enable Spring's dependency injection and
 * uses Lombok's {@link RequiredArgsConstructor} for constructor-based injection (if required in
 * future).
 *
 * @see Permission
 * @see PermissionRequestDTO
 * @see PermissionResponseDTO
 */
@Component
@RequiredArgsConstructor
public class PermissionMapperImpl implements PermissionMapper {

    /**
     * Maps a {@link Permission} entity to a {@link PermissionResponseDTO}.
     *
     * @param entity the Permission entity to convert; may be {@code null}.
     * @return corresponding PermissionResponseDTO, or {@code null} if the input is {@code null}.
     */
    @Override
    public PermissionResponseDTO toResponseDTO(Permission entity) {
        if (entity == null) return null;
        return new PermissionResponseDTO(entity.getId(), entity.getPermissionName());
    }

    /**
     * Converts a {@link PermissionRequestDTO} to a {@link Permission} entity.
     *
     * @param requestDTO the incoming request DTO; may be {@code null}.
     * @return a new Permission entity, or {@code null} if the input is {@code null}.
     */
    @Override
    public Permission toEntity(PermissionRequestDTO requestDTO) {
        if (requestDTO == null) return null;
        return new Permission(requestDTO.getPermissionName());
    }

    /**
     * Maps a collection of {@link Permission} entities into a list of {@link
     * PermissionResponseDTO}.
     *
     * @param entities the Permission entities; may be {@code null} or empty.
     * @return list of response DTOs, or an empty list if input is null or empty.
     */
    @Override
    public List<PermissionResponseDTO> toListResponseDTO(Collection<Permission> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        List<PermissionResponseDTO> permissionResponseDTOS = new ArrayList<>(entities.size());
        for (Permission permission : entities) {
            permissionResponseDTOS.add(toResponseDTO(permission));
        }
        return permissionResponseDTOS;
    }
}
