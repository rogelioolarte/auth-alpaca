package com.alpaca.integration.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.entity.Advertiser;
import com.alpaca.entity.User;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IAdvertiserDAO;
import com.alpaca.persistence.impl.AdvertiserDAOImpl;
import com.alpaca.repository.AdvertiserRepo;
import com.alpaca.repository.UserRepo;
import com.alpaca.resources.AdvertiserProvider;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link AdvertiserDAOImpl} */
@DataJpaTest
@Import({AdvertiserDAOImpl.class})
@DisplayName("AdvertiserDAOImpl Integration Tests")
class AdvertiserDAOImplIT {

    @Autowired private IAdvertiserDAO dao;
    @Autowired private AdvertiserRepo repo;
    @Autowired private UserRepo userRepo;

    private Instant now;

    @BeforeEach
    void setup() {
        now = Instant.now();
    }

    @Test
    @DisplayName("updateById: performs full update on all scalar fields and flags")
    @Transactional
    void updateById_ShouldPerformFullUpdate() {
        // Arrange
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);
        User persistedUser = userRepo.save(user);

        Advertiser initial = AdvertiserProvider.singleTemplate();
        initial.setUser(persistedUser);
        initial.setCreatedAt(now);
        initial.setIndexed(false);
        Advertiser persisted = repo.save(initial);

        Advertiser updateData = new Advertiser();
        updateData.setTitle("Updated Title");
        updateData.setDescription("Updated Description");
        updateData.setAvatarUrl("https://new-avatar.com");
        updateData.setBannerUrl("https://new-banner.com");
        updateData.setPublicLocation("New York, USA");
        updateData.setPublicUrlLocation("https://maps.new-url.com");
        updateData.setIndexed(true);

        // Act
        Advertiser result = dao.updateById(updateData, persisted.getId());

        // Assert
        assertAll(
                () -> assertEquals("Updated Title", result.getTitle()),
                () -> assertEquals("Updated Description", result.getDescription()),
                () -> assertEquals("https://new-avatar.com", result.getAvatarUrl()),
                () -> assertEquals("https://new-banner.com", result.getBannerUrl()),
                () -> assertEquals("New York, USA", result.getPublicLocation()),
                () -> assertEquals("https://maps.new-url.com", result.getPublicUrlLocation()),
                () -> assertTrue(result.isIndexed()),
                () -> assertEquals(persistedUser.getId(), result.getUser().getId()));
    }

    @Test
    @DisplayName("updateById: ignores null or blank fields and handles identical user ID")
    @Transactional
    void updateById_ShouldIgnoreNullsAndHandleSameUser() {
        // Arrange
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);
        User persistedUser = userRepo.save(user);

        Advertiser initial = AdvertiserProvider.singleTemplate();
        initial.setUser(persistedUser);
        initial.setCreatedAt(now);
        Advertiser persisted = repo.save(initial);

        // Partial update: Only title changed, and same user ID provided
        Advertiser updateData = new Advertiser();
        updateData.setTitle("Partial Title");
        updateData.setUser(persistedUser); // Same user

        // Act
        Advertiser result = dao.updateById(updateData, persisted.getId());

        // Assert
        assertEquals("Partial Title", result.getTitle());
        assertEquals(initial.getDescription(), result.getDescription()); // Maintained
        assertEquals(persistedUser.getId(), result.getUser().getId());
    }

    @Test
    @DisplayName("updateById: updates user when a different user is provided")
    @Transactional
    void updateById_ShouldChangeUser() {
        // Arrange
        User user1 = UserProvider.singleTemplate();
        user1.setCreatedAt(now);
        User userRepo1 = userRepo.save(user1);

        User user2 = UserProvider.alternativeTemplate();
        user2.setCreatedAt(now);
        User userRepo2 = userRepo.save(user2);

        Advertiser initial = AdvertiserProvider.singleTemplate();
        initial.setUser(userRepo1);
        initial.setCreatedAt(now);
        Advertiser persisted = repo.save(initial);

        Advertiser updateData = new Advertiser();
        updateData.setUser(userRepo2);

        // Act
        Advertiser result = dao.updateById(updateData, persisted.getId());

        // Assert
        assertEquals(userRepo2.getId(), result.getUser().getId());
    }

    @Test
    @DisplayName("updateById: throws NotFoundException for invalid ID")
    @Transactional
    void updateById_ShouldThrowNotFound() {
        Advertiser data = new Advertiser();
        UUID randomId = UUID.randomUUID();
        assertThrows(NotFoundException.class, () -> dao.updateById(data, randomId));
    }

    @Test
    @DisplayName("existsByUniqueProperties: validates existence by User ID")
    @Transactional
    void existsByUniqueProperties_ShouldWork() {
        // Arrange
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);
        User persistedUser = userRepo.save(user);

        Advertiser adv = AdvertiserProvider.singleTemplate();
        adv.setUser(persistedUser);
        adv.setCreatedAt(now);
        repo.save(adv);

        // Act & Assert
        Advertiser probe = new Advertiser();
        probe.setUser(persistedUser);
        assertTrue(dao.existsByUniqueProperties(probe));

        // Edge case: User with no ID (unsaved)
        Advertiser probeNoId = new Advertiser();
        probeNoId.setUser(new User());
        assertFalse(dao.existsByUniqueProperties(probeNoId));

        // Edge case: Null user
        Advertiser probeNullUser = new Advertiser();
        assertFalse(dao.existsByUniqueProperties(probeNullUser));
    }

    @Test
    @DisplayName("existsAllByIds: verifies presence of multiple IDs")
    @Transactional
    void existsAllByIds_ShouldVerifyCount() {
        // Arrange
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);
        userRepo.save(user);

        User owner2 = UserProvider.alternativeTemplate();
        owner2.setCreatedAt(now);
        userRepo.save(owner2);

        Advertiser a1 = AdvertiserProvider.singleTemplate();
        a1.setCreatedAt(now);
        a1.setUser(user);
        Advertiser a2 = AdvertiserProvider.alternativeTemplate();
        a2.setCreatedAt(now);
        a2.setUser(owner2);

        Advertiser t1 = repo.save(a1);
        Advertiser t2 = repo.save(a2);

        // Act & Assert
        assertTrue(dao.existsAllByIds(List.of(t1.getId(), t2.getId())));
        assertFalse(dao.existsAllByIds(List.of(t1.getId(), UUID.randomUUID())));
    }

    @Test
    @DisplayName("findAllPageByIndexedTrue: retrieves only indexed advertisers")
    @Transactional
    void findAllPageByIndexedTrue_ShouldFilterCorrectly() {
        // Arrange
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);
        userRepo.save(user);

        User owner2 = UserProvider.alternativeTemplate();
        owner2.setCreatedAt(now);
        userRepo.save(owner2);

        Advertiser indexed = AdvertiserProvider.singleTemplate();
        indexed.setIndexed(true);
        indexed.setCreatedAt(now);
        indexed.setUser(user);

        Advertiser notIndexed = AdvertiserProvider.alternativeTemplate();
        notIndexed.setIndexed(false);
        notIndexed.setCreatedAt(now);
        notIndexed.setUser(owner2);

        repo.saveAll(List.of(indexed, notIndexed));

        // Act
        Page<Advertiser> page = dao.findAllPageByIndexedTrue(PageRequest.of(0, 10));

        // Assert
        assertEquals(1, page.getTotalElements());
        assertTrue(page.getContent().getFirst().isIndexed());
    }
}
