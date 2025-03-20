package com.alpaca.unit.mapper;

import com.alpaca.dto.request.UserRequestDTO;
import com.alpaca.dto.response.UserResponseDTO;
import com.alpaca.entity.Role;
import com.alpaca.entity.User;
import com.alpaca.entity.intermediate.UserRole;
import com.alpaca.mapper.impl.AdvertiserMapperImpl;
import com.alpaca.mapper.impl.ProfileMapperImpl;
import com.alpaca.mapper.impl.RoleMapperImpl;
import com.alpaca.mapper.impl.UserMapperImpl;
import com.alpaca.resources.AdvertiserProvider;
import com.alpaca.resources.ProfileProvider;
import com.alpaca.resources.RoleProvider;
import com.alpaca.resources.UserProvider;
import com.alpaca.service.impl.RoleServiceImpl;
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
                mapper.toPageResponseDTO(new PageImpl<>(Collections.emptyList(),
                        Pageable.unpaged(), 0)));

        List<User> entities = UserProvider.listEntities();
        Page<UserResponseDTO> page = mapper.toPageResponseDTO(
                new PageImpl<>(entities, Pageable.unpaged(), 2));
        assertNotNull(page);
        assertEquals(Pageable.unpaged(), page.getPageable());
        assertEquals(entities.getFirst().getId(),
                page.getContent().getFirst().id());
        assertEquals(entities.getFirst().getEmail(),
                page.getContent().getFirst().email());
        assertEquals(entities.getLast().getId(),
                page.getContent().getLast().id());
        assertEquals(entities.getLast().getEmail(),
                page.getContent().getLast().email());
    }

    @Test
    void toResponseDTO() {
        assertNull(mapper.toResponseDTO(null));

        User entity = UserProvider.singleEntity();
        Role role = RoleProvider.singleEntity();
        entity.setUserRoles(new HashSet<>(Set.of(new UserRole(entity, role))));
        when(roleMapper.toListResponseDTO(entity.getRoles()))
                .thenReturn(new ArrayList<>(List.of(RoleProvider.singleResponse())));
        when(profileMapper.toResponseDTO(null))
                .thenReturn(ProfileProvider.singleResponse());
        when(advertiserMapper.toResponseDTO(null))
                .thenReturn(AdvertiserProvider.singleResponse());
        UserResponseDTO responseDTO = mapper.toResponseDTO(entity);
        assertNotNull(responseDTO);
        assertEquals(entity.getId(), responseDTO.id());
        assertEquals(entity.getEmail(), responseDTO.email());
        assertEquals(entity.getUserRoles().iterator().next().getRole().getId(),
                responseDTO.roles().getFirst().id());
        assertNotNull(responseDTO.profile().id());
        assertNotNull(responseDTO.advertiser().id());
        verify(roleMapper).toListResponseDTO(entity.getRoles());
        verify(profileMapper).toResponseDTO(null);
        verify(advertiserMapper).toResponseDTO(null);
    }

    @Test
    void toEntity() {
        assertNull(mapper.toEntity(null));

        UserRequestDTO request = UserProvider.singleRequest();
        when(roleService.findAllByIdsToSet(request.getRoles()))
                .thenReturn(new HashSet<>(Set.of(RoleProvider.singleEntity())));
        User entity = mapper.toEntity(request);
        assertNotNull(entity);
        assertEquals(request.getEmail(), entity.getEmail());
        assertEquals(request.getRoles().iterator().next(),
                entity.getRoles().getFirst().getId());
        verify(roleService).findAllByIdsToSet(request.getRoles());
    }

    @Test
    void toListResponseDTO() {
        assertEquals(Collections.emptyList(), mapper.toListResponseDTO(null));

        assertEquals(Collections.emptyList(), mapper.toListResponseDTO(Collections.emptyList()));

        List<User> entities = UserProvider.listEntities();
        List<UserResponseDTO> responseDTOS = mapper.toListResponseDTO(entities);
        assertNotNull(responseDTOS);
        assertEquals(entities.getFirst().getId(), responseDTOS.getFirst().id());
        assertEquals(entities.getFirst().getEmail(), responseDTOS.getFirst().email());
        assertEquals(entities.getLast().getId(), responseDTOS.getLast().id());
        assertEquals(entities.getLast().getEmail(), responseDTOS.getLast().email());
    }
}