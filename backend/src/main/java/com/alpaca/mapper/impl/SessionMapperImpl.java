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

@Component
@RequiredArgsConstructor
public class SessionMapperImpl implements ISessionMapper {

    @Override
    public SessionResponseDTO toResponseDTO(Session entity) {
        return new SessionResponseDTO(
                entity.getId(),
                entity.getLastSeenAt(),
                entity.getIpAddress(),
                entity.getUserAgent(),
                entity.getClientId());
    }

    @Override
    public List<SessionResponseDTO> toListResponseDTO(Collection<Session> entities) {
        if (entities == null || entities.isEmpty()) return Collections.emptyList();
        List<SessionResponseDTO> sessionResponseDTOS = new ArrayList<>(entities.size());
        for (Session session : entities) {
            sessionResponseDTOS.add(toResponseDTO(session));
        }
        return sessionResponseDTOS;
    }

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
