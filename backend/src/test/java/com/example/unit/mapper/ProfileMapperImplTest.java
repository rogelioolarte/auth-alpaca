package com.example.unit.mapper;

import com.example.dto.response.ProfileResponseDTO;
import com.example.entity.Profile;
import com.example.mapper.impl.ProfileMapperImpl;
import com.example.resources.ProfileProvider;
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
class ProfileMapperImplTest {

    @Mock
    private UserServiceImpl userService;
    
    @InjectMocks
    private ProfileMapperImpl mapper;

    @Test
    void toPageResponseDTO() {
        assertEquals(new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 0),
                mapper.toPageResponseDTO(null));

        assertEquals(new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 0),
                mapper.toPageResponseDTO(new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 0)));

        Page<ProfileResponseDTO> page = mapper.toPageResponseDTO(
                new PageImpl<>(ProfileProvider.listEntities(), Pageable.unpaged(), 2));
        assertNotNull(page);
        assertEquals(Pageable.unpaged(), page.getPageable());
        assertEquals(ProfileProvider.listEntities().getFirst().getId(),
                page.getContent().getFirst().id());
        assertEquals(ProfileProvider.listEntities().getLast().getId(),
                page.getContent().getLast().id());
    }

    @Test
    void toResponseDTO() {
        assertNull(mapper.toResponseDTO(null));

        ProfileResponseDTO responseDTO = mapper.toResponseDTO(ProfileProvider.singleEntity());
        assertNotNull(responseDTO);
        assertEquals(ProfileProvider.singleEntity().getId(), responseDTO.id());
        assertEquals(ProfileProvider.singleEntity().getFirstName(), responseDTO.firstName());
        assertEquals(ProfileProvider.singleEntity().getLastName(), responseDTO.lastName());
        assertEquals(ProfileProvider.singleEntity().getUser().getId(), responseDTO.userId());
    }

    @Test
    void toEntity() {
        assertNull(mapper.toEntity(null));

        when(userService.findById(UUID.fromString(ProfileProvider.singleRequest().getUserId())))
                .thenReturn(UserProvider.singleEntity());
        Profile entity = mapper.toEntity(ProfileProvider.singleRequest());
        assertNotNull(entity);
        assertEquals(ProfileProvider.singleRequest().getFirstName(), entity.getFirstName());
        assertEquals(ProfileProvider.singleRequest().getLastName(), entity.getLastName());
        assertEquals(ProfileProvider.singleRequest().getUserId(), entity.getUser().getId().toString());
        verify(userService).findById(UUID.fromString(ProfileProvider.singleRequest().getUserId()));
    }

    @Test
    void toListResponseDTO() {
        assertEquals(Collections.emptyList(), mapper.toListResponseDTO(null));

        assertEquals(Collections.emptyList(), mapper.toListResponseDTO(Collections.emptyList()));

        List<ProfileResponseDTO> responseDTOS = mapper.toListResponseDTO(
                ProfileProvider.listEntities());
        assertNotNull(responseDTOS);
        assertEquals(ProfileProvider.listEntities().getFirst().getId(),
                responseDTOS.getFirst().id());
        assertEquals(ProfileProvider.listEntities().getLast().getId(),
                responseDTOS.getLast().id());
    }
}