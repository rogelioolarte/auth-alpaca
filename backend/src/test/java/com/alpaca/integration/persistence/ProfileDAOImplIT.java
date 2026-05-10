package com.alpaca.integration.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.entity.Profile;
import com.alpaca.entity.User;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IProfileDAO;
import com.alpaca.persistence.impl.ProfileDAOImpl;
import com.alpaca.repository.ProfileRepo;
import com.alpaca.repository.UserRepo;
import com.alpaca.resources.ProfileProvider;
import com.alpaca.resources.UserProvider;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link ProfileDAOImpl} */
@DataJpaTest
@Import({ProfileDAOImpl.class})
@DisplayName("ProfileDAOImpl Integration Tests")
class ProfileDAOImplIT {

    @Autowired private IProfileDAO dao;
    @Autowired private ProfileRepo repo;
    @Autowired private UserRepo userRepo;

    private Instant now;

    @BeforeEach
    void setup() {
        now = Instant.now();
    }

    @Test
    @DisplayName("updateById: performs full update on all scalar fields and associations")
    @Transactional
    void updateById_ShouldPerformFullUpdate() {
        // Arrange
        User initialOwner = UserProvider.singleTemplate();
        initialOwner.setCreatedAt(now);
        User persistedInitialOwner = userRepo.save(initialOwner);

        Profile initial = ProfileProvider.singleTemplate();
        initial.setUser(persistedInitialOwner);
        initial.setCreatedAt(now);
        Profile persistedProfile = repo.save(initial);

        User newOwner = UserProvider.alternativeTemplate();
        newOwner.setCreatedAt(now);
        User persistedNewOwner = userRepo.save(newOwner);

        Profile updateData = new Profile();
        updateData.setFirstName("Jane");
        updateData.setLastName("Smith");
        updateData.setAddress("456 Elm St");
        updateData.setAvatarUrl("http://avatar.com/jane");
        updateData.setUser(persistedNewOwner);

        // Act
        Profile result = dao.updateById(updateData, persistedProfile.getId());

        // Assert
        assertAll(
                () -> assertEquals("Jane", result.getFirstName()),
                () -> assertEquals("Smith", result.getLastName()),
                () -> assertEquals("456 Elm St", result.getAddress()),
                () -> assertEquals("http://avatar.com/jane", result.getAvatarUrl()),
                () -> assertEquals(persistedNewOwner.getId(), result.getUser().getId()));
    }

    @Test
    @DisplayName("updateById: ignores null and blank values to maintain existing data")
    @Transactional
    void updateById_ShouldIgnoreInvalidValues() {
        // Arrange
        User owner = UserProvider.singleTemplate();
        owner.setCreatedAt(now);
        User persistedOwner = userRepo.save(owner);

        Profile initial = ProfileProvider.singleTemplate();
        initial.setUser(persistedOwner);
        initial.setCreatedAt(now);
        Profile persistedProfile = repo.save(initial);

        Profile updateData = new Profile();
        updateData.setFirstName("Alice");
        updateData.setLastName("  "); // Blank should be ignored
        updateData.setAddress(""); // Empty should be ignored
        updateData.setAvatarUrl(null); // Null should be ignored

        // Act
        Profile result = dao.updateById(updateData, persistedProfile.getId());

        // Assert
        assertAll(
                () -> assertEquals("Alice", result.getFirstName()),
                () -> assertEquals(initial.getLastName(), result.getLastName()),
                () -> assertEquals(initial.getAddress(), result.getAddress()),
                () -> assertEquals(initial.getAvatarUrl(), result.getAvatarUrl()));
    }

    @Test
    @DisplayName(
            "updateById: handles case where existing profile user is null and new user is provided")
    @Transactional
    void updateById_ShouldSetUserWhenCurrentlyNull() {
        // Arrange
        Profile initial = ProfileProvider.singleTemplate();
        initial.setUser(null);
        initial.setCreatedAt(now);
        Profile persistedProfile = repo.save(initial);

        User newUser = UserProvider.singleTemplate();
        newUser.setCreatedAt(now);
        User persistedNewUser = userRepo.save(newUser);

        Profile updateData = new Profile();
        updateData.setUser(persistedNewUser);

        // Act
        Profile result = dao.updateById(updateData, persistedProfile.getId());

        // Assert
        assertNotNull(result.getUser());
        assertEquals(persistedNewUser.getId(), result.getUser().getId());
    }

    @Test
    @DisplayName("updateById: throws NotFoundException when entity does not exist")
    @Transactional
    void updateById_ShouldThrowNotFound() {
        Profile data = new Profile();
        UUID randomId = UUID.randomUUID();
        assertThrows(NotFoundException.class, () -> dao.updateById(data, randomId));
    }

    @Test
    @DisplayName("existsByUniqueProperties: validates existence based on User ID")
    @Transactional
    void existsByUniqueProperties_ShouldValidateCorrectly() {
        // Arrange
        User owner = UserProvider.singleTemplate();
        owner.setCreatedAt(now);
        User persistedOwner = userRepo.save(owner);

        Profile profile = ProfileProvider.singleTemplate();
        profile.setUser(persistedOwner);
        profile.setCreatedAt(now);
        repo.save(profile);

        // Act & Assert
        Profile probe = new Profile();
        probe.setUser(persistedOwner);
        assertTrue(dao.existsByUniqueProperties(probe), "Should exist for persisted user");

        // Null User check
        Profile noUserProbe = new Profile();
        assertFalse(dao.existsByUniqueProperties(noUserProbe), "Should be false for null user");

        // Unsaved User check
        Profile unsavedUserProbe = new Profile();
        unsavedUserProbe.setUser(new User());
        assertFalse(
                dao.existsByUniqueProperties(unsavedUserProbe), "Should be false for unsaved user");
    }

    @Test
    @DisplayName("existsAllByIds: verifies multiple IDs against database count")
    @Transactional
    void existsAllByIds_ShouldHandleCollections() {
        // Arrange
        User owner = UserProvider.singleTemplate();
        owner.setCreatedAt(now);
        userRepo.save(owner);

        User owner2 = UserProvider.alternativeTemplate();
        owner2.setCreatedAt(now);
        userRepo.save(owner2);

        Profile p1 = ProfileProvider.singleTemplate();
        p1.setCreatedAt(now);
        p1.setUser(owner);
        Profile p2 = ProfileProvider.alternativeTemplate();
        p2.setCreatedAt(now);
        p2.setUser(owner2);

        UUID id1 = repo.save(p1).getId();
        UUID id2 = repo.save(p2).getId();

        // Act & Assert
        assertTrue(dao.existsAllByIds(List.of(id1, id2)));
        assertFalse(dao.existsAllByIds(List.of(id1, UUID.randomUUID())));
    }

    @Test
    @DisplayName("findById: standard generic behavior check")
    @Transactional
    void findById_ShouldRetrieveEntity() {
        // Arrange
        User owner = UserProvider.singleTemplate();
        owner.setCreatedAt(now);
        userRepo.save(owner);

        Profile profile = ProfileProvider.singleTemplate();
        profile.setCreatedAt(now);
        profile.setUser(owner);
        Profile persisted = repo.save(profile);

        // Act
        var found = dao.findById(persisted.getId());

        // Assert
        assertTrue(found.isPresent());
        assertEquals(persisted.getId(), found.get().getId());
    }
}
