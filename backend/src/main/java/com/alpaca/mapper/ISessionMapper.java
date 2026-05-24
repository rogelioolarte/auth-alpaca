package com.alpaca.mapper;

import com.alpaca.dto.response.SessionResponseDTO;
import com.alpaca.entity.Session;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;

public interface ISessionMapper {

    SessionResponseDTO toResponseDTO(Session entity);

    List<SessionResponseDTO> toListResponseDTO(Collection<Session> entities);

    Page<SessionResponseDTO> toPageResponseDTO(Page<Session> entities);
}
