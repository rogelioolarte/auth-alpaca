package com.alpaca.unit.persistence;

import com.alpaca.entity.Permission;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.impl.PermissionDAOImpl;
import com.alpaca.repository.PermissionRepo;
import com.alpaca.resources.PermissionProvider;
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
        Permission entityInitial = PermissionProvider.singleEntity();
        when(repo.findById(entityInitial.getId()))
                .thenReturn(Optional.of(PermissionProvider.singleEntity()));
        Permission entity = dao.findById(entityInitial.getId()).orElse(null);
        assertNotNull(entity);
        assertEquals(entityInitial, entity);
        verify(repo).findById(entityInitial.getId());
    }

    @Test
    void findAllByIds() {
        List<Permission> entities = PermissionProvider.listEntities();
        when(repo.findAllById(entities
                .stream().map(Permission::getId).collect(Collectors.toSet())))
                .thenReturn(PermissionProvider.listEntities());
        List<Permission> entitiesFound = dao.findAllByIds(entities
                        .stream().map(Permission::getId).collect(Collectors.toSet()));
        assertNotNull(entitiesFound);
        assertEquals(entities.getFirst().getId(),
                entitiesFound.getFirst().getId());
        assertEquals(entities.getFirst().getPermissionName(),
                entitiesFound.getFirst().getPermissionName());
        assertEquals(entities.getLast().getId(),
                entitiesFound.getLast().getId());
        assertEquals(entities.getLast().getPermissionName(),
                entitiesFound.getLast().getPermissionName());
        verify(repo).findAllById(entities
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
        Permission entity = PermissionProvider.singleEntity();
        when(repo.save(entity)).thenReturn(entity);
        dao.save(entity);
        ArgumentCaptor<Permission> pAC = ArgumentCaptor.forClass(Permission.class);
        verify(repo).save(entity);
        verify(repo).save(pAC.capture());
        assertEquals(entity, pAC.getValue());
    }

    @Test
    void saveAll() {
        List<Permission> entities = PermissionProvider.listEntities();
        when(repo.saveAll(entities)).thenReturn(entities);
        List<Permission> saveEntities = dao.saveAll(entities);
        assertNotNull(saveEntities);
        assertFalse(saveEntities.isEmpty());
        assertEquals(entities, saveEntities);
        verify(repo).saveAll(entities);
    }

    @Test
    void findAll() {
        List<Permission> entities = PermissionProvider.listEntities();
        when(repo.findAll()).thenReturn(entities);
        List<Permission> entitiesFound = dao.findAll();
        assertNotNull(entitiesFound);
        assertFalse(entitiesFound.isEmpty());
        assertEquals(entities, entitiesFound);
        verify(repo).findAll();
    }

    @Test
    void findAllPage() {
        List<Permission> entities = PermissionProvider.listEntities();
        when(repo.findAll(Pageable.unpaged()))
                .thenReturn(new PageImpl<>(entities));
        Page<Permission> entitiesPage = dao.findAllPage(Pageable.unpaged());
        assertNotNull(entitiesPage);
        assertFalse(entitiesPage.isEmpty());
        assertTrue(entitiesPage.getPageable().isUnpaged());
        assertEquals(entitiesPage.getContent(), entities);
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
        List<Permission> entities =  PermissionProvider.listEntities();
        List<UUID> ids = entities.stream().map(Permission::getId).toList();
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
        Permission initialEntity = PermissionProvider.alternativeEntity();
        when(repo.findById(initialId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> dao.updateById(initialEntity, initialId));
        verify(repo).findById(initialId);

        UUID idSecond = PermissionProvider.alternativeEntity().getId();
        Permission entitySecond = PermissionProvider.alternativeEntity();
        Permission newEntitySecond = new Permission();
        newEntitySecond.setPermissionName(null);
        when(repo.findById(idSecond)).thenReturn(Optional.of(entitySecond));
        when(repo.save(entitySecond)).thenReturn(entitySecond);
        Permission entityUpdatedSecond = dao.updateById(newEntitySecond, idSecond);
        assertNotNull(entityUpdatedSecond);
        assertEquals(entitySecond.getId(), entityUpdatedSecond.getId());
        assertNotEquals(newEntitySecond.getPermissionName(), entityUpdatedSecond.getPermissionName());
        assertNotEquals(newEntitySecond.getId(), entityUpdatedSecond.getId());
        verify(repo, times(2)).findById(idSecond);
        verify(repo).save(entitySecond);

        UUID idThird = PermissionProvider.alternativeEntity().getId();
        Permission entityThird = PermissionProvider.alternativeEntity();
        Permission newEntityThird = new Permission();
        newEntityThird.setPermissionName("  ");
        when(repo.findById(idThird)).thenReturn(Optional.of(entityThird));
        when(repo.save(entityThird)).thenReturn(entityThird);
        Permission entityUpdatedThird = dao.updateById(newEntityThird, idThird);
        assertNotNull(entityUpdatedThird);
        assertEquals(entityThird.getId(), entityUpdatedThird.getId());
        assertNotEquals(newEntityThird.getPermissionName(), entityUpdatedThird.getPermissionName());
        assertNotEquals(newEntityThird.getId(), entityUpdatedThird.getId());
        verify(repo, times(3)).findById(idThird);
        verify(repo, times(2)).save(entityThird);

        UUID id = PermissionProvider.singleEntity().getId();
        Permission entity = PermissionProvider.singleEntity();
        Permission newEntity = PermissionProvider.alternativeEntity();
        when(repo.findById(id)).thenReturn(Optional.of(entity));
        when(repo.save(entity)).thenReturn(entity);
        Permission entityUpdated = dao.updateById(newEntity, id);
        assertNotNull(entityUpdated);
        assertEquals(entity.getId(), entityUpdated.getId());
        assertEquals(newEntity.getPermissionName(), entityUpdated.getPermissionName());
        assertNotEquals(newEntity.getId(), entityUpdated.getId());
        verify(repo).findById(id);
        verify(repo).save(entity);
    }

    @Test
    void existsByUniqueProperties() {
        Permission firstEntity = new Permission();
        firstEntity.setPermissionName(null);
        assertFalse(dao.existsByUniqueProperties(firstEntity));

        Permission secondEntity = new Permission();
        secondEntity.setPermissionName("  ");
        assertFalse(dao.existsByUniqueProperties(secondEntity));

        Permission entitySecond = PermissionProvider.alternativeEntity();
        when(repo.existsByPermissionName(entitySecond.getPermissionName())).thenReturn(false);
        assertFalse(dao.existsByUniqueProperties(entitySecond));
        verify(repo).existsByPermissionName(entitySecond.getPermissionName());

        Permission entity = PermissionProvider.singleEntity();
        when(repo.existsByPermissionName(entity.getPermissionName())).thenReturn(true);
        assertTrue(dao.existsByUniqueProperties(entity));
        verify(repo).existsByPermissionName(entity.getPermissionName());

    }

    @Test
    void findByPermissionName() {
        Permission entity = PermissionProvider.singleEntity();
        when(repo.findByPermissionName(entity.getPermissionName()))
                .thenReturn(Optional.of(entity));
        Permission entityFound = dao.findByPermissionName(entity.getPermissionName())
                .orElse(null);
        assertNotNull(entityFound);
        assertEquals(entity.getId(), entityFound.getId());
        assertEquals(entity.getPermissionName(), entityFound.getPermissionName());
        verify(repo).findByPermissionName(entity.getPermissionName());
    }
}