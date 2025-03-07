package com.example.unit.persistence;

import com.example.entity.Profile;
import com.example.entity.User;
import com.example.exception.NotFoundException;
import com.example.persistence.impl.ProfileDAOImpl;
import com.example.repository.ProfileRepo;
import com.example.resources.ProfileProvider;
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
        Profile initialProfile = ProfileProvider.alternativeEntity();
        when(repo.findById(initialId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> dao.updateById(initialProfile, initialId));
        verify(repo).findById(initialId);

        UUID idSecond = ProfileProvider.alternativeEntity().getId();
        Profile profileSecond = ProfileProvider.alternativeEntity();
        Profile newProfileSecond = new Profile();
        newProfileSecond.setAddress(null);
        when(repo.findById(idSecond)).thenReturn(Optional.of(profileSecond));
        when(repo.save(profileSecond)).thenReturn(profileSecond);
        Profile profileUpdatedSecond = dao.updateById(newProfileSecond, idSecond);
        assertNotNull(profileUpdatedSecond);
        assertEquals(profileSecond.getId(), profileUpdatedSecond.getId());
        assertNotEquals(newProfileSecond.getAddress(), profileUpdatedSecond.getAddress());
        assertNotEquals(newProfileSecond.getId(), profileUpdatedSecond.getId());
        verify(repo, times(2)).findById(idSecond);
        verify(repo).save(profileSecond);

        UUID idThird = ProfileProvider.alternativeEntity().getId();
        Profile profileThird = ProfileProvider.alternativeEntity();
        Profile newProfileThird = new Profile();
        newProfileThird.setAddress(" ");
        when(repo.findById(idThird)).thenReturn(Optional.of(profileThird));
        when(repo.save(profileThird)).thenReturn(profileThird);
        Profile profileUpdatedThird = dao.updateById(newProfileThird, idThird);
        assertNotNull(profileUpdatedThird);
        assertEquals(profileThird.getId(), profileUpdatedThird.getId());
        assertNotEquals(newProfileThird.getAddress(), profileUpdatedThird.getAddress());
        assertNotEquals(newProfileThird.getId(), profileUpdatedThird.getId());
        verify(repo, times(3)).findById(idThird);
        verify(repo).save(profileThird);

        UUID id = ProfileProvider.singleEntity().getId();
        Profile profile = ProfileProvider.singleEntity();
        Profile newProfile = ProfileProvider.alternativeEntity();
        when(repo.findById(id)).thenReturn(Optional.of(profile));
        when(repo.save(profile)).thenReturn(profile);
        Profile profileUpdated = dao.updateById(newProfile, id);
        assertNotNull(profileUpdated);
        assertEquals(profile.getId(), profileUpdated.getId());
        assertEquals(newProfile.getAddress(), profileUpdated.getAddress());
        assertNotEquals(newProfile.getId(), profileUpdated.getId());
        verify(repo).findById(id);
        verify(repo).save(profile);
    }

    @Test
    void existsByUniqueProperties() {
        Profile firstProfile = new Profile();
        firstProfile.setUser(null);
        assertFalse(dao.existsByUniqueProperties(firstProfile));

        Profile secondProfile = new Profile();
        User secondUser = new User();
        secondUser.setId(null);
        secondProfile.setUser(secondUser);
        assertFalse(dao.existsByUniqueProperties(secondProfile));

        Profile profileSecond = ProfileProvider.alternativeEntity();
        when(repo.countByUserId(profileSecond.getUser().getId())).thenReturn(0L);
        assertFalse(dao.existsByUniqueProperties(profileSecond));
        verify(repo).countByUserId(profileSecond.getUser().getId());

        Profile profile = ProfileProvider.singleEntity();
        when(repo.countByUserId(profile.getUser().getId())).thenReturn(1L);
        assertTrue(dao.existsByUniqueProperties(profile));
        verify(repo).countByUserId(profile.getUser().getId());
    }
}