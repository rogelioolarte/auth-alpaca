package com.alpaca.unit.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.dto.response.SessionResponseDTO;
import com.alpaca.entity.Session;
import com.alpaca.mapper.impl.SessionMapperImpl;
import com.alpaca.resources.SessionProvider;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class SessionMapperImplTest {

    @InjectMocks private SessionMapperImpl mapper;

    @Test
    void toResponseDTO() {
        Session entity = SessionProvider.singleEntity();

        SessionResponseDTO responseDTO = mapper.toResponseDTO(entity);

        assertNotNull(responseDTO);
        assertEquals(entity.getId(), responseDTO.id());
        assertEquals(entity.getLastSeenAt(), responseDTO.lastSeenAt());
        assertEquals(entity.getIpAddress(), responseDTO.ipAddress());
        assertEquals(entity.getUserAgent(), responseDTO.userAgent());
        assertEquals(entity.getClientId(), responseDTO.clientId());
    }

    @Test
    void toListResponseDTOShouldReturnEmptyListWhenEntitiesIsNull() {
        List<SessionResponseDTO> responseDTOS = mapper.toListResponseDTO(null);

        assertNotNull(responseDTOS);
        assertEquals(Collections.emptyList(), responseDTOS);
        assertTrue(responseDTOS.isEmpty());
    }

    @Test
    void toListResponseDTOShouldReturnEmptyListWhenEntitiesIsEmpty() {
        List<SessionResponseDTO> responseDTOS = mapper.toListResponseDTO(Collections.emptyList());

        assertNotNull(responseDTOS);
        assertEquals(Collections.emptyList(), responseDTOS);
        assertTrue(responseDTOS.isEmpty());
    }

    @Test
    void toListResponseDTOShouldMapAllEntitiesSuccessfully() {
        List<Session> entities = SessionProvider.listEntities();

        List<SessionResponseDTO> responseDTOS = mapper.toListResponseDTO(entities);

        assertNotNull(responseDTOS);
        assertEquals(entities.size(), responseDTOS.size());

        Session firstEntity = entities.getFirst();
        SessionResponseDTO firstResponse = responseDTOS.getFirst();

        assertEquals(firstEntity.getId(), firstResponse.id());
        assertEquals(firstEntity.getLastSeenAt(), firstResponse.lastSeenAt());
        assertEquals(firstEntity.getIpAddress(), firstResponse.ipAddress());
        assertEquals(firstEntity.getUserAgent(), firstResponse.userAgent());
        assertEquals(firstEntity.getClientId(), firstResponse.clientId());

        Session lastEntity = entities.getLast();
        SessionResponseDTO lastResponse = responseDTOS.getLast();

        assertEquals(lastEntity.getId(), lastResponse.id());
        assertEquals(lastEntity.getLastSeenAt(), lastResponse.lastSeenAt());
        assertEquals(lastEntity.getIpAddress(), lastResponse.ipAddress());
        assertEquals(lastEntity.getUserAgent(), lastResponse.userAgent());
        assertEquals(lastEntity.getClientId(), lastResponse.clientId());
    }

    @Test
    void toPageResponseDTOShouldReturnEmptyPageWhenEntitiesIsNull() {
        Page<SessionResponseDTO> responsePage = mapper.toPageResponseDTO(null);

        assertNotNull(responsePage);
        assertTrue(responsePage.isEmpty());
        assertEquals(0, responsePage.getTotalElements());
        assertEquals(Collections.emptyList(), responsePage.getContent());
        assertEquals(Pageable.unpaged(), responsePage.getPageable());
    }

    @Test
    void toPageResponseDTOShouldReturnEmptyPageWhenEntitiesIsEmpty() {
        Page<Session> entities = new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 0);

        Page<SessionResponseDTO> responsePage = mapper.toPageResponseDTO(entities);

        assertNotNull(responsePage);
        assertTrue(responsePage.isEmpty());
        assertEquals(0, responsePage.getTotalElements());
        assertEquals(Collections.emptyList(), responsePage.getContent());
        assertEquals(Pageable.unpaged(), responsePage.getPageable());
    }

    @Test
    void toPageResponseDTOShouldMapPageSuccessfully() {
        List<Session> entities = SessionProvider.listEntities();

        Pageable pageable = PageRequest.of(0, entities.size());

        Page<Session> entityPage = new PageImpl<>(entities, pageable, entities.size());

        Page<SessionResponseDTO> responsePage = mapper.toPageResponseDTO(entityPage);

        assertNotNull(responsePage);
        assertFalse(responsePage.isEmpty());
        assertEquals(entities.size(), responsePage.getTotalElements());
        assertEquals(pageable, responsePage.getPageable());
        assertEquals(entities.size(), responsePage.getContent().size());

        Session firstEntity = entities.getFirst();
        SessionResponseDTO firstResponse = responsePage.getContent().getFirst();

        assertEquals(firstEntity.getId(), firstResponse.id());
        assertEquals(firstEntity.getLastSeenAt(), firstResponse.lastSeenAt());
        assertEquals(firstEntity.getIpAddress(), firstResponse.ipAddress());
        assertEquals(firstEntity.getUserAgent(), firstResponse.userAgent());
        assertEquals(firstEntity.getClientId(), firstResponse.clientId());

        Session lastEntity = entities.getLast();
        SessionResponseDTO lastResponse = responsePage.getContent().getLast();

        assertEquals(lastEntity.getId(), lastResponse.id());
        assertEquals(lastEntity.getLastSeenAt(), lastResponse.lastSeenAt());
        assertEquals(lastEntity.getIpAddress(), lastResponse.ipAddress());
        assertEquals(lastEntity.getUserAgent(), lastResponse.userAgent());
        assertEquals(lastEntity.getClientId(), lastResponse.clientId());
    }
}
