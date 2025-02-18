package com.example.unit.service;

import com.example.entity.Permission;
import com.example.exception.BadRequestException;
import com.example.exception.NotFoundException;
import com.example.persistence.impl.PermissionDAOImpl;
import com.example.resources.DataProvider;
import com.example.service.impl.PermissionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

    @Mock
    private PermissionDAOImpl dao;

    @InjectMocks
    private PermissionServiceImpl service;

    @Test
    void findById() {
        assertThrows(BadRequestException.class, () -> service.findById(null));

        UUID randomId = UUID.randomUUID();
        when(this.dao.findById(randomId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> this.service.findById(randomId));
        verify(this.dao).findById(randomId);

        UUID id = UUID.fromString("c06f3206-c469-4216-bbc7-77fed3a8a133");
        when(dao.findById(id))
                .thenReturn(Optional.ofNullable(DataProvider.permissionMock()));
        Permission permission = service.findById(id);
        assertNotNull(permission);
        assertEquals(id, permission.getId());
        assertEquals("CREATE", permission.getPermissionName());
        verify(dao).findById(id);
    }

    @Test
    void findAllByIds() {
        UUID id = UUID.fromString("c06f3206-c469-4216-bbc7-77fed3a8a133");
        assertThrows(BadRequestException.class, () -> service.findAllByIds(null));
        assertThrows(BadRequestException.class, () -> service.findAllByIds(Collections.emptyList()));
        List<UUID> uuids = new ArrayList<>();
        uuids.add(id);
        uuids.add(null);
        assertThrows(BadRequestException.class, () -> service.findAllByIds(uuids));
        UUID secondId = UUID.fromString("b1f383ce-4c1e-4d0e-bb43-a9674377c4a2");
        when(dao.existsAllByIds(List.of(secondId))).thenReturn(false);
        assertThrows(NotFoundException.class, () -> service.findAllByIds(List.of(secondId)));

        when(dao.existsAllByIds(List.of(id))).thenReturn(true);
        when(dao.findAllByIds(List.of(id))).thenReturn(List.of(DataProvider.permissionMock()));
        List<Permission> permissions = service.findAllByIds(List.of(id));
        assertEquals(permissions, List.of(DataProvider.permissionMock()));
        verify(dao).existsAllByIds(List.of(id));
        verify(dao).findAllByIds(List.of(id));
    }

    @Test
    void findAllByIdsToSet() {
        UUID id = UUID.fromString("c06f3206-c469-4216-bbc7-77fed3a8a133");
        assertThrows(BadRequestException.class, () -> service.findAllByIdsToSet(null));
        assertThrows(BadRequestException.class, () -> service.findAllByIdsToSet(Collections.emptyList()));
        List<UUID> uuids = new ArrayList<>();
        uuids.add(id);
        uuids.add(null);
        assertThrows(BadRequestException.class, () -> service.findAllByIdsToSet(uuids));
        UUID secondId = UUID.fromString("b1f383ce-4c1e-4d0e-bb43-a9674377c4a2");
        when(dao.existsAllByIds(List.of(secondId))).thenReturn(false);
        assertThrows(NotFoundException.class, () -> service.findAllByIdsToSet(List.of(secondId)));

        when(dao.existsAllByIds(List.of(id))).thenReturn(true);
        when(dao.findAllByIds(List.of(id))).thenReturn(List.of(DataProvider.permissionMock()));
        Set<Permission> permissions = service.findAllByIdsToSet(List.of(id));
        assertEquals(permissions, Set.of(DataProvider.permissionMock()));
        verify(dao).existsAllByIds(List.of(id));
        verify(dao).findAllByIds(List.of(id));
    }

    @Test
    void save() {
    }

    @Test
    void saveAll() {
    }

    @Test
    void updateById() {
    }

    @Test
    void deleteById() {
    }

    @Test
    void findAll() {
    }

    @Test
    void findAllPage() {
    }

    @Test
    void existsById() {
    }

    @Test
    void existsAllByIds() {
    }

    @Test
    void existsByUniqueProperties() {
    }

    @Test
    void getDAO() {
    }

    @Test
    void getEntityName() {
    }
}