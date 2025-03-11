package com.alpaca.unit.persistence;

import com.alpaca.entity.Profile;
import com.alpaca.entity.User;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.impl.ProfileDAOImpl;
import com.alpaca.repository.ProfileRepo;
import com.alpaca.resources.ProfileProvider;
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
class ProfileDAOImplTest {

    @Mock
    private ProfileRepo repo;

    @InjectMocks
    private ProfileDAOImpl dao;

    @Test
    void updateById() {
        UUID initialId = ProfileProvider.alternativeEntity().getId();
        Profile initialEntity = ProfileProvider.alternativeEntity();
        when(repo.findById(initialId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> dao.updateById(initialEntity, initialId));
        verify(repo).findById(initialId);

        UUID idSecond = ProfileProvider.alternativeEntity().getId();
        Profile entitySecond = ProfileProvider.alternativeEntity();
        Profile newEntitySecond = new Profile();
        newEntitySecond.setAddress(null);
        when(repo.findById(idSecond)).thenReturn(Optional.of(entitySecond));
        when(repo.save(entitySecond)).thenReturn(entitySecond);
        Profile entityUpdatedSecond = dao.updateById(newEntitySecond, idSecond);
        assertNotNull(entityUpdatedSecond);
        assertEquals(entitySecond.getId(), entityUpdatedSecond.getId());
        assertNotEquals(newEntitySecond.getAddress(), entityUpdatedSecond.getAddress());
        assertNotEquals(newEntitySecond.getId(), entityUpdatedSecond.getId());
        verify(repo, times(2)).findById(idSecond);
        verify(repo).save(entitySecond);

        UUID idThird = ProfileProvider.alternativeEntity().getId();
        Profile entityThird = ProfileProvider.alternativeEntity();
        Profile newEntityThird = new Profile();
        newEntityThird.setAddress(" ");
        when(repo.findById(idThird)).thenReturn(Optional.of(entityThird));
        when(repo.save(entityThird)).thenReturn(entityThird);
        Profile entityUpdatedThird = dao.updateById(newEntityThird, idThird);
        assertNotNull(entityUpdatedThird);
        assertEquals(entityThird.getId(), entityUpdatedThird.getId());
        assertNotEquals(newEntityThird.getAddress(), entityUpdatedThird.getAddress());
        assertNotEquals(newEntityThird.getId(), entityUpdatedThird.getId());
        verify(repo, times(3)).findById(idThird);
        verify(repo).save(entityThird);

        UUID id = ProfileProvider.singleEntity().getId();
        Profile entity = ProfileProvider.singleEntity();
        Profile newEntity = ProfileProvider.alternativeEntity();
        when(repo.findById(id)).thenReturn(Optional.of(entity));
        when(repo.save(entity)).thenReturn(entity);
        Profile entityUpdated = dao.updateById(newEntity, id);
        assertNotNull(entityUpdated);
        assertEquals(entity.getId(), entityUpdated.getId());
        assertEquals(newEntity.getAddress(), entityUpdated.getAddress());
        assertNotEquals(newEntity.getId(), entityUpdated.getId());
        verify(repo).findById(id);
        verify(repo).save(entity);
    }

    @Test
    void existsByUniqueProperties() {
        Profile firstEntity = new Profile();
        firstEntity.setUser(null);
        assertFalse(dao.existsByUniqueProperties(firstEntity));

        Profile secondEntity = new Profile();
        User secondUser = new User();
        secondUser.setId(null);
        secondEntity.setUser(secondUser);
        assertFalse(dao.existsByUniqueProperties(secondEntity));

        Profile entitySecond = ProfileProvider.alternativeEntity();
        when(repo.countByUserId(entitySecond.getUser().getId())).thenReturn(0L);
        assertFalse(dao.existsByUniqueProperties(entitySecond));
        verify(repo).countByUserId(entitySecond.getUser().getId());

        Profile entity = ProfileProvider.singleEntity();
        when(repo.countByUserId(entity.getUser().getId())).thenReturn(1L);
        assertTrue(dao.existsByUniqueProperties(entity));
        verify(repo).countByUserId(entity.getUser().getId());
    }
}