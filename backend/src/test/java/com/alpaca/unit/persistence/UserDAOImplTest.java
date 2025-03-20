package com.alpaca.unit.persistence;

import com.alpaca.entity.User;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.impl.UserDAOImpl;
import com.alpaca.repository.UserRepo;
import com.alpaca.resources.UserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDAOImplTest {

    @Mock
    private UserRepo repo;

    @InjectMocks
    private UserDAOImpl dao;

    @Test
    void findByEmail() {
        User firstEntity = new User();
        firstEntity.setEmail(null);
        assertEquals(dao.findByEmail(firstEntity.getEmail()), Optional.empty());

        User secondEntity = new User();
        secondEntity.setEmail("  ");
        assertEquals(dao.findByEmail(secondEntity.getEmail()), Optional.empty());

        User entitySecond = UserProvider.alternativeEntity();
        when(repo.findByEmail(entitySecond.getEmail())).thenReturn(Optional.empty());
        User entityFoundSecond = dao.findByEmail(entitySecond.getEmail())
                .orElseGet(User::new);
        assertNotEquals(entityFoundSecond, entitySecond);
        verify(repo).findByEmail(entitySecond.getEmail());

        User entity = UserProvider.singleEntity();
        when(repo.findByEmail(entity.getEmail())).thenReturn(Optional.of(entity));
        User entityFound = dao.findByEmail(entity.getEmail()).orElseGet(User::new);
        assertEquals(entityFound, entity);
        verify(repo).findByEmail(entity.getEmail());
    }

    @Test
    void updateById() {
        UUID initialId = UserProvider.alternativeEntity().getId();
        User initialEntity = UserProvider.alternativeEntity();
        when(repo.findById(initialId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> dao.updateById(initialEntity, initialId));
        verify(repo).findById(initialId);

        UUID idSecond = UserProvider.alternativeEntity().getId();
        User secondEntity = UserProvider.alternativeEntity();
        User newEntitySecond = new User();
        newEntitySecond.setEmail(null);
        newEntitySecond.setPassword(null);
        newEntitySecond.setUserRoles(null);
        when(repo.findById(idSecond)).thenReturn(Optional.of(secondEntity));
        when(repo.save(secondEntity)).thenReturn(secondEntity);
        User roleUpdatedSecond = dao.updateById(newEntitySecond, idSecond);
        assertNotNull(roleUpdatedSecond);
        assertEquals(secondEntity.getId(), roleUpdatedSecond.getId());
        assertNotEquals(newEntitySecond.getEmail(), roleUpdatedSecond.getEmail());
        assertNotEquals(newEntitySecond.getId(), roleUpdatedSecond.getId());
        verify(repo, times(2)).findById(idSecond);
        verify(repo).save(secondEntity);

        UUID idThird = UserProvider.alternativeEntity().getId();
        User thirdEntity = UserProvider.alternativeEntity();
        User newEntityThird = new User();
        newEntityThird.setEmail(" ");
        newEntityThird.setPassword(" ");
        newEntityThird.setUserRoles(Collections.emptySet());
        when(repo.findById(idThird)).thenReturn(Optional.of(thirdEntity));
        when(repo.save(thirdEntity)).thenReturn(thirdEntity);
        User roleUpdatedThird = dao.updateById(newEntityThird, idThird);
        assertNotNull(roleUpdatedThird);
        assertEquals(thirdEntity.getId(), roleUpdatedThird.getId());
        assertNotEquals(newEntityThird.getEmail(), roleUpdatedThird.getEmail());
        assertNotEquals(newEntityThird.getId(), roleUpdatedThird.getId());
        verify(repo, times(3)).findById(idThird);
        verify(repo).save(thirdEntity);

        UUID id = UserProvider.singleEntity().getId();
        User entity = UserProvider.singleEntity();
        User newEntity = UserProvider.alternativeEntity();
        when(repo.findById(id)).thenReturn(Optional.of(entity));
        when(repo.save(entity)).thenReturn(entity);
        User roleUpdated = dao.updateById(newEntity, id);
        assertNotNull(roleUpdated);
        assertEquals(entity.getId(), roleUpdated.getId());
        assertEquals(newEntity.getEmail(), roleUpdated.getEmail());
        assertNotEquals(newEntity.getId(), roleUpdated.getId());
        verify(repo).findById(id);
        verify(repo).save(entity);
    }

    @Test
    void existsByUniqueProperties() {
        User firstEntity = new User();
        firstEntity.setEmail(null);
        assertFalse(dao.existsByUniqueProperties(firstEntity));

        User secondEntity = new User();
        secondEntity.setEmail("  ");
        assertFalse(dao.existsByUniqueProperties(secondEntity));

        User alternativeEntity = UserProvider.alternativeEntity();
        when(repo.existsByEmail(alternativeEntity.getEmail())).thenReturn(false);
        assertFalse(dao.existsByUniqueProperties(alternativeEntity));
        verify(repo).existsByEmail(alternativeEntity.getEmail());

        User entity = UserProvider.singleEntity();
        when(repo.existsByEmail(entity.getEmail())).thenReturn(true);
        assertTrue(dao.existsByUniqueProperties(entity));
        verify(repo).existsByEmail(entity.getEmail());
    }

    @Test
    void existsByEmail() {
        User firstEntity = new User();
        firstEntity.setEmail(null);
        assertFalse(dao.existsByUniqueProperties(firstEntity));

        User secondEntity = new User();
        secondEntity.setEmail("  ");
        assertFalse(dao.existsByUniqueProperties(secondEntity));

        User alternativeEntity = UserProvider.alternativeEntity();
        when(repo.existsByEmail(alternativeEntity.getEmail())).thenReturn(false);
        assertFalse(dao.existsByUniqueProperties(alternativeEntity));
        verify(repo).existsByEmail(alternativeEntity.getEmail());

        User entity = UserProvider.singleEntity();
        when(repo.existsByEmail(entity.getEmail())).thenReturn(true);
        assertTrue(dao.existsByUniqueProperties(entity));
        verify(repo).existsByEmail(entity.getEmail());
    }
}