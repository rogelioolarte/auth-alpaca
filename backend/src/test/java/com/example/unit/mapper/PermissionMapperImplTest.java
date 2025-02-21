package com.example.unit.mapper;

import com.example.dto.response.PermissionResponseDTO;
import com.example.entity.Permission;
import com.example.mapper.impl.PermissionMapperImpl;
import com.example.resources.PermissionProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PermissionMapperImplTest {

    @InjectMocks
    private PermissionMapperImpl mapper;

    @Test
    void toPageResponseDTO() {
        assertEquals(new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 0),
                mapper.toPageResponseDTO(null));

        assertEquals(new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 0),
                mapper.toPageResponseDTO(new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 0)));

        Page<PermissionResponseDTO> permissionPage = mapper.toPageResponseDTO(
                new PageImpl<>(PermissionProvider.listEntities(), Pageable.unpaged(), 0));
        assertNotNull(permissionPage);
        assertEquals(Pageable.unpaged(), permissionPage.getPageable());
        assertEquals(PermissionProvider.listEntities().getFirst().getId(),
                permissionPage.getContent().getFirst().id());
        assertEquals(PermissionProvider.listEntities().getFirst().getPermissionName(),
                permissionPage.getContent().getFirst().permissionName());
        assertEquals(PermissionProvider.listEntities().getLast().getId(),
                permissionPage.getContent().getLast().id());
        assertEquals(PermissionProvider.listEntities().getLast().getPermissionName(),
                permissionPage.getContent().getLast().permissionName());
    }

    @Test
    void toResponseDTO() {
        assertNull(mapper.toResponseDTO(null));

        PermissionResponseDTO responseDTO = mapper.toResponseDTO(PermissionProvider.singleEntity());
        assertNotNull(responseDTO);
        assertEquals(PermissionProvider.singleEntity().getId(), responseDTO.id());
        assertEquals(PermissionProvider.singleEntity().getPermissionName(), responseDTO.permissionName());
    }

    @Test
    void toEntity() {
        assertNull(mapper.toEntity(null));

        Permission permission = mapper.toEntity(PermissionProvider.singleRequest());
        assertNotNull(permission);
        assertEquals(PermissionProvider.singleRequest().getPermissionName(), permission.getPermissionName());
    }

    @Test
    void toListResponseDTO() {
        assertEquals(Collections.emptyList(), mapper.toListResponseDTO(null));

        assertEquals(Collections.emptyList(), mapper.toListResponseDTO(Collections.emptyList()));

        List<PermissionResponseDTO> responseDTOS = mapper.toListResponseDTO(
                PermissionProvider.listEntities());
        assertNotNull(responseDTOS);
        assertEquals(PermissionProvider.listEntities().getFirst().getId(),
                responseDTOS.getFirst().id());
        assertEquals(PermissionProvider.listEntities().getFirst().getPermissionName(),
                responseDTOS.getFirst().permissionName());
        assertEquals(PermissionProvider.listEntities().getLast().getId(),
                responseDTOS.getLast().id());
        assertEquals(PermissionProvider.listEntities().getLast().getPermissionName(),
                responseDTOS.getLast().permissionName());
    }
}