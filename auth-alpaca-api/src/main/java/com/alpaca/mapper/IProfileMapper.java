package com.alpaca.mapper;

import com.alpaca.dto.request.ProfileRequestDTO;
import com.alpaca.dto.response.ProfileResponseDTO;
import com.alpaca.entity.Profile;

/**
 * Mapper interface for converting between {@link Profile} entities and their corresponding request
 * and response DTOs.
 *
 * <p>Extends the generic {@link GenericMapper}, inheriting methods for:
 *
 * <ul>
 *   <li>Turning a {@link Profile} entity into a {@link ProfileResponseDTO}.
 *   <li>Creating a {@link Profile} entity from a {@link ProfileRequestDTO}.
 *   <li>Converting collections of entities into lists of response DTOs.
 *   <li>Handling paginated entity-to-DTO transformations while preserving pagination metadata.
 * </ul>
 *
 * Implementations of this interface should focus solely on data transformation, keeping mapping
 * logic consistent and centralized for maintainability across the application layers.
 *
 * @see GenericMapper
 * @see Profile
 * @see ProfileRequestDTO
 * @see ProfileResponseDTO
 */
public interface IProfileMapper
        extends GenericMapper<Profile, ProfileResponseDTO, ProfileRequestDTO> {}
