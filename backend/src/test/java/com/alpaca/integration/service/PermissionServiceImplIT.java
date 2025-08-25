package com.alpaca.integration.service;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.entity.Permission;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.resources.PermissionProvider;
import com.alpaca.service.impl.PermissionServiceImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link PermissionServiceImpl} */
@SpringBootTest
@ExtendWith(SpringExtension.class)
class PermissionServiceImplIT {

    @Autowired private PermissionServiceImpl service;

    private Permission singleEntity;
    private Permission alternativeEntity;

    @BeforeEach
    void setup() {
        singleEntity = new Permission(PermissionProvider.singleEntity().getPermissionName());
        alternativeEntity =
                new Permission(PermissionProvider.alternativeEntity().getPermissionName());
    }

    @Test
    @Transactional
    void findById() {
        assertThrows(BadRequestException.class, () -> service.findById(null));

        UUID randomId = UUID.randomUUID();
        assertThrows(NotFoundException.class, () -> this.service.findById(randomId));

        Permission permission = service.save(alternativeEntity);
        Permission permissionFound = service.findById(permission.getId());
        assertNotNull(permissionFound);
        assertEquals(permission.getId(), permissionFound.getId());
        assertEquals(permissionFound, permission);
    }

    @Test
    @Transactional
    void findAllByIds() {
        assertThrows(BadRequestException.class, () -> service.findAllByIds(null));
        assertThrows(
                BadRequestException.class, () -> service.findAllByIds(Collections.emptyList()));

        UUID id = UUID.fromString("c06f3206-c469-4216-bbc7-77fed3a8a133");
        List<UUID> uuids = new ArrayList<>();
        uuids.add(id);
        uuids.add(null);
        assertThrows(BadRequestException.class, () -> service.findAllByIds(uuids));

        Permission permission = service.save(singleEntity);
        List<Permission> permissions =
                service.findAllByIds(new ArrayList<>(List.of(permission.getId())));
        assertNotNull(permissions);
        assertFalse(permissions.isEmpty());
        assertEquals(new ArrayList<>(List.of(permission)), permissions);
    }

    @Test
    @Transactional
    void save() {
        assertThrows(BadRequestException.class, () -> service.save(null));

        service.save(singleEntity);
        assertThrows(BadRequestException.class, () -> service.save(singleEntity));

        Permission savedPermission = service.save(alternativeEntity);
        assertNotNull(savedPermission);
        assertEquals(alternativeEntity.getId(), savedPermission.getId());
    }

    @Test
    @Transactional
    void updateById() {
        assertThrows(BadRequestException.class, () -> service.updateById(null, null));

        Permission permission = service.save(singleEntity);
        Permission permissionSecond = service.save(alternativeEntity);

        Permission updatedPermission = service.updateById(permissionSecond, permission.getId());
        assertNotNull(updatedPermission);
        assertEquals(permission.getId(), updatedPermission.getId());
        assertEquals(permissionSecond.getPermissionName(), updatedPermission.getPermissionName());
    }

    @Test
    @Transactional
    void deleteById() {
        assertThrows(BadRequestException.class, () -> service.deleteById(null));

        UUID id = UUID.fromString("c06f3206-c469-4216-bbc7-77fed3a8a122");
        assertThrows(BadRequestException.class, () -> service.deleteById(id));

        Permission permission = service.save(singleEntity);
        service.deleteById(permission.getId());
        assertFalse(service.existsById(permission.getId()));
    }

    @Test
    @Transactional
    void findAll() {
        List<Permission> initialPermissions = PermissionProvider.listEntities();
        List<Permission> savedPermissions = new ArrayList<>();
        for (Permission permission : initialPermissions) {
            savedPermissions.add(service.save(permission));
        }

        List<Permission> permissions = service.findAll();
        assertNotNull(permissions);
        assertFalse(permissions.isEmpty());
        assertEquals(initialPermissions.size(), permissions.size());
        assertEquals(savedPermissions, permissions);
    }

    @Test
    @Transactional
    void findAllPage() {
        List<Permission> initialPermissions = PermissionProvider.listEntities();
        List<Permission> savedPermissions = new ArrayList<>();
        for (Permission permission : initialPermissions) {
            savedPermissions.add(service.save(permission));
        }
        Page<Permission> permissionPage = service.findAllPage(Pageable.unpaged());
        assertNotNull(permissionPage);
        assertFalse(permissionPage.isEmpty());
        assertTrue(permissionPage.getPageable().isUnpaged());
        assertEquals(savedPermissions, permissionPage.getContent());
    }

    @Test
    @Transactional
    void existsById() {
        Permission permission = service.save(alternativeEntity);
        assertTrue(service.existsById(permission.getId()));
        assertFalse(service.existsById(UUID.randomUUID()));
    }
}
