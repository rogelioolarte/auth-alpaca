package com.alpaca.integration.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alpaca.entity.Permission;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IPermissionDAO;
import com.alpaca.resources.PermissionProvider;
import com.alpaca.service.IPermissionService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith(SpringExtension.class)
class PermissionServiceImplIT {

  @Autowired private IPermissionDAO dao;

  @Autowired private IPermissionService service;

  @Test
  void findById() {
    assertThrows(BadRequestException.class, () -> service.findById(null));

    UUID randomId = UUID.randomUUID();
    assertThrows(NotFoundException.class, () -> this.service.findById(randomId));

    Permission permission = PermissionProvider.alternativeEntity();
    service.save(permission);
    Permission permissionFound = service.findById(permission.getId());
    assertNotNull(permissionFound);
    assertEquals(permission.getId(), permissionFound.getId());
    assertEquals(permissionFound, permission);
  }

  @Test
  void findAllByIds() {
    // Test con entrada nula o vacía
    assertThrows(BadRequestException.class, () -> service.findAllByIds(null));
    assertThrows(BadRequestException.class, () -> service.findAllByIds(Collections.emptyList()));

    // Test con IDs inválidos
    UUID id = UUID.fromString("c06f3206-c469-4216-bbc7-77fed3a8a133");
    List<UUID> uuids = new ArrayList<>();
    uuids.add(id);
    uuids.add(null);
    assertThrows(BadRequestException.class, () -> service.findAllByIds(uuids));

    // Test con IDs válidos
    UUID secondId = UUID.fromString("b1f383ce-4c1e-4d0e-bb43-a9674377c4a3");
    Permission permission = PermissionProvider.singleEntity();
    dao.save(permission); // Guardamos el permiso en la base de datos
    when(dao.existsAllByIds(List.of(permission.getId()))).thenReturn(true);
    when(dao.findAllByIds(List.of(permission.getId()))).thenReturn(List.of(permission));

    List<Permission> permissions =
        service.findAllByIds(new ArrayList<>(List.of(permission.getId())));
    assertNotNull(permissions);
    assertFalse(permissions.isEmpty());
    assertEquals(new ArrayList<>(List.of(permission)), permissions);
  }

  @Test
  void save() {
    // Test con permiso nulo
    assertThrows(BadRequestException.class, () -> service.save(null));

    // Test con permiso existente
    Permission permission = PermissionProvider.singleEntity();
    dao.save(permission); // Guardamos el permiso en la base de datos
    assertThrows(BadRequestException.class, () -> service.save(permission));

    // Test con permiso nuevo
    Permission permissionSecond = PermissionProvider.alternativeEntity();
    when(dao.existsByUniqueProperties(permissionSecond)).thenReturn(false);
    Permission savedPermission = service.save(permissionSecond);
    assertNotNull(savedPermission);
    assertEquals(permissionSecond.getId(), savedPermission.getId());
  }

  @Test
  void updateById() {
    assertThrows(BadRequestException.class, () -> service.updateById(null, null));

    UUID id = UUID.fromString("c06f3206-c469-4216-bbc7-77fed3a8a122");
    Permission permission = PermissionProvider.singleEntity();
    dao.save(permission); // Guardamos el permiso en la base de datos

    Permission permissionSecond = PermissionProvider.alternativeEntity();
    UUID idSecond = permissionSecond.getId();
    dao.save(permissionSecond); // Guardamos el permiso actualizado en la base de datos

    Permission updatedPermission = service.updateById(permissionSecond, idSecond);
    assertNotNull(updatedPermission);
    assertEquals(permissionSecond.getPermissionName(), updatedPermission.getPermissionName());
  }

  @Test
  void deleteById() {
    assertThrows(BadRequestException.class, () -> service.deleteById(null));

    UUID id = UUID.fromString("c06f3206-c469-4216-bbc7-77fed3a8a133");
    Permission permission = PermissionProvider.singleEntity();
    dao.save(permission); // Guardamos el permiso en la base de datos

    when(dao.existsById(id)).thenReturn(false);
    assertThrows(BadRequestException.class, () -> service.deleteById(id));

    // Eliminar permiso de la base de datos
    dao.deleteById(permission.getId());
    assertFalse(dao.existsById(permission.getId()));
  }

  @Test
  void findAll() {
    List<Permission> initialPermissions = PermissionProvider.listEntities();
    for (Permission permission : initialPermissions) {
      dao.save(permission); // Guardamos los permisos en la base de datos
    }

    List<Permission> permissions = service.findAll();
    assertNotNull(permissions);
    assertFalse(permissions.isEmpty());
    assertEquals(initialPermissions.size(), permissions.size());
  }

  @Test
  void findAllPage() {
    List<Permission> permissions = PermissionProvider.listEntities();
    for (Permission permission : permissions) {
      dao.save(permission); // Guardamos los permisos en la base de datos
    }

    Page<Permission> permissionPage = service.findAllPage(Pageable.unpaged());
    assertNotNull(permissionPage);
    assertFalse(permissionPage.isEmpty());
    assertTrue(permissionPage.getPageable().isUnpaged());
  }

  @Test
  void existsById() {
    UUID id = UUID.fromString("c06f3206-c469-4216-bbc7-77fed3a8a133");
    Permission permission = PermissionProvider.singleEntity();
    dao.save(permission); // Guardamos el permiso en la base de datos

    assertTrue(service.existsById(permission.getId()));
    assertFalse(service.existsById(UUID.randomUUID())); // ID no existente
  }
}
