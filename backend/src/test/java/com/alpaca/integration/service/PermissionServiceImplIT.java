package com.alpaca.integration.service;

import com.alpaca.entity.Permission;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.resources.PermissionProvider;
import com.alpaca.service.impl.PermissionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Integration tests for {@link PermissionServiceImpl} */
@SpringBootTest
@ExtendWith(SpringExtension.class)
class PermissionServiceImplIT {

  @Autowired private PermissionServiceImpl service;

  @Test
  @Transactional
  void findById() {
    assertThrows(BadRequestException.class, () -> service.findById(null));

    UUID randomId = UUID.randomUUID();
    assertThrows(NotFoundException.class, () -> this.service.findById(randomId));

    String permissionName = PermissionProvider.alternativeEntity().getPermissionName();
    Permission permission = service.save(new Permission(permissionName));
    Permission permissionFound = service.findById(permission.getId());
    assertNotNull(permissionFound);
    assertEquals(permission.getId(), permissionFound.getId());
    assertEquals(permissionFound, permission);
  }

  @Test
  @Transactional
  void findAllByIds() {
    assertThrows(BadRequestException.class, () -> service.findAllByIds(null));
    assertThrows(BadRequestException.class, () -> service.findAllByIds(Collections.emptyList()));

    UUID id = UUID.fromString("c06f3206-c469-4216-bbc7-77fed3a8a133");
    List<UUID> uuids = new ArrayList<>();
    uuids.add(id);
    uuids.add(null);
    assertThrows(BadRequestException.class, () -> service.findAllByIds(uuids));

    UUID secondId = UUID.fromString("b1f383ce-4c1e-4d0e-bb43-a9674377c4a3");

    String permissionName = PermissionProvider.singleEntity().getPermissionName();
    Permission permission = service.save(new Permission(permissionName));
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

    String permissionName = PermissionProvider.singleEntity().getPermissionName();
    service.save(new Permission(permissionName));
    assertThrows(BadRequestException.class, () -> service.save(new Permission(permissionName)));

    String permissionNameSecond = PermissionProvider.alternativeEntity().getPermissionName();
    Permission permissionSecond = new Permission(permissionNameSecond);
    Permission savedPermission = service.save(permissionSecond);
    assertNotNull(savedPermission);
    assertEquals(permissionSecond.getId(), savedPermission.getId());
  }

  @Test
  @Transactional
  void updateById() {
    assertThrows(BadRequestException.class, () -> service.updateById(null, null));

    String permissionName = PermissionProvider.singleEntity().getPermissionName();
    Permission permission = service.save(new Permission(permissionName));

    String permissionNameSecond = PermissionProvider.alternativeEntity().getPermissionName();
    Permission permissionSecond = service.save(new Permission(permissionNameSecond));

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

    String permissionName = PermissionProvider.singleEntity().getPermissionName();
    Permission permission = service.save(new Permission(permissionName));
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
    String permissionNameSecond = PermissionProvider.alternativeEntity().getPermissionName();
    Permission permission = service.save(new Permission(permissionNameSecond));
    assertTrue(service.existsById(permission.getId()));
    assertFalse(service.existsById(UUID.randomUUID()));
  }
}
