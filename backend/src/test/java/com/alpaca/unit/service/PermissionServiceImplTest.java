package com.alpaca.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alpaca.entity.Permission;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.impl.PermissionDAOImpl;
import com.alpaca.resources.PermissionProvider;
import com.alpaca.service.impl.PermissionServiceImpl;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

  @Mock private PermissionDAOImpl dao;

  @InjectMocks private PermissionServiceImpl service;

  private Permission firstEntity;
  private Permission secondEntity;
  private List<Permission> entities;
  private List<UUID> ids;

  @BeforeEach
  void setup() {
    firstEntity = PermissionProvider.singleEntity();
    secondEntity = PermissionProvider.alternativeEntity();
    entities = PermissionProvider.listEntities();
    ids = entities.stream().map(Permission::getId).collect(Collectors.toList());
  }

  // --- findById ---
  @Test
  void findByIdCaseOne() {
    assertThrows(BadRequestException.class, () -> service.findById(null));
  }

  @Test
  void findByIdCaseTwo() {
    UUID randomId = UUID.randomUUID();
    when(dao.findById(randomId)).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> service.findById(randomId));
    verify(dao).findById(randomId);
  }

  @Test
  void findByIdCaseThree() {
    when(dao.findById(secondEntity.getId())).thenReturn(Optional.of(secondEntity));
    Permission result = service.findById(secondEntity.getId());
    assertNotNull(result);
    assertEquals(secondEntity, result);
    verify(dao).findById(secondEntity.getId());
  }

  // --- findAllByIds ---
  @Test
  void findAllByIdsCaseOne() {
    assertThrows(BadRequestException.class, () -> service.findAllByIds(null));
  }

  @Test
  void findAllByIdsCaseTwo() {
    assertThrows(BadRequestException.class, () -> service.findAllByIds(Collections.emptyList()));
  }

  @Test
  void findAllByIdsCaseThree() {
    List<UUID> invalidIds = new ArrayList<>(ids);
    invalidIds.add(null);
    assertThrows(BadRequestException.class, () -> service.findAllByIds(invalidIds));
  }

  @Test
  void findAllByIdsCaseFour() {
    List<UUID> someIds = List.of(secondEntity.getId());
    when(dao.existsAllByIds(someIds)).thenReturn(false);
    assertThrows(NotFoundException.class, () -> service.findAllByIds(new ArrayList<>(someIds)));
    verify(dao).existsAllByIds(someIds);
  }

  @Test
  void findAllByIdsCaseFive() {
    List<UUID> someIds = List.of(firstEntity.getId());
    when(dao.existsAllByIds(someIds)).thenReturn(true);
    when(dao.findAllByIds(someIds)).thenReturn(List.of(firstEntity));
    List<Permission> result = service.findAllByIds(new ArrayList<>(someIds));
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(firstEntity, result.getFirst());
    verify(dao).existsAllByIds(someIds);
    verify(dao).findAllByIds(someIds);
  }

  // --- findAllByIdsToSet ---
  @Test
  void findAllByIdsToSetCaseOne() {
    assertThrows(BadRequestException.class, () -> service.findAllByIdsToSet(null));
  }

  @Test
  void findAllByIdsToSetCaseTwo() {
    assertThrows(
        BadRequestException.class, () -> service.findAllByIdsToSet(Collections.emptyList()));
  }

  @Test
  void findAllByIdsToSetCaseThree() {
    UUID id = UUID.fromString("c06f3206-c469-4216-bbc7-77fed3a8a133");
    List<UUID> uuids = new ArrayList<>();
    uuids.add(id);
    uuids.add(null);
    assertThrows(BadRequestException.class, () -> service.findAllByIdsToSet(uuids));
  }

  @Test
  void findAllByIdsToSetCaseFour() {
    UUID secondId = UUID.fromString("b1f383ce-4c1e-4d0e-bb43-a9674377c4b3");
    when(dao.existsAllByIds(List.of(secondId))).thenReturn(false);
    assertThrows(
        NotFoundException.class,
        () -> service.findAllByIdsToSet(new ArrayList<>(List.of(secondId))));
  }

  @Test
  void findAllByIdsToSetCaseFive() {
    when(dao.existsAllByIds(List.of(firstEntity.getId()))).thenReturn(true);
    when(dao.findAllByIds(List.of(firstEntity.getId()))).thenReturn(List.of(firstEntity));
    Set<Permission> permissions =
        service.findAllByIdsToSet(new ArrayList<>(List.of(firstEntity.getId())));
    assertNotNull(permissions);
    assertFalse(permissions.isEmpty());
    assertEquals(Set.of(firstEntity), permissions);
    verify(dao).existsAllByIds(List.of(firstEntity.getId()));
    verify(dao).findAllByIds(List.of(firstEntity.getId()));
  }

  // --- save ---
  @Test
  void saveCaseOne() {
    assertThrows(BadRequestException.class, () -> service.save(null));
  }

  @Test
  void saveCaseTwo() {
    when(dao.existsByUniqueProperties(firstEntity)).thenReturn(true);
    assertThrows(BadRequestException.class, () -> service.save(firstEntity));
    verify(dao).existsByUniqueProperties(firstEntity);
  }

  @Test
  void saveCaseThree() {
    when(dao.existsByUniqueProperties(secondEntity)).thenReturn(false);
    when(dao.save(secondEntity)).thenReturn(secondEntity);
    service.save(secondEntity);
    verify(dao).existsByUniqueProperties(secondEntity);
    ArgumentCaptor<Permission> captor = ArgumentCaptor.forClass(Permission.class);
    verify(dao).save(captor.capture());
    assertEquals(secondEntity.getId(), captor.getValue().getId());
  }

  // --- saveAll ---
  @Test
  void saveAllCaseOne() {
    assertThrows(BadRequestException.class, () -> service.saveAll(null));
  }

  @Test
  void saveAllCaseTwo() {
    assertThrows(BadRequestException.class, () -> service.saveAll(Collections.emptyList()));
  }

  @Test
  void saveAllCaseThree() {
    List<Permission> invalidList = new ArrayList<>(entities);
    invalidList.add(null);
    assertThrows(BadRequestException.class, () -> service.saveAll(invalidList));
  }

  @Test
  void saveAllCaseFour() {
    when(dao.saveAll(entities)).thenReturn(entities);
    List<Permission> result = service.saveAll(entities);
    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals(entities, result);
    verify(dao).saveAll(entities);
  }

  // --- updateById ---
  @Test
  void updateByIdCaseOne() {
    assertThrows(BadRequestException.class, () -> service.updateById(null, null));
  }

  @Test
  void updateByIdCaseTwo() {
    UUID id = UUID.randomUUID();
    assertThrows(BadRequestException.class, () -> service.updateById(null, id));
  }

  @Test
  void updateByIdCaseThree() {
    assertThrows(BadRequestException.class, () -> service.updateById(firstEntity, null));
  }

  @Test
  void updateByIdCaseFour() {
    UUID id = UUID.randomUUID();
    when(dao.updateById(firstEntity, id)).thenReturn(null);
    assertThrows(BadRequestException.class, () -> service.updateById(firstEntity, id));
    verify(dao).updateById(firstEntity, id);
  }

  @Test
  void updateByIdCaseFive() {
    UUID id = secondEntity.getId();
    when(dao.updateById(secondEntity, id)).thenReturn(secondEntity);
    service.updateById(secondEntity, id);
    ArgumentCaptor<UUID> idCaptor = ArgumentCaptor.forClass(UUID.class);
    ArgumentCaptor<Permission> permissionCaptor = ArgumentCaptor.forClass(Permission.class);
    verify(dao).updateById(permissionCaptor.capture(), idCaptor.capture());
    assertEquals(secondEntity.getPermissionName(), permissionCaptor.getValue().getPermissionName());
  }

  // --- deleteById ---
  @Test
  void deleteByIdCaseOne() {
    assertThrows(BadRequestException.class, () -> service.deleteById(null));
  }

  @Test
  void deleteByIdCaseTwo() {
    UUID id = UUID.randomUUID();
    when(dao.existsById(id)).thenReturn(false);
    assertThrows(BadRequestException.class, () -> service.deleteById(id));
  }

  @Test
  void deleteByIdCaseThree() {
    UUID id = UUID.randomUUID();
    when(dao.existsById(id)).thenReturn(true);
    service.deleteById(id);
    ArgumentCaptor<UUID> idCaptor = ArgumentCaptor.forClass(UUID.class);
    verify(dao).existsById(id);
    verify(dao).deleteById(idCaptor.capture());
    assertEquals(id, idCaptor.getValue());
  }

  // --- findAll ---
  @Test
  void findAllCaseOne() {
    when(dao.findAll()).thenReturn(entities);
    List<Permission> result = service.findAll();
    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals(entities, result);
    verify(dao).findAll();
  }

  // --- findAllPage ---
  @Test
  void findAllPageCaseOne() {
    assertThrows(BadRequestException.class, () -> service.findAllPage(null));
  }

  @Test
  void findAllPageCaseTwo() {
    when(dao.findAllPage(Pageable.unpaged())).thenReturn(new PageImpl<>(entities));
    Page<Permission> page = service.findAllPage(Pageable.unpaged());
    assertNotNull(page);
    assertFalse(page.isEmpty());
    assertEquals(entities, page.getContent());
    verify(dao).findAllPage(Pageable.unpaged());
  }

  // --- existsById ---
  @Test
  void existsByIdCaseOne() {
    assertFalse(service.existsById(null));
  }

  @Test
  void existsByIdCaseTwo() {
    UUID id = UUID.randomUUID();
    when(dao.existsById(id)).thenReturn(true);
    assertTrue(service.existsById(id));
    verify(dao).existsById(id);
  }

  // --- existsAllByIds ---
  @Test
  void existsAllByIdsCaseOne() {
    assertFalse(service.existsAllByIds(null));
  }

  @Test
  void existsAllByIdsCaseTwo() {
    assertFalse(service.existsAllByIds(Collections.emptyList()));
  }

  @Test
  void existsAllByIdsCaseThree() {
    UUID id = UUID.fromString("c06f3206-c469-4216-bbc7-77fed3a8a133");
    List<UUID> uuids = new ArrayList<>();
    uuids.add(id);
    uuids.add(null);
    assertFalse(service.existsAllByIds(uuids));
  }

  @Test
  void existsAllByIdsCaseFour() {
    UUID secondId = UUID.fromString("b1f383ce-4c1e-4d0e-bb43-a9674377c4a2");
    when(dao.existsAllByIds(List.of(secondId))).thenReturn(false);
    assertFalse(service.existsAllByIds(new ArrayList<>(List.of(secondId))));
    verify(dao).existsAllByIds(List.of(secondId));
  }

  @Test
  void existsAllByIdsCaseFive() {
    UUID thirdId = UUID.fromString("c06f3206-c469-4216-bbc7-77fed3a8a133");
    when(dao.existsAllByIds(List.of(thirdId))).thenReturn(true);
    assertTrue(service.existsAllByIds(new ArrayList<>(List.of(thirdId))));
    verify(dao).existsAllByIds(List.of(thirdId));
  }

  // --- existsByUniqueProperties ---
  @Test
  void existsByUniquePropertiesCaseOne() {
    assertFalse(service.existsByUniqueProperties(null));
  }

  @Test
  void existsByUniquePropertiesCaseTwo() {
    Permission permissionSecond = PermissionProvider.alternativeEntity();
    when(dao.existsByUniqueProperties(permissionSecond)).thenReturn(false);
    assertFalse(service.existsByUniqueProperties(permissionSecond));
    verify(dao).existsByUniqueProperties(permissionSecond);
  }

  @Test
  void existsByUniquePropertiesCaseThree() {
    Permission permission = PermissionProvider.singleEntity();
    when(dao.existsByUniqueProperties(permission)).thenReturn(true);
    assertTrue(service.existsByUniqueProperties(permission));
    verify(dao).existsByUniqueProperties(permission);
  }
}
