package com.alpaca.unit.persistence;

import com.alpaca.entity.Role;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.impl.RoleDAOImpl;
import com.alpaca.repository.RoleRepo;
import com.alpaca.resources.RoleProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleDAOImplTest {

    @Mock
    private RoleRepo repo;

    @InjectMocks
    private RoleDAOImpl dao;
    
    @Test
    void findByRoleName() {
        Role firstEntity = new Role();
        firstEntity.setRoleName(null);
        assertEquals(dao.findByRoleName(firstEntity.getRoleName()), Optional.empty());

        Role secondEntity = new Role();
        secondEntity.setRoleName("  ");
        assertEquals(dao.findByRoleName(secondEntity.getRoleName()), Optional.empty());

        Role entitySecond = RoleProvider.alternativeEntity();
        when(repo.findByRoleName(entitySecond.getRoleName())).thenReturn(Optional.of(entitySecond));
        Role entityFoundSecond = dao.findByRoleName(entitySecond.getRoleName())
                .orElseGet(Role::new);
        assertEquals(entityFoundSecond, entitySecond);
        verify(repo).findByRoleName(entitySecond.getRoleName());

        Role entity = RoleProvider.singleEntity();
        when(repo.findByRoleName(entity.getRoleName())).thenReturn(Optional.of(entity));
        Role entityFound = dao.findByRoleName(entity.getRoleName())
                .orElseGet(Role::new);
        assertEquals(entityFound, entity);
        verify(repo).findByRoleName(entity.getRoleName());
    }

    @Test
    void updateById() {
        UUID initialId = RoleProvider.alternativeEntity().getId();
        Role initialEntity = RoleProvider.alternativeEntity();
        when(repo.findById(initialId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> dao.updateById(initialEntity, initialId));
        verify(repo).findById(initialId);

        UUID idSecond = RoleProvider.alternativeEntity().getId();
        Role entitySecond = RoleProvider.alternativeEntity();
        Role newEntitySecond = new Role();
        newEntitySecond.setRoleName(null);
        when(repo.findById(idSecond)).thenReturn(Optional.of(entitySecond));
        when(repo.save(entitySecond)).thenReturn(entitySecond);
        Role entityUpdatedSecond = dao.updateById(newEntitySecond, idSecond);
        assertNotNull(entityUpdatedSecond);
        assertEquals(entitySecond.getId(), entityUpdatedSecond.getId());
        assertNotEquals(newEntitySecond.getRoleName(), entityUpdatedSecond.getRoleName());
        assertNotEquals(newEntitySecond.getId(), entityUpdatedSecond.getId());
        verify(repo, times(2)).findById(idSecond);
        verify(repo).save(entitySecond);

        UUID idThird = RoleProvider.alternativeEntity().getId();
        Role entityThird = RoleProvider.alternativeEntity();
        Role newEntityThird = new Role();
        newEntityThird.setRoleName(" ");
        when(repo.findById(idThird)).thenReturn(Optional.of(entityThird));
        when(repo.save(entityThird)).thenReturn(entityThird);
        Role entityUpdatedThird = dao.updateById(newEntityThird, idThird);
        assertNotNull(entityUpdatedThird);
        assertEquals(entityThird.getId(), entityUpdatedThird.getId());
        assertNotEquals(newEntityThird.getRoleName(), entityUpdatedThird.getRoleName());
        assertNotEquals(newEntityThird.getId(), entityUpdatedThird.getId());
        verify(repo, times(3)).findById(idThird);
        verify(repo).save(entityThird);

        UUID id = RoleProvider.singleEntity().getId();
        Role entity = RoleProvider.singleEntity();
        Role newEntity = RoleProvider.alternativeEntity();
        when(repo.findById(id)).thenReturn(Optional.of(entity));
        when(repo.save(entity)).thenReturn(entity);
        Role entityUpdated = dao.updateById(newEntity, id);
        assertNotNull(entityUpdated);
        assertEquals(entity.getId(), entityUpdated.getId());
        assertEquals(newEntity.getRoleName(), entityUpdated.getRoleName());
        assertNotEquals(newEntity.getId(), entityUpdated.getId());
        verify(repo).findById(id);
        verify(repo).save(entity);
    }

    @Test
    void existsByUniqueProperties() {
        Role firstEntity = new Role();
        firstEntity.setRoleName(null);
        assertFalse(dao.existsByUniqueProperties(firstEntity));

        Role secondEntity = new Role();
        secondEntity.setRoleName("  ");
        assertFalse(dao.existsByUniqueProperties(secondEntity));

        Role thirdEntity = new Role();
        thirdEntity.setRoleDescription(null);
        assertFalse(dao.existsByUniqueProperties(thirdEntity));

        Role fourthEntity = new Role();
        fourthEntity.setRoleDescription("  ");
        assertFalse(dao.existsByUniqueProperties(fourthEntity));

        Role entitySecond = RoleProvider.alternativeEntity();
        when(repo.existsByRoleName(entitySecond.getRoleName())).thenReturn(false);
        assertFalse(dao.existsByUniqueProperties(entitySecond));
        verify(repo).existsByRoleName(entitySecond.getRoleName());

        Role entity = RoleProvider.singleEntity();
        when(repo.existsByRoleName(entity.getRoleName())).thenReturn(true);
        assertTrue(dao.existsByUniqueProperties(entity));
        verify(repo).existsByRoleName(entity.getRoleName());
    }
}