package com.alpaca.mapper;

import com.alpaca.dto.request.UserRequestDTO;
import com.alpaca.dto.response.UserResponseDTO;
import com.alpaca.entity.User;

/**
 * Mapper interface for converting between {@link User} entities and their corresponding request and
 * response DTOs.
 *
 * <p>Extends the generic {@link GenericMapper}, inheriting methods for:
 *
 * <ul>
 *   <li>Turning a {@link User} entity into a {@link UserResponseDTO}.
 *   <li>Creating a {@link User} entity from a {@link UserRequestDTO}.
 *   <li>Converting collections of entities into lists of response DTOs.
 *   <li>Handling paginated entity-to-DTO transformations while preserving pagination metadata.
 * </ul>
 *
 * Implementations of this interface should focus solely on data transformation, keeping mapping
 * logic consistent and centralized for maintainability across the application layers.
 *
 * @see GenericMapper
 * @see User
 * @see UserRequestDTO
 * @see UserResponseDTO
 */
public interface IUserMapper extends GenericMapper<User, UserResponseDTO, UserRequestDTO> {}
