package com.example.unit.service;

import com.example.entity.Permission;
import com.example.exception.BadRequestException;
import com.example.exception.NotFoundException;
import com.example.persistence.impl.PermissionDAOImpl;
import com.example.resources.PermissionProvider;
import com.example.service.impl.PermissionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
                .thenReturn(Optional.ofNullable(PermissionProvider.alternativeEntity()));
        Permission permission = service.findById(id);
        assertNotNull(permission);
        assertEquals(id, permission.getId());
        assertEquals(PermissionProvider.alternativeEntity(), permission);
        verify(dao).findById(id);
    }

    @Test
    void findAllByIds() {
        assertThrows(BadRequestException.class, () -> service.findAllByIds(null));

        UUID id = UUID.fromString("c06f3206-c469-4216-bbc7-77fed3a8a133");
        assertThrows(BadRequestException.class, () -> service.findAllByIds(Collections.emptyList()));

        List<UUID> uuids = new ArrayList<>();
        uuids.add(id);
        uuids.add(null);
        assertThrows(BadRequestException.class, () -> service.findAllByIds(uuids));

        UUID secondId = UUID.fromString("b1f383ce-4c1e-4d0e-bb43-a9674377c4a2");
        when(dao.existsAllByIds(List.of(secondId))).thenReturn(false);
        assertThrows(NotFoundException.class, () -> service.findAllByIds(
                new ArrayList<>(List.of(secondId))));

        when(dao.existsAllByIds(List.of(id))).thenReturn(true);
        when(dao.findAllByIds(List.of(id))).thenReturn(List.of(PermissionProvider.singleEntity()));
        List<Permission> permissions = service.findAllByIds(new ArrayList<>(List.of(id)));
        assertNotNull(permissions);
        assertFalse(permissions.isEmpty());
        assertEquals(List.of(PermissionProvider.singleEntity()), permissions);
        verify(dao).existsAllByIds(List.of(id));
        verify(dao).findAllByIds(List.of(id));
    }

    @Test
    void findAllByIdsToSet() {
        assertThrows(BadRequestException.class, () -> service.findAllByIdsToSet(null));

        UUID id = UUID.fromString("c06f3206-c469-4216-bbc7-77fed3a8a133");
        assertThrows(BadRequestException.class, () -> service.findAllByIdsToSet(Collections.emptyList()));

        List<UUID> uuids = new ArrayList<>();
        uuids.add(id);
        uuids.add(null);
        assertThrows(BadRequestException.class, () -> service.findAllByIdsToSet(uuids));

        UUID secondId = UUID.fromString("b1f383ce-4c1e-4d0e-bb43-a9674377c4a2");
        when(dao.existsAllByIds(List.of(secondId))).thenReturn(false);
        assertThrows(NotFoundException.class, () -> service.findAllByIdsToSet(
                new ArrayList<>(List.of(secondId))));

        when(dao.existsAllByIds(List.of(id))).thenReturn(true);
        when(dao.findAllByIds(List.of(id))).thenReturn(List.of(PermissionProvider.singleEntity()));
        Set<Permission> permissions = service.findAllByIdsToSet(new ArrayList<>(List.of(id)));
        assertNotNull(permissions);
        assertFalse(permissions.isEmpty());
        assertEquals(Set.of(PermissionProvider.singleEntity()), permissions);
        verify(dao).existsAllByIds(List.of(id));
        verify(dao).findAllByIds(List.of(id));
    }

    @Test
    void save() {
        assertThrows(BadRequestException.class, () -> service.save(null));

        Permission permission = PermissionProvider.singleEntity();
        when(dao.existsByUniqueProperties(permission)).thenReturn(true);
        assertThrows(BadRequestException.class, () -> service.save(permission));
        verify(dao).existsByUniqueProperties(permission);

        Permission permissionSecond = PermissionProvider.alternativeEntity();
        when(dao.existsByUniqueProperties(permissionSecond)).thenReturn(false);
        when(dao.save(permissionSecond)).thenReturn(permissionSecond);
        service.save(permissionSecond);
        verify(dao).existsByUniqueProperties(permissionSecond);
        ArgumentCaptor<Permission> permissionAC = ArgumentCaptor.forClass(Permission.class);
        verify(dao).save(permissionAC.capture());
        assertEquals(permissionSecond.getId(), permissionAC.getValue().getId());
        assertEquals(permissionSecond.getPermissionName(), permissionAC.getValue().getPermissionName());
        assertEquals(permissionSecond.getRolePermissions(), permissionAC.getValue().getRolePermissions());
    }

    @Test
    void saveAll() {
        assertThrows(BadRequestException.class, () -> service.saveAll(null));

        assertThrows(BadRequestException.class, () -> service.saveAll(Collections.emptyList()));

        List<Permission> permissions = new ArrayList<>(PermissionProvider.listEntities());
        permissions.add(null);
        assertThrows(BadRequestException.class, () -> service.saveAll(permissions));

        List<Permission> listEntities = new ArrayList<>(PermissionProvider.listEntities());
        when(dao.saveAll(listEntities)).thenReturn(listEntities);
        List<Permission> savePermissions = service.saveAll(listEntities);
        assertNotNull(savePermissions);
        assertFalse(savePermissions.isEmpty());
        assertEquals(listEntities, savePermissions);
        verify(dao).saveAll(listEntities);
    }

    @Test
    void updateById() {
        assertThrows(BadRequestException.class, () -> service.updateById(null, null));

        UUID id = UUID.fromString("c06f3206-c469-4216-bbc7-77fed3a8a133");
        assertThrows(BadRequestException.class, () -> service.updateById(null, id));

        Permission permission = PermissionProvider.singleEntity();
        assertThrows(BadRequestException.class, () -> service.updateById(permission, null));

        when(dao.updateById(permission, id)).thenReturn(null);
        assertThrows(BadRequestException.class, () -> service.updateById(permission, id));
        verify(dao).updateById(permission, id);
        reset(dao);

        UUID idSecond = UUID.fromString("b1f383ce-4c1e-4d0e-bb43-a9674377c4a2");
        Permission permissionSecond = PermissionProvider.alternativeEntity();
        when(dao.updateById(permissionSecond, idSecond)).thenReturn(permissionSecond);
        service.updateById(permissionSecond, idSecond);
        ArgumentCaptor<UUID> idAC = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<Permission> permissionAC = ArgumentCaptor.forClass(Permission.class);
        verify(dao).updateById(permissionAC.capture(), idAC.capture());
        assertEquals(idSecond, idAC.getValue());
        assertEquals(permissionSecond.getId(), permissionAC.getValue().getId());
        assertEquals(permissionSecond.getPermissionName(), permissionAC.getValue().getPermissionName());
        assertEquals(permissionSecond.getRolePermissions(), permissionAC.getValue().getRolePermissions());
    }

    @Test
    void deleteById() {
        assertThrows(BadRequestException.class, () -> service.deleteById(null));

        UUID id = UUID.fromString("c06f3206-c469-4216-bbc7-77fed3a8a133");
        when(dao.existsById(id)).thenReturn(false);
        assertThrows(BadRequestException.class, () -> service.deleteById(id));

        UUID idSecond = UUID.fromString("b1f383ce-4c1e-4d0e-bb43-a9674377c4a2");
        when(dao.existsById(idSecond)).thenReturn(true);
        service.deleteById(idSecond);
        ArgumentCaptor<UUID> idAC = ArgumentCaptor.forClass(UUID.class);
        verify(dao).existsById(idSecond);
        verify(dao).deleteById(idAC.capture());
        assertEquals(idSecond, idAC.getValue());
    }

    @Test
    void findAll() {
        when(dao.findAll()).thenReturn(PermissionProvider.listEntities());
        List<Permission> permissions = service.findAll();
        assertNotNull(permissions);
        assertFalse(permissions.isEmpty());
        assertEquals(PermissionProvider.listEntities(), permissions);
        assertEquals(PermissionProvider.listEntities().getFirst(), permissions.getFirst());
        verify(dao).findAll();
    }

    @Test
    void findAllPage() {
        assertThrows(BadRequestException.class, () -> service.findAllPage(null));

        when(dao.findAllPage(Pageable.unpaged()))
                .thenReturn(new PageImpl<>(PermissionProvider.listEntities()));
        Page<Permission> permissionPage = service.findAllPage(Pageable.unpaged());
        assertNotNull(permissionPage);
        assertFalse(permissionPage.isEmpty());
        assertTrue(permissionPage.getPageable().isUnpaged());
        assertEquals(permissionPage.getContent(), PermissionProvider.listEntities());
        verify(dao).findAllPage(Pageable.unpaged());
    }

    @Test
    void existsById() {
        assertFalse(service.existsById(null));

        UUID id = UUID.fromString("c06f3206-c469-4216-bbc7-77fed3a8a133");
        when(dao.existsById(id)).thenReturn(true);
        assertTrue(service.existsById(id));
        verify(dao).existsById(id);

        UUID idSecond = UUID.fromString("b1f383ce-4c1e-4d0e-bb43-a9674377c4a2");
        when(dao.existsById(idSecond)).thenReturn(false);
        assertFalse(service.existsById(idSecond));
        verify(dao).existsById(idSecond);
    }

    @Test
    void existsAllByIds() {
        assertFalse(service.existsAllByIds(null));

        assertFalse(service.existsAllByIds(Collections.emptyList()));

        UUID id = UUID.fromString("c06f3206-c469-4216-bbc7-77fed3a8a133");
        List<UUID> uuids = new ArrayList<>();
        uuids.add(id);
        uuids.add(null);
        assertFalse(service.existsAllByIds(uuids));

        UUID secondId = UUID.fromString("b1f383ce-4c1e-4d0e-bb43-a9674377c4a2");
        when(dao.existsAllByIds(List.of(secondId))).thenReturn(false);
        assertFalse(service.existsAllByIds(new ArrayList<>(List.of(secondId))));
        verify(dao).existsAllByIds(List.of(secondId));

        UUID thirdId = UUID.fromString("c06f3206-c469-4216-bbc7-77fed3a8a133");
        when(dao.existsAllByIds(List.of(thirdId))).thenReturn(true);
        assertTrue(service.existsAllByIds(new ArrayList<>(List.of(thirdId))));
        verify(dao).existsAllByIds(List.of(thirdId));
    }

    @Test
    void existsByUniqueProperties() {
        assertFalse(service.existsByUniqueProperties(null));

        Permission permission = PermissionProvider.singleEntity();
        when(dao.existsByUniqueProperties(permission)).thenReturn(true);
        assertTrue(service.existsByUniqueProperties(permission));
        verify(dao).existsByUniqueProperties(permission);

        Permission permissionSecond = PermissionProvider.alternativeEntity();
        when(dao.existsByUniqueProperties(permissionSecond)).thenReturn(false);
        assertFalse(service.existsByUniqueProperties(permissionSecond));
        verify(dao).existsByUniqueProperties(permissionSecond);
    }
}