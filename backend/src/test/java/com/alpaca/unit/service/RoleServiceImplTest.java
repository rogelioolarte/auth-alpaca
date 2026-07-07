package com.alpaca.unit.service;

import com.alpaca.entity.Role;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.impl.RoleDAOImpl;
import com.alpaca.resources.provider.RoleProvider;
import com.alpaca.service.impl.RoleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Unit tests for {@link RoleServiceImpl}. */
@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock private RoleDAOImpl dao;

    @InjectMocks private RoleServiceImpl service;

    private Role firstEntity;
    private Role secondEntity;

    @BeforeEach
    void setup() {
        firstEntity = RoleProvider.singleEntity();
        secondEntity = RoleProvider.alternativeEntity();

        secondEntity.setName("TEST_USER");
    }

    // --- save ---

    @Test
    void saveShouldThrowBadRequestExceptionWhenRoleIsNull() {
        assertThrows(BadRequestException.class, () -> service.save(null));

        verify(dao).existsByUniqueProperties(null);
    }

    @Test
    void saveShouldThrowBadRequestExceptionWhenRoleAlreadyExists() {
        when(dao.existsByUniqueProperties(firstEntity)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.save(firstEntity));

        verify(dao).existsByUniqueProperties(firstEntity);
        verify(dao, never()).save(any(Role.class));
    }

    @Test
    void saveShouldPersistRoleSuccessfully() {
        when(dao.save(secondEntity)).thenReturn(secondEntity);

        Role result = service.save(secondEntity);

        assertNotNull(result);
        assertEquals(secondEntity, result);

        verify(dao).save(secondEntity);
    }

    // --- getUserRoles ---

    @Test
    void getUserRolesShouldThrowNotFoundExceptionWhenDefaultRoleDoesNotExist() {
        when(dao.findByRoleName("USER")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getUserRoles());

        verify(dao).findByRoleName("USER");
        verify(dao, never()).save(any(Role.class));
    }

    @Test
    void getUserRolesShouldReturnDefaultRoleSuccessfully() {
        when(dao.findByRoleName("USER")).thenReturn(Optional.of(secondEntity));

        Set<Role> result = service.getUserRoles();

        assertNotNull(result);
        assertEquals(new HashSet<>(Set.of(secondEntity)), result);

        verify(dao).findByRoleName("USER");
    }

    // --- findByRoleName ---

    @Test
    void findByRoleNameShouldThrowBadRequestExceptionWhenRoleNameIsNull() {
        assertThrows(BadRequestException.class, () -> service.findByRoleName(null));

        verifyNoInteractions(dao);
    }

    @Test
    void findByRoleNameShouldThrowBadRequestExceptionWhenRoleNameIsBlank() {
        assertThrows(BadRequestException.class, () -> service.findByRoleName("  "));

        verifyNoInteractions(dao);
    }

    @Test
    void findByRoleNameShouldThrowNotFoundExceptionWhenRoleDoesNotExist() {
        String roleName = secondEntity.getName();

        when(dao.findByRoleName(roleName)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.findByRoleName(roleName));

        verify(dao).findByRoleName(roleName);
    }

    @Test
    void findByRoleNameShouldReturnRoleSuccessfully() {
        String roleName = firstEntity.getName();

        when(dao.findByRoleName(roleName)).thenReturn(Optional.of(firstEntity));

        Role result = service.findByRoleName(roleName);

        assertNotNull(result);
        assertEquals(firstEntity, result);

        verify(dao).findByRoleName(roleName);
    }

    // --- updateById ---

    @Test
    void updateByIdShouldThrowBadRequestExceptionWhenRoleIsNull() {
        UUID roleId = firstEntity.getId();

        assertThrows(BadRequestException.class, () -> service.updateById(null, roleId));

        verifyNoInteractions(dao);
    }

    @Test
    void updateByIdShouldThrowBadRequestExceptionWhenIdIsNull() {
        assertThrows(BadRequestException.class, () -> service.updateById(firstEntity, null));

        verifyNoInteractions(dao);
    }

    @Test
    void updateByIdShouldThrowNotFoundExceptionWhenRoleDoesNotExist() {
        UUID roleId = firstEntity.getId();

        when(dao.findById(roleId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.updateById(firstEntity, roleId));

        verify(dao).findById(roleId);
        verify(dao, never()).save(any(Role.class));
    }

    @Test
    void updateByIdShouldUpdateNameAndDescriptionSuccessfully() {
        Role existingRole = RoleProvider.singleEntity();

        Role incomingRole = RoleProvider.alternativeEntity();
        incomingRole.setRolePermissions(existingRole.getPermissions());

        UUID roleId = existingRole.getId();

        when(dao.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(dao.save(existingRole)).thenReturn(existingRole);

        Role result = service.updateById(incomingRole, roleId);

        assertNotNull(result);
        assertEquals(incomingRole.getName(), result.getName());
        assertEquals(incomingRole.getDescription(), result.getDescription());

        ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);

        verify(dao).findById(roleId);
        verify(dao).save(roleCaptor.capture());

        assertEquals(incomingRole.getName(), roleCaptor.getValue().getName());
        assertEquals(incomingRole.getDescription(), roleCaptor.getValue().getDescription());
    }

    @Test
    void updateByIdShouldNotUpdateNameWhenIncomingNameIsBlank() {
        Role existingRole = RoleProvider.singleEntity();

        Role incomingRole = RoleProvider.alternativeEntity();
        incomingRole.setName(" ");
        incomingRole.setDescription(existingRole.getDescription());
        incomingRole.setRolePermissions(existingRole.getPermissions());

        UUID roleId = existingRole.getId();

        when(dao.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(dao.save(existingRole)).thenReturn(existingRole);

        Role result = service.updateById(incomingRole, roleId);

        assertNotNull(result);
        assertEquals(firstEntity.getName(), result.getName());

        verify(dao).findById(roleId);
        verify(dao).save(existingRole);
    }

    @Test
    void updateByIdShouldNotUpdateDescriptionWhenIncomingDescriptionIsBlank() {
        Role existingRole = RoleProvider.singleEntity();

        Role incomingRole = RoleProvider.alternativeEntity();
        incomingRole.setName(existingRole.getName());
        incomingRole.setDescription(" ");
        incomingRole.setRolePermissions(existingRole.getPermissions());

        UUID roleId = existingRole.getId();

        when(dao.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(dao.save(existingRole)).thenReturn(existingRole);

        Role result = service.updateById(incomingRole, roleId);

        assertNotNull(result);
        assertEquals(firstEntity.getDescription(), result.getDescription());

        verify(dao).findById(roleId);
        verify(dao).save(existingRole);
    }

    @Test
    void updateByIdShouldUpdatePermissionsWhenPermissionsAreDifferent() {
        Role existingRole = RoleProvider.singleEntity();

        Role incomingRole = RoleProvider.alternativeEntity();

        UUID roleId = existingRole.getId();

        when(dao.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(dao.save(existingRole)).thenReturn(existingRole);

        Role result = service.updateById(incomingRole, roleId);

        assertNotNull(result);
        assertEquals(incomingRole.getPermissions(), result.getPermissions());

        ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);

        verify(dao).findById(roleId);
        verify(dao).save(roleCaptor.capture());

        assertEquals(incomingRole.getPermissions(), roleCaptor.getValue().getPermissions());
    }

    @Test
    void updateByIdShouldNotUpdatePermissionsWhenPermissionsAreEqual() {
        Role existingRole = RoleProvider.singleEntity();

        Role incomingRole = RoleProvider.alternativeEntity();
        incomingRole.setRolePermissions(existingRole.getPermissions());

        UUID roleId = existingRole.getId();

        when(dao.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(dao.save(existingRole)).thenReturn(existingRole);

        Role result = service.updateById(incomingRole, roleId);

        assertNotNull(result);
        assertEquals(existingRole.getRolePermissions(), result.getRolePermissions());

        verify(dao).findById(roleId);
        verify(dao).save(existingRole);
    }

    @Test
    void updateByIdShouldNotUpdatePermissionsWhenIncomingPermissionsAreNull() {
        Role existingRole = RoleProvider.singleEntity();

        Role incomingRole = RoleProvider.alternativeEntity();
        incomingRole.setRolePermissions(null);

        UUID roleId = existingRole.getId();

        when(dao.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(dao.save(existingRole)).thenReturn(existingRole);

        Role result = service.updateById(incomingRole, roleId);

        assertNotNull(result);
        assertEquals(existingRole.getRolePermissions(), result.getRolePermissions());

        verify(dao).findById(roleId);
        verify(dao).save(existingRole);
    }
}
