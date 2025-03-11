package com.alpaca.unit.mapper;

import com.alpaca.dto.request.ProfileRequestDTO;
import com.alpaca.dto.response.ProfileResponseDTO;
import com.alpaca.entity.Profile;
import com.alpaca.mapper.impl.ProfileMapperImpl;
import com.alpaca.resources.ProfileProvider;
import com.alpaca.resources.UserProvider;
import com.alpaca.service.impl.UserServiceImpl;
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

        List<Profile> profiles = ProfileProvider.listEntities();
        Page<ProfileResponseDTO> page = mapper.toPageResponseDTO(
                new PageImpl<>(profiles, Pageable.unpaged(), 2));
        assertNotNull(page);
        assertEquals(Pageable.unpaged(), page.getPageable());
        assertEquals(profiles.getFirst().getId(), page.getContent().getFirst().id());
        assertEquals(profiles.getLast().getId(), page.getContent().getLast().id());
    }

    @Test
    void toResponseDTO() {
        assertNull(mapper.toResponseDTO(null));

        Profile profile = ProfileProvider.singleEntity();
        ProfileResponseDTO responseDTO = mapper.toResponseDTO(ProfileProvider.singleEntity());
        assertNotNull(responseDTO);
        assertEquals(profile.getId(), responseDTO.id());
        assertEquals(profile.getFirstName(), responseDTO.firstName());
        assertEquals(profile.getLastName(), responseDTO.lastName());
        assertEquals(profile.getUser().getId(), responseDTO.userId());
    }

    @Test
    void toEntity() {
        assertNull(mapper.toEntity(null));

        ProfileRequestDTO profileRequestDTO = ProfileProvider.singleRequest();
        when(userService.findById(UUID.fromString(profileRequestDTO.getUserId())))
                .thenReturn(UserProvider.singleEntity());
        Profile entity = mapper.toEntity(profileRequestDTO);
        assertNotNull(entity);
        assertEquals(profileRequestDTO.getFirstName(), entity.getFirstName());
        assertEquals(profileRequestDTO.getLastName(), entity.getLastName());
        assertEquals(profileRequestDTO.getUserId(), entity.getUser().getId().toString());
        verify(userService).findById(UUID.fromString(profileRequestDTO.getUserId()));
    }

    @Test
    void toListResponseDTO() {
        assertEquals(Collections.emptyList(), mapper.toListResponseDTO(null));

        assertEquals(Collections.emptyList(), mapper.toListResponseDTO(Collections.emptyList()));

        List<Profile> profiles = ProfileProvider.listEntities();
        List<ProfileResponseDTO> responseDTOS = mapper.toListResponseDTO(profiles);
        assertNotNull(responseDTOS);
        assertEquals(profiles.getFirst().getId(), responseDTOS.getFirst().id());
        assertEquals(profiles.getLast().getId(), responseDTOS.getLast().id());
    }
}