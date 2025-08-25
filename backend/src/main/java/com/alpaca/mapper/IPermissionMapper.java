package com.alpaca.mapper;

import com.alpaca.dto.request.PermissionRequestDTO;
import com.alpaca.dto.response.PermissionResponseDTO;
import com.alpaca.entity.Permission;

/**
 * Mapper interface for converting between {@link Permission} entities and their corresponding
 * request and response DTOs.
 *
 * <p>Extends the generic {@link GenericMapper}, inheriting methods for:
 *
 * <ul>
 *   <li>Turning a {@link Permission} entity into a {@link PermissionResponseDTO}.
 *   <li>Creating a {@link Permission} entity from a {@link PermissionRequestDTO}.
 *   <li>Converting collections of entities into lists of response DTOs.
 *   <li>Handling paginated entity-to-DTO transformations while preserving pagination metadata.
 * </ul>
 *
 * Implementations of this interface should focus solely on data transformation, keeping mapping
 * logic consistent and centralized for maintainability across the application layers.
 *
 * @see GenericMapper
 * @see Permission
 * @see PermissionRequestDTO
 * @see PermissionResponseDTO
 */
public interface IPermissionMapper
        extends GenericMapper<Permission, PermissionResponseDTO, PermissionRequestDTO> {}
