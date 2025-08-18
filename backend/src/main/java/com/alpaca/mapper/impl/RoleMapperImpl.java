package com.alpaca.mapper.impl;

import com.alpaca.dto.request.RoleRequestDTO;
import com.alpaca.dto.response.RoleResponseDTO;
import com.alpaca.entity.Role;
import com.alpaca.mapper.PermissionMapper;
import com.alpaca.mapper.RoleMapper;
import com.alpaca.service.IPermissionService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementation of the {@link RoleMapper} interface responsible for mapping between {@link Role}
 * entities and their corresponding request and response DTOs.
 *
 * <p>This mapper uses:
 *
 * <ul>
 *   <li>{@link PermissionMapper} to handle nested permission mappings.
 *   <li>{@link IPermissionService} to resolve permission entities from IDs provided in request
 *       DTOs.
 * </ul>
 *
 * <p>Key responsibilities:
 *
 * <ul>
 *   <li>Convert {@link Role} entities into {@link RoleResponseDTO} objects.
 *   <li>Convert {@link RoleRequestDTO} objects into {@link Role} entities.
 *   <li>Transform collections of roles into lists of response DTOs.
 *   <li>Handle null and empty inputs gracefully by returning safe defaults.
 * </ul>
 *
 * <p>This class is annotated with {@link Component} to enable Spring's dependency injection and
 * uses Lombok's {@link RequiredArgsConstructor} for constructor-based injection (if required in
 * future).
 *
 * @see Role
 * @see RoleRequestDTO
 * @see RoleResponseDTO
 * @see PermissionMapper
 * @see IPermissionService
 */
@Component
@RequiredArgsConstructor
public class RoleMapperImpl implements RoleMapper {

    private final PermissionMapper permissionMapper;
    private final IPermissionService permissionService;

    /**
     * Converts a {@link Role} entity into a {@link RoleResponseDTO}.
     *
     * @param entity the role entity to convert, may be {@code null}.
     * @return the corresponding response DTO, or {@code null} if the input is {@code null}.
     */
    @Override
    public RoleResponseDTO toResponseDTO(Role entity) {
        if (entity == null) return null;
        return new RoleResponseDTO(
                entity.getId(),
                entity.getRoleName(),
                entity.getRoleDescription(),
                permissionMapper.toListResponseDTO(entity.getPermissions()));
    }

    /**
     * Converts a {@link RoleRequestDTO} into a {@link Role} entity.
     *
     * <p>Permissions are resolved using the {@link IPermissionService} to fetch the corresponding
     * entities.
     *
     * @param requestDTO the request DTO containing role data, may be {@code null}.
     * @return the corresponding {@link Role} entity, or {@code null} if the input is {@code null}.
     */
    @Override
    public Role toEntity(RoleRequestDTO requestDTO) {
        if (requestDTO == null) return null;
        return new Role(
                requestDTO.getRoleName(),
                requestDTO.getRoleDescription(),
                permissionService.findAllByIdsToSet(requestDTO.getPermissions()));
    }

    /**
     * Converts a collection of {@link Role} entities into a list of {@link RoleResponseDTO}.
     *
     * <p>If the collection contains only one element, it is processed without iteration for
     * optimization purposes.
     *
     * @param entities the collection of role entities, may be {@code null} or empty.
     * @return a list of response DTOs, or an empty list if the input is {@code null} or empty.
     */
    @Override
    public List<RoleResponseDTO> toListResponseDTO(Collection<Role> entities) {
        if (entities == null || entities.isEmpty()) return Collections.emptyList();
        List<RoleResponseDTO> roleResponseDTOS = new ArrayList<>(entities.size());
        if (entities.size() == 1) {
            roleResponseDTOS.add(toResponseDTO(entities.iterator().next()));
        } else {
            for (Role role : entities) {
                roleResponseDTOS.add(toResponseDTO(role));
            }
        }
        return roleResponseDTOS;
    }
}
