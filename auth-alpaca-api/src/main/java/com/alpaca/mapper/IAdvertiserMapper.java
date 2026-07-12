package com.alpaca.mapper;

import com.alpaca.dto.request.AdvertiserRequestDTO;
import com.alpaca.dto.response.AdvertiserResponseDTO;
import com.alpaca.entity.Advertiser;

/**
 * Mapper interface for converting between {@link Advertiser} entities and their corresponding
 * request and response DTOs.
 *
 * <p>Extends the generic {@link GenericMapper}, inheriting methods for:
 *
 * <ul>
 *   <li>Turning a {@link Advertiser} entity into a {@link AdvertiserResponseDTO}.
 *   <li>Creating a {@link Advertiser} entity from a {@link AdvertiserRequestDTO}.
 *   <li>Converting collections of entities into lists of response DTOs.
 *   <li>Handling paginated entity-to-DTO transformations while preserving pagination metadata.
 * </ul>
 *
 * Implementations of this interface should focus solely on data transformation, keeping mapping
 * logic consistent and centralized for maintainability across the application layers.
 *
 * @see GenericMapper
 * @see Advertiser
 * @see AdvertiserRequestDTO
 * @see AdvertiserResponseDTO
 */
public interface IAdvertiserMapper
        extends GenericMapper<Advertiser, AdvertiserResponseDTO, AdvertiserRequestDTO> {}
