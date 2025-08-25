package com.alpaca.mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * A generic mapper interface to facilitate conversion between entities, request DTOs, and response
 * DTOs.
 *
 * <p>This interface defines methods for:
 *
 * <ul>
 *   <li>Converting a single entity to a response DTO.
 *   <li>Converting a request DTO to an entity.
 *   <li>Mapping a collection of entities to a list of response DTOs.
 *   <li>Converting a Spring Data {@link Page} of entities into a {@code Page} of response DTOs,
 *       handling null or empty cases gracefully.
 * </ul>
 *
 * By standardizing these operations, implementations can reduce boilerplate code and ensure
 * consistency across data transformation layers.
 */
public interface GenericMapper<T, ResponseDTO, RequestDTO> {

    /**
     * Converts an entity instance to its corresponding response DTO.
     *
     * @param entity the domain entity
     * @return a response DTO representing the entity
     */
    ResponseDTO toResponseDTO(T entity);

    /**
     * Converts a request DTO into an entity instance.
     *
     * @param requestDTO the incoming request data
     * @return a domain entity constructed from the request DTO
     */
    T toEntity(RequestDTO requestDTO);

    /**
     * Converts a collection of entities into a list of response DTOs.
     *
     * @param entities a collection of domain entities
     * @return a list of corresponding response DTOs
     */
    List<ResponseDTO> toListResponseDTO(Collection<T> entities);

    /**
     * Converts a paginated list of entities into a paginated list of response DTOs. If the source
     * page is null or empty, returns an empty, unpaged page.
     *
     * @param entities a {@link Page} of domain entities
     * @return a {@link Page} of response DTOs preserving paging metadata
     */
    default Page<ResponseDTO> toPageResponseDTO(Page<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 0);
        }
        return new PageImpl<>(
                toListResponseDTO(entities.getContent()),
                entities.getPageable(),
                entities.getTotalElements());
    }
}
