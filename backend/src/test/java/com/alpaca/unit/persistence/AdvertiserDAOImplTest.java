package com.alpaca.unit.persistence;

import com.alpaca.entity.Advertiser;
import com.alpaca.entity.User;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.impl.AdvertiserDAOImpl;
import com.alpaca.repository.AdvertiserRepo;
import com.alpaca.resources.AdvertiserProvider;
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
class AdvertiserDAOImplTest {

    @Mock
    private AdvertiserRepo repo;

    @InjectMocks
    private AdvertiserDAOImpl dao;

    @Test
    void updateById() {
        UUID initialId = AdvertiserProvider.alternativeEntity().getId();
        Advertiser initialEntity = AdvertiserProvider.alternativeEntity();
        when(repo.findById(initialId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> dao.updateById(initialEntity, initialId));
        verify(repo).findById(initialId);

        UUID idSecond = AdvertiserProvider.alternativeEntity().getId();
        Advertiser entitySecond = AdvertiserProvider.alternativeEntity();
        Advertiser newEntitySecond = new Advertiser();
        newEntitySecond.setDescription(null);
        newEntitySecond.setTitle(null);
        newEntitySecond.setAvatarUrl(null);
        newEntitySecond.setBannerUrl(null);
        newEntitySecond.setPublicUrlLocation(null);
        newEntitySecond.setPublicLocation(null);
        newEntitySecond.setIndexed(false);
        newEntitySecond.setUser(null);
        when(repo.findById(idSecond)).thenReturn(Optional.of(entitySecond));
        when(repo.save(entitySecond)).thenReturn(entitySecond);
        Advertiser entityUpdatedSecond = dao.updateById(newEntitySecond, idSecond);
        assertNotNull(entityUpdatedSecond);
        assertEquals(entitySecond.getId(), entityUpdatedSecond.getId());
        assertNotEquals(newEntitySecond.getDescription(), entityUpdatedSecond.getDescription());
        assertNotEquals(newEntitySecond.getId(), entityUpdatedSecond.getId());
        verify(repo, times(2)).findById(idSecond);
        verify(repo).save(entitySecond);

        UUID idThird = AdvertiserProvider.alternativeEntity().getId();
        Advertiser entityThird = AdvertiserProvider.alternativeEntity();
        Advertiser newEntityThird = new Advertiser();
        newEntityThird.setDescription(" ");
        newEntityThird.setTitle(" ");
        newEntityThird.setAvatarUrl(" ");
        newEntityThird.setBannerUrl(" ");
        newEntityThird.setPublicUrlLocation(" ");
        newEntityThird.setPublicLocation(" ");
        User newUser = new User();
        newUser.setId(null);
        newEntityThird.setUser(newUser);
        when(repo.findById(idThird)).thenReturn(Optional.of(entityThird));
        when(repo.save(entityThird)).thenReturn(entityThird);
        Advertiser entityUpdatedThird = dao.updateById(newEntityThird, idThird);
        assertNotNull(entityUpdatedThird);
        assertEquals(entityThird.getId(), entityUpdatedThird.getId());
        assertNotEquals(newEntityThird.getDescription(), entityUpdatedThird.getDescription());
        assertNotEquals(newEntityThird.getId(), entityUpdatedThird.getId());
        verify(repo, times(3)).findById(idThird);
        verify(repo).save(entityThird);

        UUID id = AdvertiserProvider.singleEntity().getId();
        Advertiser entity = AdvertiserProvider.singleEntity();
        Advertiser newEntity = AdvertiserProvider.alternativeEntity();
        when(repo.findById(id)).thenReturn(Optional.of(entity));
        when(repo.save(entity)).thenReturn(entity);
        Advertiser entityUpdated = dao.updateById(newEntity, id);
        assertNotNull(entityUpdated);
        assertEquals(entity.getId(), entityUpdated.getId());
        assertEquals(newEntity.getTitle(), entityUpdated.getTitle());
        assertNotEquals(newEntity.getId(), entityUpdated.getId());
        verify(repo).findById(id);
        verify(repo).save(entity);
    }

    @Test
    void existsByUniqueProperties() {
        Advertiser firstEntity = new Advertiser();
        firstEntity.setUser(null);
        assertFalse(dao.existsByUniqueProperties(firstEntity));

        Advertiser secondEntity = new Advertiser();
        User secondUser = new User();
        secondUser.setId(null);
        secondEntity.setUser(secondUser);
        assertFalse(dao.existsByUniqueProperties(secondEntity));

        Advertiser entitySecond = AdvertiserProvider.alternativeEntity();
        when(repo.countByUserId(entitySecond.getUser().getId())).thenReturn(0L);
        assertFalse(dao.existsByUniqueProperties(entitySecond));
        verify(repo).countByUserId(entitySecond.getUser().getId());

        Advertiser entity = AdvertiserProvider.singleEntity();
        when(repo.countByUserId(entity.getUser().getId())).thenReturn(1L);
        assertTrue(dao.existsByUniqueProperties(entity));
        verify(repo).countByUserId(entity.getUser().getId());
    }
}