package com.alpaca.unit.mapper;

import com.alpaca.dto.request.PermissionRequestDTO;
import com.alpaca.dto.response.PermissionResponseDTO;
import com.alpaca.entity.Permission;
import com.alpaca.mapper.impl.PermissionMapperImpl;
import com.alpaca.resources.PermissionProvider;
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

    @InjectMocks private PermissionMapperImpl mapper;

    @Test
    void toPageResponseDTO() {
        assertEquals(
                new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 0),
                mapper.toPageResponseDTO(null));

        assertEquals(
                new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 0),
                mapper.toPageResponseDTO(
                        new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 0)));

        List<Permission> entities = PermissionProvider.listEntities();
        Page<PermissionResponseDTO> permissionPage =
                mapper.toPageResponseDTO(new PageImpl<>(entities, Pageable.unpaged(), 2));
        assertNotNull(permissionPage);
        assertEquals(Pageable.unpaged(), permissionPage.getPageable());
        assertEquals(entities.getFirst().getId(), permissionPage.getContent().getFirst().id());
        assertEquals(
                entities.getFirst().getPermissionName(),
                permissionPage.getContent().getFirst().permissionName());
        assertEquals(entities.getLast().getId(), permissionPage.getContent().getLast().id());
        assertEquals(
                entities.getLast().getPermissionName(),
                permissionPage.getContent().getLast().permissionName());
    }

    @Test
    void toResponseDTO() {
        assertNull(mapper.toResponseDTO(null));

        Permission permission = PermissionProvider.singleEntity();
        PermissionResponseDTO responseDTO = mapper.toResponseDTO(permission);
        assertNotNull(responseDTO);
        assertEquals(permission.getId(), responseDTO.id());
        assertEquals(permission.getPermissionName(), responseDTO.permissionName());
    }

    @Test
    void toEntity() {
        assertNull(mapper.toEntity(null));

        Permission permission = mapper.toEntity(PermissionProvider.singleRequest());
        PermissionRequestDTO permissionRequestDTO = PermissionProvider.singleRequest();
        assertNotNull(permission);
        assertEquals(permissionRequestDTO.getPermissionName(), permission.getPermissionName());
    }

    @Test
    void toListResponseDTO() {
        assertEquals(Collections.emptyList(), mapper.toListResponseDTO(null));

        assertEquals(Collections.emptyList(), mapper.toListResponseDTO(Collections.emptyList()));

        List<Permission> entities = PermissionProvider.listEntities();
        List<PermissionResponseDTO> responseDTOS = mapper.toListResponseDTO(entities);
        assertNotNull(responseDTOS);
        assertEquals(entities.getFirst().getId(), responseDTOS.getFirst().id());
        assertEquals(
                entities.getFirst().getPermissionName(), responseDTOS.getFirst().permissionName());
        assertEquals(entities.getLast().getId(), responseDTOS.getLast().id());
        assertEquals(
                entities.getLast().getPermissionName(), responseDTOS.getLast().permissionName());
    }
}
