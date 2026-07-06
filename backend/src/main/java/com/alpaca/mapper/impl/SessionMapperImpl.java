package com.alpaca.mapper.impl;

import com.alpaca.dto.response.SessionResponseDTO;
import com.alpaca.entity.Session;
import com.alpaca.mapper.ISessionMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link ISessionMapper} that converts {@link Session} entities into {@link
 * SessionResponseDTO}s.
 *
 * <p>Since sessions are read-only at the mapper layer (they are created implicitly by the
 * authentication flow), this implementation only provides response-side mappings — no
 * request-DTO-to-entity conversion.
 *
 * @see Session
 * @see SessionResponseDTO
 * @see ISessionMapper
 */
@Component
@RequiredArgsConstructor
public class SessionMapperImpl implements ISessionMapper {

    /**
     * Converts a {@link Session} entity to a {@link SessionResponseDTO}.
     *
     * @param entity the session entity; never {@code null} in expected usage
     * @return the corresponding response DTO
     */
    @Override
    public SessionResponseDTO toResponseDTO(Session entity) {
        return new SessionResponseDTO(
                entity.getId(),
                entity.getLastSeenAt(),
                entity.getIpAddress(),
                entity.getUserAgent(),
                entity.getClientId());
    }

    /**
     * Converts a collection of {@link Session} entities into a list of response DTOs.
     *
     * @param entities the session entities; may be {@code null} or empty
     * @return a list of response DTOs, or an empty list if the input is null or empty
     */
    @Override
    public List<SessionResponseDTO> toListResponseDTO(Collection<Session> entities) {
        if (entities == null || entities.isEmpty()) return Collections.emptyList();
        List<SessionResponseDTO> sessionResponseDTOS = new ArrayList<>(entities.size());
        for (Session session : entities) {
            sessionResponseDTOS.add(toResponseDTO(session));
        }
        return sessionResponseDTOS;
    }

    /**
     * Converts a paginated list of {@link Session} entities into a paginated list of response DTOs.
     *
     * @param entities a page of session entities; may be {@code null} or empty
     * @return a page of response DTOs preserving pagination metadata, or an empty unpaged page
     */
    @Override
    public Page<SessionResponseDTO> toPageResponseDTO(Page<Session> entities) {
        if (entities == null || entities.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 0);
        }
        return new PageImpl<>(
                toListResponseDTO(entities.getContent()),
                entities.getPageable(),
                entities.getTotalElements());
    }
}
