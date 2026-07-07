package com.alpaca.integration.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.entity.Advertiser;
import com.alpaca.entity.User;
import com.alpaca.persistence.IAdvertiserDAO;
import com.alpaca.persistence.impl.AdvertiserDAOImpl;
import com.alpaca.repository.AdvertiserRepo;
import com.alpaca.repository.UserRepo;
import com.alpaca.resources.provider.AdvertiserProvider;
import com.alpaca.resources.provider.UserProvider;
import com.alpaca.resources.utility.DataJpaIntegrationTest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link AdvertiserDAOImpl} */
@DataJpaIntegrationTest
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
    @DisplayName("findAllByIndexedTrue: retrieves only indexed advertisers")
    @Transactional
    void findAllByIndexedTrue_ShouldFilterCorrectly() {
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
        Page<Advertiser> page = dao.findAllByIndexedTrue(PageRequest.of(0, 10));

        // Assert
        assertEquals(1, page.getTotalElements());
        assertTrue(page.getContent().getFirst().isIndexed());
    }
}
