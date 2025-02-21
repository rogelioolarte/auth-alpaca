package com.example.unit.mapper;

import com.example.dto.response.UserResponseDTO;
import com.example.entity.User;
import com.example.mapper.impl.AdvertiserMapperImpl;
import com.example.mapper.impl.ProfileMapperImpl;
import com.example.mapper.impl.RoleMapperImpl;
import com.example.mapper.impl.UserMapperImpl;
import com.example.resources.AdvertiserProvider;
import com.example.resources.ProfileProvider;
import com.example.resources.RoleProvider;
import com.example.resources.UserProvider;
import com.example.service.impl.RoleServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserMapperImplTest {

    @Mock
    private ProfileMapperImpl profileMapper;

    @Mock
    private AdvertiserMapperImpl advertiserMapper;

    @Mock
    private RoleMapperImpl roleMapper;

    @Mock
    private RoleServiceImpl roleService;

    @InjectMocks
    private UserMapperImpl mapper;

    @Test
    void toPageResponseDTO() {
        assertEquals(new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 0),
                mapper.toPageResponseDTO(null));

        assertEquals(new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 0),
                mapper.toPageResponseDTO(new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 0)));

        Page<UserResponseDTO> page = mapper.toPageResponseDTO(
                new PageImpl<>(UserProvider.listEntities(), Pageable.unpaged(), 2));
        assertNotNull(page);
        assertEquals(Pageable.unpaged(), page.getPageable());
        assertEquals(UserProvider.listEntities().getFirst().getId(),
                page.getContent().getFirst().id());
        assertEquals(UserProvider.listEntities().getFirst().getEmail(),
                page.getContent().getFirst().email());
        assertEquals(UserProvider.listEntities().getLast().getId(),
                page.getContent().getLast().id());
        assertEquals(UserProvider.listEntities().getLast().getEmail(),
                page.getContent().getLast().email());
    }

    @Test
    void toResponseDTO() {
        assertNull(mapper.toResponseDTO(null));

        when(roleMapper.toListResponseDTO(UserProvider.singleEntity().getRoles()))
                .thenReturn(new ArrayList<>(List.of(RoleProvider.singleResponse())));
        when(profileMapper.toResponseDTO(null))
                .thenReturn(ProfileProvider.singleResponse());
        when(advertiserMapper.toResponseDTO(null))
                .thenReturn(AdvertiserProvider.singleResponse());
        UserResponseDTO responseDTO = mapper.toResponseDTO(UserProvider.singleEntity());
        assertNotNull(responseDTO);
        assertEquals(UserProvider.singleEntity().getId(), responseDTO.id());
        assertEquals(UserProvider.singleEntity().getEmail(), responseDTO.email());
        assertEquals(UserProvider.singleEntity().getUserRoles().iterator().next().getRole().getId(),
                responseDTO.roles().getFirst().id());
        assertNotNull(responseDTO.profile().id());
        assertNotNull(responseDTO.advertiser().id());
        verify(roleMapper).toListResponseDTO(UserProvider.singleEntity().getRoles());
        verify(profileMapper).toResponseDTO(null);
        verify(advertiserMapper).toResponseDTO(null);
    }

    @Test
    void toEntity() {
        assertNull(mapper.toEntity(null));

        when(roleService.findAllByIdsToSet(UserProvider.singleRequest().getRoles()))
                .thenReturn(new HashSet<>(Set.of(RoleProvider.singleEntity())));
        User entity = mapper.toEntity(UserProvider.singleRequest());
        assertNotNull(entity);
        assertEquals(UserProvider.singleRequest().getEmail(), entity.getEmail());
        assertEquals(UserProvider.singleRequest().getRoles().iterator().next(),
                entity.getRoles().getFirst().getId());
        verify(roleService).findAllByIdsToSet(UserProvider.singleRequest().getRoles());
    }

    @Test
    void toListResponseDTO() {
        assertEquals(Collections.emptyList(), mapper.toListResponseDTO(null));

        assertEquals(Collections.emptyList(), mapper.toListResponseDTO(Collections.emptyList()));

        List<UserResponseDTO> responseDTOS = mapper.toListResponseDTO(
                UserProvider.listEntities());
        assertNotNull(responseDTOS);
        assertEquals(UserProvider.listEntities().getFirst().getId(),
                responseDTOS.getFirst().id());
        assertEquals(UserProvider.listEntities().getFirst().getEmail(),
                responseDTOS.getFirst().email());
        assertEquals(UserProvider.listEntities().getLast().getId(),
                responseDTOS.getLast().id());
        assertEquals(UserProvider.listEntities().getLast().getEmail(),
                responseDTOS.getLast().email());
    }
}