package com.example.unit.persistence;

import com.example.entity.Permission;
import com.example.exception.NotFoundException;
import com.example.persistence.impl.PermissionDAOImpl;
import com.example.repository.PermissionRepo;
import com.example.resources.PermissionProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionDAOImplTest {

    @Mock
    private PermissionRepo repo;

    @InjectMocks
    private PermissionDAOImpl dao;

    @Test
    void findById() {
        Permission permissionInitial = PermissionProvider.singleEntity();
        when(repo.findById(permissionInitial.getId()))
                .thenReturn(Optional.of(PermissionProvider.singleEntity()));
        Permission permission = dao
                .findById(permissionInitial.getId()).orElse(null);
        assertNotNull(permission);
        assertEquals(permissionInitial.getId(), permission.getId());
        assertEquals(permissionInitial.getPermissionName(), permission.getPermissionName());
        verify(repo).findById(permissionInitial.getId());
    }

    @Test
    void findAllByIds() {
        List<Permission> permissions = PermissionProvider.listEntities();
        when(repo.findAllById(permissions
                .stream().map(Permission::getId).collect(Collectors.toSet())))
                .thenReturn(PermissionProvider.listEntities());
        List<Permission> permissionsFound = dao.findAllByIds(permissions
                        .stream().map(Permission::getId).collect(Collectors.toSet()));
        assertNotNull(permissionsFound);
        assertEquals(permissions.getFirst().getId(),
                permissionsFound.getFirst().getId());
        assertEquals(permissions.getFirst().getPermissionName(),
                permissionsFound.getFirst().getPermissionName());
        assertEquals(permissions.getLast().getId(),
                permissionsFound.getLast().getId());
        assertEquals(permissions.getLast().getPermissionName(),
                permissionsFound.getLast().getPermissionName());
        verify(repo).findAllById(permissions
                .stream().map(Permission::getId).collect(Collectors.toSet()));
    }

    @Test
    void deleteById() {
        UUID id = PermissionProvider.singleEntity().getId();
        dao.deleteById(id);
        ArgumentCaptor<UUID> idAC = ArgumentCaptor.forClass(UUID.class);
        verify(repo).deleteById(id);
        verify(repo).deleteById(idAC.capture());
        assertEquals(id, idAC.getValue());
    }

    @Test
    void save() {
        Permission permission = PermissionProvider.singleEntity();
        when(repo.save(permission)).thenReturn(permission);
        dao.save(permission);
        ArgumentCaptor<Permission> pAC = ArgumentCaptor.forClass(Permission.class);
        verify(repo).save(permission);
        verify(repo).save(pAC.capture());
        assertEquals(permission.getId(), pAC.getValue().getId());
        assertEquals(permission.getPermissionName(), pAC.getValue().getPermissionName());
    }

    @Test
    void saveAll() {
        List<Permission> permissions = PermissionProvider.listEntities();
        when(repo.saveAll(permissions)).thenReturn(permissions);
        List<Permission> savePermissions = dao.saveAll(permissions);
        assertNotNull(savePermissions);
        assertFalse(savePermissions.isEmpty());
        assertEquals(permissions, savePermissions);
        verify(repo).saveAll(permissions);
    }

    @Test
    void findAll() {
        List<Permission> permissions = PermissionProvider.listEntities();
        when(repo.findAll()).thenReturn(permissions);
        List<Permission> permissionsFound = dao.findAll();
        assertNotNull(permissionsFound);
        assertFalse(permissionsFound.isEmpty());
        assertEquals(permissions, permissionsFound);
        verify(repo).findAll();
    }

    @Test
    void findAllPage() {
        List<Permission> permissions = PermissionProvider.listEntities();
        when(repo.findAll(Pageable.unpaged()))
                .thenReturn(new PageImpl<>(permissions));
        Page<Permission> permissionPage = dao.findAllPage(Pageable.unpaged());
        assertNotNull(permissionPage);
        assertFalse(permissionPage.isEmpty());
        assertTrue(permissionPage.getPageable().isUnpaged());
        assertEquals(permissionPage.getContent(), permissions);
        verify(repo).findAll(Pageable.unpaged());
    }

    @Test
    void existsById() {
        UUID id = PermissionProvider.singleEntity().getId();
        when(repo.existsById(id)).thenReturn(false);
        assertFalse(dao.existsById(id));
        verify(repo).existsById(id);

        UUID idSecond = PermissionProvider.alternativeEntity().getId();
        when(repo.existsById(idSecond)).thenReturn(true);
        assertTrue(dao.existsById(idSecond));
        verify(repo).existsById(idSecond);
    }

    @Test
    void existsAllByIds() {
        List<Permission> permissions =  PermissionProvider.listEntities();
        List<UUID> ids = permissions.stream().map(Permission::getId).toList();
        when(repo.countByIds(ids)).thenReturn((long) ids.size());
        assertTrue(dao.existsAllByIds(ids));
        verify(repo).countByIds(ids);

        when(repo.countByIds(ids)).thenReturn(1L);
        assertFalse(dao.existsAllByIds(ids));
        verify(repo, times(2)).countByIds(ids);
    }

    @Test
    void updateById() {
        UUID initialId = PermissionProvider.alternativeEntity().getId();
        Permission initialPermission = PermissionProvider.alternativeEntity();
        when(repo.findById(initialId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> dao.updateById(initialPermission, initialId));
        verify(repo).findById(initialId);

        UUID idSecond = PermissionProvider.alternativeEntity().getId();
        Permission permissionSecond = PermissionProvider.alternativeEntity();
        Permission newPermissionSecond = new Permission();
        newPermissionSecond.setPermissionName(null);
        when(repo.findById(idSecond)).thenReturn(Optional.of(permissionSecond));
        when(repo.save(permissionSecond)).thenReturn(permissionSecond);
        Permission permissionUpdatedSecond = dao.updateById(newPermissionSecond, idSecond);
        assertNotNull(permissionUpdatedSecond);
        assertEquals(permissionSecond.getId(), permissionUpdatedSecond.getId());
        assertNotEquals(newPermissionSecond.getPermissionName(), permissionUpdatedSecond.getPermissionName());
        assertNotEquals(newPermissionSecond.getId(), permissionUpdatedSecond.getId());
        verify(repo, times(2)).findById(idSecond);
        verify(repo).save(permissionSecond);

        UUID idThird = PermissionProvider.alternativeEntity().getId();
        Permission permissionThird = PermissionProvider.alternativeEntity();
        Permission newPermissionThird = new Permission();
        newPermissionThird.setPermissionName("  ");
        when(repo.findById(idThird)).thenReturn(Optional.of(permissionThird));
        when(repo.save(permissionThird)).thenReturn(permissionThird);
        Permission permissionUpdatedThird = dao.updateById(newPermissionThird, idThird);
        assertNotNull(permissionUpdatedThird);
        assertEquals(permissionThird.getId(), permissionUpdatedThird.getId());
        assertNotEquals(newPermissionThird.getPermissionName(), permissionUpdatedThird.getPermissionName());
        assertNotEquals(newPermissionThird.getId(), permissionUpdatedThird.getId());
        verify(repo, times(3)).findById(idThird);
        verify(repo).save(permissionThird);

        UUID id = PermissionProvider.singleEntity().getId();
        Permission permission = PermissionProvider.singleEntity();
        Permission newPermission = PermissionProvider.alternativeEntity();
        when(repo.findById(id)).thenReturn(Optional.of(permission));
        when(repo.save(permission)).thenReturn(permission);
        Permission permissionUpdated = dao.updateById(newPermission, id);
        assertNotNull(permissionUpdated);
        assertEquals(permission.getId(), permissionUpdated.getId());
        assertEquals(newPermission.getPermissionName(), permissionUpdated.getPermissionName());
        assertNotEquals(newPermission.getId(), permissionUpdated.getId());
        verify(repo).findById(id);
        verify(repo).save(permission);
    }

    @Test
    void existsByUniqueProperties() {
        Permission firstPermission = new Permission();
        firstPermission.setPermissionName(null);
        assertFalse(dao.existsByUniqueProperties(firstPermission));

        Permission secondPermission = new Permission();
        secondPermission.setPermissionName("  ");
        assertFalse(dao.existsByUniqueProperties(secondPermission));

        Permission permissionSecond = PermissionProvider.alternativeEntity();
        when(repo.existsByPermissionName(permissionSecond.getPermissionName())).thenReturn(false);
        assertFalse(dao.existsByUniqueProperties(permissionSecond));
        verify(repo).existsByPermissionName(permissionSecond.getPermissionName());

        Permission permission = PermissionProvider.singleEntity();
        when(repo.existsByPermissionName(permission.getPermissionName())).thenReturn(true);
        assertTrue(dao.existsByUniqueProperties(permission));
        verify(repo).existsByPermissionName(permission.getPermissionName());

    }

    @Test
    void findByPermissionName() {
        Permission permission = PermissionProvider.singleEntity();
        when(repo.findByPermissionName(permission.getPermissionName()))
                .thenReturn(Optional.of(permission));
        Permission permissionFound = dao.findByPermissionName(permission.getPermissionName())
                .orElse(null);
        assertNotNull(permissionFound);
        assertEquals(permission.getId(), permissionFound.getId());
        assertEquals(permission.getPermissionName(), permissionFound.getPermissionName());
        verify(repo).findByPermissionName(permission.getPermissionName());
    }
}