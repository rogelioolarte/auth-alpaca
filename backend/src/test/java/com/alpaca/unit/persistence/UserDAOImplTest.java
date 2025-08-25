package com.alpaca.unit.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alpaca.entity.Advertiser;
import com.alpaca.entity.Profile;
import com.alpaca.entity.Role;
import com.alpaca.entity.User;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.impl.UserDAOImpl;
import com.alpaca.repository.UserRepo;
import com.alpaca.resources.AdvertiserProvider;
import com.alpaca.resources.ProfileProvider;
import com.alpaca.resources.RoleProvider;
import com.alpaca.resources.UserProvider;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link UserDAOImpl} */
@ExtendWith(MockitoExtension.class)
class UserDAOImplTest {

    @Mock private UserRepo repo;

    @InjectMocks private UserDAOImpl dao;

    private User firstEntity;
    private User secondEntity;
    private User thirdEntity;
    private final UUID id = UserProvider.singleEntity().getId();
    private final UUID idSecond = UserProvider.alternativeEntity().getId();
    private final UUID idThird = UserProvider.alternativeEntity().getId();

    @BeforeEach
    void setup() {
        firstEntity = UserProvider.singleEntity();
        secondEntity = UserProvider.alternativeEntity();
        thirdEntity = UserProvider.alternativeEntity();
    }

    // --- findByEmail ---
    @Test
    void findByEmailCaseOne() {
        User firstEntityWithNullEmail = new User();
        firstEntityWithNullEmail.setEmail(null);
        assertEquals(dao.findByEmail(firstEntityWithNullEmail.getEmail()), Optional.empty());
    }

    @Test
    void findByEmailCaseTwo() {
        User secondEntityWithEmptyEmail = new User();
        secondEntityWithEmptyEmail.setEmail("  ");
        assertEquals(dao.findByEmail(secondEntityWithEmptyEmail.getEmail()), Optional.empty());
    }

    @Test
    void findByEmailCaseThree() {
        when(repo.findByEmail(secondEntity.getEmail())).thenReturn(Optional.empty());
        User entityFoundSecond = dao.findByEmail(secondEntity.getEmail()).orElseGet(User::new);
        assertNotEquals(entityFoundSecond, secondEntity);
        verify(repo).findByEmail(secondEntity.getEmail());
    }

    @Test
    void findByEmailCaseFour() {
        when(repo.findByEmail(firstEntity.getEmail())).thenReturn(Optional.of(firstEntity));
        User entityFound = dao.findByEmail(firstEntity.getEmail()).orElseGet(User::new);
        assertEquals(entityFound, firstEntity);
        verify(repo).findByEmail(firstEntity.getEmail());
    }

    // --- updateById ---
    @Test
    void updateByIdCaseOne() {
        when(repo.findById(idSecond)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> dao.updateById(secondEntity, idSecond));
        verify(repo).findById(idSecond);
    }

    @Test
    void updateByIdCaseTwo() {
        User newEntitySecond = new User();
        newEntitySecond.setEmail(null);
        newEntitySecond.setPassword(null);
        newEntitySecond.setUserRoles(null);
        newEntitySecond.setProfile(null);
        newEntitySecond.setAdvertiser(null);
        newEntitySecond.setEnabled(false);
        newEntitySecond.setAccountNoLocked(false);
        newEntitySecond.setCredentialNoExpired(false);
        newEntitySecond.setAccountNoExpired(false);
        when(repo.findById(idSecond)).thenReturn(Optional.of(secondEntity));
        when(repo.save(secondEntity)).thenReturn(secondEntity);
        User userUpdatedSecond = dao.updateById(newEntitySecond, idSecond);
        assertNotNull(userUpdatedSecond);
        assertEquals(secondEntity.getId(), userUpdatedSecond.getId());
        assertNotEquals(newEntitySecond.getEmail(), userUpdatedSecond.getEmail());
        verify(repo).findById(idSecond);
        verify(repo).save(secondEntity);
    }

    @Test
    void updateByIdCaseThree() {
        User newEntityThird = new User();
        newEntityThird.setEmail(" ");
        newEntityThird.setPassword(" ");
        newEntityThird.setUserRoles(Collections.emptySet());
        newEntityThird.setEmailVerified(true);
        newEntityThird.setGoogleConnected(true);
        Profile thirdProfile = new Profile();
        thirdProfile.setId(null);
        newEntityThird.setProfile(thirdProfile);
        Advertiser thirdAdvertiser = new Advertiser();
        thirdAdvertiser.setId(null);
        newEntityThird.setAdvertiser(thirdAdvertiser);
        when(repo.findById(idThird)).thenReturn(Optional.of(thirdEntity));
        when(repo.save(thirdEntity)).thenReturn(thirdEntity);
        User userUpdatedThird = dao.updateById(newEntityThird, idThird);
        assertNotNull(userUpdatedThird);
        assertEquals(thirdEntity.getId(), userUpdatedThird.getId());
        assertNotEquals(newEntityThird.getEmail(), userUpdatedThird.getEmail());
        verify(repo).findById(idThird);
        verify(repo).save(thirdEntity);
    }

    @Test
    void updateByIdCaseFour() {
        User newEntity = UserProvider.alternativeEntity();
        Profile profile = ProfileProvider.singleEntity();
        Advertiser advertiser = AdvertiserProvider.singleEntity();
        newEntity.setProfile(profile);
        newEntity.setAdvertiser(advertiser);
        newEntity.setEmailVerified(true);
        newEntity.setGoogleConnected(true);
        Role newRole = RoleProvider.alternativeEntity();
        newEntity.setUserRoles(new HashSet<>(Set.of(newRole)));
        when(repo.findById(id)).thenReturn(Optional.of(firstEntity));
        when(repo.save(firstEntity)).thenReturn(firstEntity);
        User userUpdated = dao.updateById(newEntity, id);
        assertNotNull(userUpdated);
        assertEquals(firstEntity.getId(), userUpdated.getId());
        assertEquals(newEntity.getEmail(), userUpdated.getEmail());
        assertEquals(newEntity.getProfile(), userUpdated.getProfile());
        assertEquals(newEntity.getAdvertiser(), userUpdated.getAdvertiser());
        assertNotEquals(newEntity.getId(), userUpdated.getId());
        verify(repo).findById(id);
        verify(repo).save(firstEntity);
    }

    // --- existsByUniqueProperties ---
    @Test
    void existsByUniquePropertiesCaseOne() {
        User firstEntityWithNullEmail = new User();
        firstEntityWithNullEmail.setEmail(null);
        assertFalse(dao.existsByUniqueProperties(firstEntityWithNullEmail));
    }

    @Test
    void existsByUniquePropertiesCaseTwo() {
        User secondEntityWithEmptyEmail = new User();
        secondEntityWithEmptyEmail.setEmail("  ");
        assertFalse(dao.existsByUniqueProperties(secondEntityWithEmptyEmail));
    }

    @Test
    void existsByUniquePropertiesCaseThree() {
        when(repo.existsByEmail(secondEntity.getEmail())).thenReturn(false);
        assertFalse(dao.existsByUniqueProperties(secondEntity));
        verify(repo).existsByEmail(secondEntity.getEmail());
    }

    @Test
    void existsByUniquePropertiesCaseFour() {
        when(repo.existsByEmail(firstEntity.getEmail())).thenReturn(true);
        assertTrue(dao.existsByUniqueProperties(firstEntity));
        verify(repo).existsByEmail(firstEntity.getEmail());
    }

    // --- existsByEmail ---
    @Test
    void existsByEmailCaseOne() {
        assertFalse(dao.existsByEmail(null));
    }

    @Test
    void existsByEmailCaseTwo() {
        assertFalse(dao.existsByEmail("  "));
    }

    @Test
    void existsByEmailCaseThree() {
        when(repo.existsByEmail(secondEntity.getEmail())).thenReturn(false);
        assertFalse(dao.existsByEmail(secondEntity.getEmail()));
        verify(repo).existsByEmail(secondEntity.getEmail());
    }

    @Test
    void existsByEmailCaseFour() {
        when(repo.existsByEmail(firstEntity.getEmail())).thenReturn(true);
        assertTrue(dao.existsByEmail(firstEntity.getEmail()));
        verify(repo).existsByEmail(firstEntity.getEmail());
    }
}
