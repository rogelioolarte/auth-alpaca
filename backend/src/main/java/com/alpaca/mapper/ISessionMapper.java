package com.alpaca.mapper;

import com.alpaca.dto.response.SessionResponseDTO;
import com.alpaca.entity.Session;
import org.springframework.data.domain.Page;

import java.util.Collection;
import java.util.List;

/**
 * Mapper interface for converting between {@link Session} entities and their response DTOs.
 *
 * <p>Unlike other domain mappers, this interface does not extend {@link GenericMapper} because
 * {@link Session} entities are never created or modified through a request DTO at this layer —
 * sessions are managed implicitly by the authentication flow. Only read-side transformations are
 * needed.
 *
 * @see Session
 * @see SessionResponseDTO
 */
public interface ISessionMapper {

    /**
     * Converts a {@link Session} entity into a {@link SessionResponseDTO}.
     *
     * @param entity the session entity, may be {@code null}
     * @return the corresponding response DTO
     */
    SessionResponseDTO toResponseDTO(Session entity);

    /**
     * Converts a collection of {@link Session} entities into a list of response DTOs.
     *
     * @param entities the session entities
     * @return list of response DTOs
     */
    List<SessionResponseDTO> toListResponseDTO(Collection<Session> entities);

    /**
     * Converts a paginated list of {@link Session} entities into a paginated list of response DTOs.
     *
     * @param entities a page of session entities
     * @return a page of response DTOs preserving pagination metadata
     */
    Page<SessionResponseDTO> toPageResponseDTO(Page<Session> entities);
}
