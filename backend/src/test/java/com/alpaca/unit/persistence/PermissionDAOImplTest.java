package com.alpaca.unit.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alpaca.entity.Permission;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.impl.PermissionDAOImpl;
import com.alpaca.repository.PermissionRepo;
import com.alpaca.resources.PermissionProvider;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
class PermissionDAOImplTest {

  @Mock private PermissionRepo repo;

  @InjectMocks private PermissionDAOImpl dao;

  private Permission firstEntity;
  private Permission secondEntity;
  private List<Permission> entities;
  private final List<UUID> ids =
      PermissionProvider.listEntities().stream()
          .map(Permission::getId)
          .collect(Collectors.toList());

  @BeforeEach
  void setup() {
    firstEntity = PermissionProvider.singleEntity();
    secondEntity = PermissionProvider.alternativeEntity();
    entities = PermissionProvider.listEntities();
  }

  @Test
  void findByIdCaseOne() {
    when(repo.findById(firstEntity.getId())).thenReturn(Optional.of(firstEntity));
    Permission result = dao.findById(firstEntity.getId()).orElse(null);
    assertNotNull(result);
    assertEquals(firstEntity, result);
    verify(repo).findById(firstEntity.getId());
  }

  @Test
  void findAllByIdsCaseOne() {
    when(repo.findAllById(ids)).thenReturn(entities);
    List<Permission> result = dao.findAllByIds(ids);
    assertNotNull(result);
    assertEquals(entities, result);
    verify(repo).findAllById(ids);
  }

  @Test
  void deleteByIdCaseOne() {
    dao.deleteById(firstEntity.getId());
    ArgumentCaptor<UUID> idAC = ArgumentCaptor.forClass(UUID.class);
    verify(repo).deleteById(firstEntity.getId());
    verify(repo).deleteById(idAC.capture());
    assertEquals(firstEntity.getId(), idAC.getValue());
  }

  @Test
  void saveCaseOne() {
    when(repo.save(firstEntity)).thenReturn(firstEntity);
    dao.save(firstEntity);
    ArgumentCaptor<Permission> pAC = ArgumentCaptor.forClass(Permission.class);
    verify(repo).save(firstEntity);
    verify(repo).save(pAC.capture());
    assertEquals(firstEntity, pAC.getValue());
  }

  @Test
  void saveAllCaseOne() {
    when(repo.saveAll(entities)).thenReturn(entities);
    List<Permission> result = dao.saveAll(entities);
    assertNotNull(result);
    assertEquals(entities, result);
    verify(repo).saveAll(entities);
  }

  @Test
  void findAllCaseOne() {
    when(repo.findAll()).thenReturn(entities);
    List<Permission> result = dao.findAll();
    assertNotNull(result);
    assertEquals(entities, result);
    verify(repo).findAll();
  }

  @Test
  void findAllPageCaseOne() {
    when(repo.findAll(Pageable.unpaged())).thenReturn(new PageImpl<>(entities));
    Page<Permission> result = dao.findAllPage(Pageable.unpaged());
    assertNotNull(result);
    assertEquals(entities, result.getContent());
    verify(repo).findAll(Pageable.unpaged());
  }

  @Test
  void existsByIdCaseOne() {
    when(repo.existsById(firstEntity.getId())).thenReturn(false);
    assertFalse(dao.existsById(firstEntity.getId()));
    verify(repo).existsById(firstEntity.getId());
  }

  @Test
  void existsByIdCaseTwo() {
    when(repo.existsById(secondEntity.getId())).thenReturn(true);
    assertTrue(dao.existsById(secondEntity.getId()));
    verify(repo).existsById(secondEntity.getId());
  }

  @Test
  void existsAllByIdsCaseOne() {
    when(repo.countByIds(ids)).thenReturn((long) ids.size());
    assertTrue(dao.existsAllByIds(ids));
    verify(repo).countByIds(ids);
  }

  @Test
  void existsAllByIdsCaseTwo() {
    when(repo.countByIds(ids)).thenReturn(1L);
    assertFalse(dao.existsAllByIds(ids));
    verify(repo).countByIds(ids);
  }

  @Test
  void updateByIdCaseOne() {
    when(repo.findById(secondEntity.getId())).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> dao.updateById(secondEntity, secondEntity.getId()));
    verify(repo).findById(secondEntity.getId());
  }

  @Test
  void updateByIdCaseTwo() {
    Permission emptyName = new Permission();
    emptyName.setPermissionName(null);
    when(repo.findById(secondEntity.getId())).thenReturn(Optional.of(secondEntity));
    when(repo.save(secondEntity)).thenReturn(secondEntity);
    Permission result = dao.updateById(emptyName, secondEntity.getId());
    assertNotNull(result);
    assertEquals(secondEntity.getId(), result.getId());
    verify(repo).findById(secondEntity.getId());
    verify(repo).save(secondEntity);
  }

  @Test
  void updateByIdCaseThree() {
    Permission blankName = new Permission();
    blankName.setPermissionName("  ");
    when(repo.findById(secondEntity.getId())).thenReturn(Optional.of(secondEntity));
    when(repo.save(secondEntity)).thenReturn(secondEntity);
    Permission result = dao.updateById(blankName, secondEntity.getId());
    assertNotNull(result);
    assertEquals(secondEntity.getId(), result.getId());
    verify(repo).findById(secondEntity.getId());
    verify(repo).save(secondEntity);
  }

  @Test
  void updateByIdCaseFour() {
    Permission updated = new Permission();
    updated.setPermissionName("New Name");
    when(repo.findById(firstEntity.getId())).thenReturn(Optional.of(firstEntity));
    when(repo.save(firstEntity)).thenReturn(firstEntity);
    Permission result = dao.updateById(updated, firstEntity.getId());
    assertNotNull(result);
    assertEquals(firstEntity.getId(), result.getId());
    assertEquals(updated.getPermissionName(), result.getPermissionName());
    verify(repo).findById(firstEntity.getId());
    verify(repo).save(firstEntity);
  }

  @Test
  void existsByUniquePropertiesCaseOne() {
    Permission test = new Permission();
    test.setPermissionName(null);
    assertFalse(dao.existsByUniqueProperties(test));
  }

  @Test
  void existsByUniquePropertiesCaseTwo() {
    Permission test = new Permission();
    test.setPermissionName("   ");
    assertFalse(dao.existsByUniqueProperties(test));
  }

  @Test
  void existsByUniquePropertiesCaseThree() {
    when(repo.existsByPermissionName(secondEntity.getPermissionName())).thenReturn(false);
    assertFalse(dao.existsByUniqueProperties(secondEntity));
    verify(repo).existsByPermissionName(secondEntity.getPermissionName());
  }

  @Test
  void existsByUniquePropertiesCaseFour() {
    when(repo.existsByPermissionName(firstEntity.getPermissionName())).thenReturn(true);
    assertTrue(dao.existsByUniqueProperties(firstEntity));
    verify(repo).existsByPermissionName(firstEntity.getPermissionName());
  }

  @Test
  void findByPermissionNameCaseOne() {
    when(repo.findByPermissionName(firstEntity.getPermissionName()))
        .thenReturn(Optional.of(firstEntity));
    Permission result = dao.findByPermissionName(firstEntity.getPermissionName()).orElse(null);
    assertNotNull(result);
    assertEquals(firstEntity, result);
    verify(repo).findByPermissionName(firstEntity.getPermissionName());
  }
}
