package com.alpaca.mapper;

import com.alpaca.dto.request.RoleRequestDTO;
import com.alpaca.dto.response.RoleResponseDTO;
import com.alpaca.entity.Role;

/**
 * Mapper interface for converting between {@link Role} entities and their corresponding request and
 * response DTOs.
 *
 * <p>Extends the generic {@link GenericMapper}, inheriting methods for:
 *
 * <ul>
 *   <li>Turning a {@link Role} entity into a {@link RoleResponseDTO}.
 *   <li>Creating a {@link Role} entity from a {@link RoleRequestDTO}.
 *   <li>Converting collections of entities into lists of response DTOs.
 *   <li>Handling paginated entity-to-DTO transformations while preserving pagination metadata.
 * </ul>
 *
 * Implementations of this interface should focus solely on data transformation, keeping mapping
 * logic consistent and centralized for maintainability across the application layers.
 *
 * @see GenericMapper
 * @see Role
 * @see RoleRequestDTO
 * @see RoleResponseDTO
 */
public interface IRoleMapper extends GenericMapper<Role, RoleResponseDTO, RoleRequestDTO> {}
