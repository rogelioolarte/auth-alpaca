package com.alpaca.integration.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.entity.Profile;
import com.alpaca.entity.User;
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
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
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
