package com.alpaca.integration.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.alpaca.entity.Advertiser;
import com.alpaca.entity.Profile;
import com.alpaca.entity.User;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IUserDAO;
import com.alpaca.persistence.impl.UserDAOImpl;
import com.alpaca.repository.AdvertiserRepo;
import com.alpaca.repository.ProfileRepo;
import com.alpaca.repository.UserRepo;
import com.alpaca.resources.AdvertiserProvider;
import com.alpaca.resources.ProfileProvider;
import com.alpaca.resources.UserProvider;
import com.alpaca.security.manager.PasswordManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link UserDAOImpl} */
@DataJpaTest
@Import({UserDAOImpl.class})
@DisplayName("UserDAOImpl Integration Tests")
class UserDAOImplIT {

    @Autowired private IUserDAO dao;
    @Autowired private UserRepo userRepo;
    @Autowired private ProfileRepo profileRepo;
    @Autowired private AdvertiserRepo advertiserRepo;
    @MockitoBean private PasswordManager passwordManager;

    private User user;

    @BeforeEach
    void setup() {
        Instant now = Instant.now();
        user = UserProvider.singleTemplate();
        user.setCreatedAt(now);
    }

    @Test
    @DisplayName("findByEmail: returns user when email matches and handles invalid inputs")
    @Transactional
    void findByEmail_ShouldReturnCorrectResults() {
        userRepo.save(user);

        assertTrue(dao.findByEmail(null).isEmpty());
        assertTrue(dao.findByEmail("").isEmpty());

        Optional<User> found = dao.findByEmail(user.getEmail());
        assertTrue(found.isPresent());
        assertEquals(user.getEmail(), found.get().getEmail());
    }

    @Test
    @DisplayName("updateById: updates all fields and relations when different")
    @Transactional
    void updateById_ShouldUpdateAllFields() {
        User user = UserProvider.singleTemplate();
        userRepo.save(user);

        Profile profile = ProfileProvider.singleTemplate();
        profile.setUser(user);
        profileRepo.save(profile);

        Advertiser advertiser = AdvertiserProvider.singleTemplate();
        advertiser.setUser(user);
        advertiserRepo.save(advertiser);

        String newPassword = "newEncodedPassword";
        when(passwordManager.matches(anyString(), anyString())).thenReturn(false);
        when(passwordManager.encodePassword(anyString())).thenReturn(newPassword);

        User updateData = UserProvider.singleTemplate();
        updateData.setEmail("updated@alpaca.com");
        updateData.setPassword("newRawPassword");
        updateData.setProfile(profile);
        updateData.setAdvertiser(advertiser);
        updateData.setEnabled(!user.isEnabled());

        User result = dao.updateById(updateData, user.getId());

        assertAll(
                () -> assertEquals(updateData.getEmail(), result.getEmail()),
                () -> assertEquals(newPassword, result.getPassword()),
                () -> assertEquals(profile.getId(), result.getProfile().getId()),
                () -> assertEquals(advertiser.getId(), result.getAdvertiser().getId()),
                () -> assertEquals(updateData.isEnabled(), result.isEnabled()));
    }

    @Test
    @DisplayName("updateById: ignores null or identical values during update")
    @Transactional
    void updateById_ShouldIgnoreIdenticalOrNullValues() {
        userRepo.save(user);
        String originalPassword = user.getPassword();

        when(passwordManager.matches(anyString(), anyString())).thenReturn(true);

        User partialUpdate = new User();
        partialUpdate.setPassword("samePassword"); // matches true, should not re-encode

        User result = dao.updateById(partialUpdate, user.getId());

        assertEquals(originalPassword, result.getPassword());
    }

    @Test
    @DisplayName("updateById: throws NotFoundException when ID does not exist")
    @Transactional
    void updateById_ShouldThrowNotFoundException() {
        UUID randomId = UUID.randomUUID();

        assertThrows(NotFoundException.class, () -> dao.updateById(user, randomId));
    }

    @Test
    @DisplayName("existsByEmail: returns true only for existing records")
    @Transactional
    void existsByEmail_ShouldValidateCorrectly() {
        userRepo.save(user);

        assertTrue(dao.existsByEmail(user.getEmail()));
        assertFalse(dao.existsByEmail("non-existent@alpaca.com"));
        assertFalse(dao.existsByEmail(null));
    }

    @Test
    @DisplayName("existsByUniqueProperties: uses email to check existence")
    @Transactional
    void existsByUniqueProperties_ShouldCheckEmail() {
        userRepo.save(user);

        User probe = new User();
        probe.setEmail(user.getEmail());

        assertTrue(dao.existsByUniqueProperties(probe));
    }

    @Test
    @DisplayName("lockFindUserById: retrieves user with pessimistic lock")
    @Transactional
    void lockFindUserById_ShouldRetrieveUser() {
        userRepo.save(user);

        Optional<User> lockedUser = dao.lockFindUserById(user.getId());

        assertTrue(lockedUser.isPresent());
        assertEquals(user.getId(), lockedUser.get().getId());
    }

    @Test
    @DisplayName("existsAllByIds: verifies multiple IDs correctly")
    @Transactional
    void existsAllByIds_ShouldVerifyCount() {
        Instant now = Instant.now();
        User user1 = UserProvider.singleTemplate();
        user1.setEmail("u1@test.com");
        user1.setCreatedAt(now);
        User user2 = UserProvider.singleTemplate();
        user2.setEmail("u2@test.com");
        user2.setCreatedAt(now);

        userRepo.save(user1);
        userRepo.save(user2);

        List<UUID> ids = List.of(user1.getId(), user2.getId());
        assertTrue(dao.existsAllByIds(ids));

        List<UUID> invalidIds = List.of(user1.getId(), UUID.randomUUID());
        assertFalse(dao.existsAllByIds(invalidIds));
    }
}
