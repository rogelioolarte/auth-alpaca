package com.example.unit.persistence;

import com.example.entity.Advertiser;
import com.example.entity.User;
import com.example.exception.NotFoundException;
import com.example.persistence.impl.AdvertiserDAOImpl;
import com.example.repository.AdvertiserRepo;
import com.example.resources.AdvertiserProvider;
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
        Advertiser initialAdvertiser = AdvertiserProvider.alternativeEntity();
        when(repo.findById(initialId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> dao.updateById(initialAdvertiser, initialId));
        verify(repo).findById(initialId);

        UUID idSecond = AdvertiserProvider.alternativeEntity().getId();
        Advertiser advertiserSecond = AdvertiserProvider.alternativeEntity();
        Advertiser newAdvertiserSecond = new Advertiser();
        newAdvertiserSecond.setDescription(null);
        when(repo.findById(idSecond)).thenReturn(Optional.of(advertiserSecond));
        when(repo.save(advertiserSecond)).thenReturn(advertiserSecond);
        Advertiser advertiserUpdatedSecond = dao.updateById(newAdvertiserSecond, idSecond);
        assertNotNull(advertiserUpdatedSecond);
        assertEquals(advertiserSecond.getId(), advertiserUpdatedSecond.getId());
        assertNotEquals(newAdvertiserSecond.getDescription(), advertiserUpdatedSecond.getDescription());
        assertNotEquals(newAdvertiserSecond.getId(), advertiserUpdatedSecond.getId());
        verify(repo, times(2)).findById(idSecond);
        verify(repo).save(advertiserSecond);

        UUID idThird = AdvertiserProvider.alternativeEntity().getId();
        Advertiser advertiserThird = AdvertiserProvider.alternativeEntity();
        Advertiser newAdvertiserThird = new Advertiser();
        newAdvertiserThird.setDescription(" ");
        when(repo.findById(idThird)).thenReturn(Optional.of(advertiserThird));
        when(repo.save(advertiserThird)).thenReturn(advertiserThird);
        Advertiser permissionUpdatedThird = dao.updateById(newAdvertiserThird, idThird);
        assertNotNull(permissionUpdatedThird);
        assertEquals(advertiserThird.getId(), permissionUpdatedThird.getId());
        assertNotEquals(newAdvertiserThird.getDescription(), permissionUpdatedThird.getDescription());
        assertNotEquals(newAdvertiserThird.getId(), permissionUpdatedThird.getId());
        verify(repo, times(3)).findById(idThird);
        verify(repo, times(2)).save(advertiserThird);

        UUID id = AdvertiserProvider.singleEntity().getId();
        Advertiser advertiser = AdvertiserProvider.singleEntity();
        Advertiser newAdvertiser = AdvertiserProvider.alternativeEntity();
        when(repo.findById(id)).thenReturn(Optional.of(advertiser));
        when(repo.save(advertiser)).thenReturn(advertiser);
        Advertiser advertiserUpdated = dao.updateById(newAdvertiser, id);
        assertNotNull(advertiserUpdated);
        assertEquals(advertiser.getId(), advertiserUpdated.getId());
        assertEquals(newAdvertiser.getTitle(), advertiserUpdated.getTitle());
        assertNotEquals(newAdvertiser.getId(), advertiserUpdated.getId());
        verify(repo).findById(id);
        verify(repo).save(advertiser);
    }

    @Test
    void existsByUniqueProperties() {
        Advertiser firstAdvertiser = new Advertiser();
        firstAdvertiser.setUser(null);
        assertFalse(dao.existsByUniqueProperties(firstAdvertiser));

        Advertiser secondAdvertiser = new Advertiser();
        User secondUser = new User();
        secondUser.setId(null);
        secondAdvertiser.setUser(secondUser);
        assertFalse(dao.existsByUniqueProperties(secondAdvertiser));

        Advertiser advertiserSecond = AdvertiserProvider.alternativeEntity();
        when(repo.countByUserId(advertiserSecond.getUser().getId())).thenReturn(0L);
        assertFalse(dao.existsByUniqueProperties(advertiserSecond));
        verify(repo).countByUserId(advertiserSecond.getUser().getId());

        Advertiser advertiser = AdvertiserProvider.singleEntity();
        when(repo.countByUserId(advertiser.getUser().getId())).thenReturn(1L);
        assertTrue(dao.existsByUniqueProperties(advertiser));
        verify(repo).countByUserId(advertiser.getUser().getId());
    }
}