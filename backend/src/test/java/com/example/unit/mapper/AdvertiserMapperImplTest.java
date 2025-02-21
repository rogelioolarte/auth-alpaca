package com.example.unit.mapper;

import com.example.dto.response.AdvertiserResponseDTO;
import com.example.entity.Advertiser;
import com.example.mapper.impl.AdvertiserMapperImpl;
import com.example.resources.AdvertiserProvider;
import com.example.resources.UserProvider;
import com.example.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdvertiserMapperImplTest {

    @Mock
    private UserServiceImpl userService;

    @InjectMocks
    private AdvertiserMapperImpl mapper;

    @Test
    void toPageResponseDTO() {
        assertEquals(new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 0),
                mapper.toPageResponseDTO(null));

        assertEquals(new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 0),
                mapper.toPageResponseDTO(new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 0)));

        Page<AdvertiserResponseDTO> page = mapper.toPageResponseDTO(
                new PageImpl<>(AdvertiserProvider.listEntities(), Pageable.unpaged(), 2));
        assertNotNull(page);
        assertEquals(Pageable.unpaged(), page.getPageable());
        assertEquals(AdvertiserProvider.listEntities().getFirst().getId(),
                page.getContent().getFirst().id());
        assertEquals(AdvertiserProvider.listEntities().getFirst().getTitle(),
                page.getContent().getFirst().title());
        assertEquals(AdvertiserProvider.listEntities().getLast().getId(),
                page.getContent().getLast().id());
        assertEquals(AdvertiserProvider.listEntities().getLast().getTitle(),
                page.getContent().getLast().title());
    }

    @Test
    void toResponseDTO() {
        assertNull(mapper.toResponseDTO(null));

        AdvertiserResponseDTO responseDTO = mapper.toResponseDTO(AdvertiserProvider.singleEntity());
        assertNotNull(responseDTO);
        assertEquals(AdvertiserProvider.singleEntity().getId(), responseDTO.id());
        assertEquals(AdvertiserProvider.singleEntity().getTitle(), responseDTO.title());
        assertEquals(AdvertiserProvider.singleEntity().getDescription(), responseDTO.description());
        assertEquals(AdvertiserProvider.singleEntity().getUser().getId(), responseDTO.userId());
    }

    @Test
    void toEntity() {
        assertNull(mapper.toEntity(null));

        when(userService.findById(UUID.fromString(AdvertiserProvider.singleRequest().getUserId())))
                .thenReturn(UserProvider.singleEntity());
        Advertiser entity = mapper.toEntity(AdvertiserProvider.singleRequest());
        assertNotNull(entity);
        assertEquals(AdvertiserProvider.singleRequest().getTitle(), entity.getTitle());
        assertEquals(AdvertiserProvider.singleRequest().getDescription(), entity.getDescription());
        assertEquals(AdvertiserProvider.singleRequest().getUserId(), entity.getUser().getId().toString());
        verify(userService).findById(UUID.fromString(AdvertiserProvider.singleRequest().getUserId()));
    }

    @Test
    void toListResponseDTO() {
        assertEquals(Collections.emptyList(), mapper.toListResponseDTO(null));

        assertEquals(Collections.emptyList(), mapper.toListResponseDTO(Collections.emptyList()));

        List<AdvertiserResponseDTO> responseDTOS = mapper.toListResponseDTO(
                AdvertiserProvider.listEntities());
        assertNotNull(responseDTOS);
        assertEquals(AdvertiserProvider.listEntities().getFirst().getId(),
                responseDTOS.getFirst().id());
        assertEquals(AdvertiserProvider.listEntities().getFirst().getTitle(),
                responseDTOS.getFirst().title());
        assertEquals(AdvertiserProvider.listEntities().getLast().getId(),
                responseDTOS.getLast().id());
        assertEquals(AdvertiserProvider.listEntities().getLast().getTitle(),
                responseDTOS.getLast().title());
    }
}