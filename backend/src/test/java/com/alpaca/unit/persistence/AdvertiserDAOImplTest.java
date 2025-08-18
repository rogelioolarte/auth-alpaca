package com.alpaca.unit.persistence;

import com.alpaca.entity.Advertiser;
import com.alpaca.entity.User;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.impl.AdvertiserDAOImpl;
import com.alpaca.repository.AdvertiserRepo;
import com.alpaca.resources.AdvertiserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit tests for {@link AdvertiserDAOImpl} */
@ExtendWith(MockitoExtension.class)
class AdvertiserDAOImplTest {

    @Mock private AdvertiserRepo repo;

    @InjectMocks private AdvertiserDAOImpl dao;

    private Advertiser firstEntity;
    private Advertiser secondEntity;
    private Advertiser thirdEntity;

    @BeforeEach
    void setup() {
        firstEntity = AdvertiserProvider.singleEntity();
        secondEntity = AdvertiserProvider.alternativeEntity();
        thirdEntity = AdvertiserProvider.alternativeEntity();
    }

    // --- updateById ---
    @Test
    void updateByIdCaseOne() {
        UUID initialId = firstEntity.getId();
        when(repo.findById(initialId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> dao.updateById(firstEntity, initialId));
        verify(repo).findById(initialId);
    }

    @Test
    void updateByIdCaseTwo() {
        UUID idSecond = secondEntity.getId();
        Advertiser newEntitySecond = new Advertiser();
        newEntitySecond.setDescription(null);
        newEntitySecond.setTitle(null);
        newEntitySecond.setAvatarUrl(null);
        newEntitySecond.setBannerUrl(null);
        newEntitySecond.setPublicUrlLocation(null);
        newEntitySecond.setPublicLocation(null);
        newEntitySecond.setIndexed(false);
        newEntitySecond.setUser(null);

        when(repo.findById(idSecond)).thenReturn(Optional.of(secondEntity));
        when(repo.save(secondEntity)).thenReturn(secondEntity);
        Advertiser entityUpdatedSecond = dao.updateById(newEntitySecond, idSecond);

        assertNotNull(entityUpdatedSecond);
        assertEquals(secondEntity.getId(), entityUpdatedSecond.getId());
        assertNotEquals(newEntitySecond.getDescription(), entityUpdatedSecond.getDescription());
        assertNotEquals(newEntitySecond.getId(), entityUpdatedSecond.getId());
        verify(repo).findById(idSecond);
        verify(repo).save(secondEntity);
    }

    @Test
    void updateByIdCaseThree() {
        UUID idThird = thirdEntity.getId();
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

        when(repo.findById(idThird)).thenReturn(Optional.of(thirdEntity));
        when(repo.save(thirdEntity)).thenReturn(thirdEntity);
        Advertiser entityUpdatedThird = dao.updateById(newEntityThird, idThird);

        assertNotNull(entityUpdatedThird);
        assertEquals(thirdEntity.getId(), entityUpdatedThird.getId());
        assertNotEquals(newEntityThird.getDescription(), entityUpdatedThird.getDescription());
        assertNotEquals(newEntityThird.getId(), entityUpdatedThird.getId());
        verify(repo).findById(idThird);
        verify(repo).save(thirdEntity);
    }

    @Test
    void updateByIdCaseFour() {
        UUID id = firstEntity.getId();
        Advertiser newEntity = AdvertiserProvider.alternativeEntity();

        when(repo.findById(id)).thenReturn(Optional.of(firstEntity));
        when(repo.save(firstEntity)).thenReturn(firstEntity);

        Advertiser entityUpdated = dao.updateById(newEntity, id);

        assertNotNull(entityUpdated);
        assertEquals(firstEntity.getId(), entityUpdated.getId());
        assertEquals(newEntity.getTitle(), entityUpdated.getTitle());
        assertNotEquals(newEntity.getId(), entityUpdated.getId());
        verify(repo).findById(id);
        verify(repo).save(firstEntity);
    }

    // --- existsByUniqueProperties ---
    @Test
    void existsByUniquePropertiesCaseOne() {
        Advertiser firstEntity = new Advertiser();
        firstEntity.setUser(null);
        assertFalse(dao.existsByUniqueProperties(firstEntity));
    }

    @Test
    void existsByUniquePropertiesCaseTwo() {
        Advertiser secondEntity = new Advertiser();
        User secondUser = new User();
        secondUser.setId(null);
        secondEntity.setUser(secondUser);
        assertFalse(dao.existsByUniqueProperties(secondEntity));
    }

    @Test
    void existsByUniquePropertiesCaseThree() {
        Advertiser entitySecond = AdvertiserProvider.alternativeEntity();
        when(repo.countByUserId(entitySecond.getUser().getId())).thenReturn(0L);
        assertFalse(dao.existsByUniqueProperties(entitySecond));
        verify(repo).countByUserId(entitySecond.getUser().getId());
    }

    @Test
    void existsByUniquePropertiesCaseFour() {
        Advertiser entity = AdvertiserProvider.singleEntity();
        when(repo.countByUserId(entity.getUser().getId())).thenReturn(1L);
        assertTrue(dao.existsByUniqueProperties(entity));
        verify(repo).countByUserId(entity.getUser().getId());
    }
}
