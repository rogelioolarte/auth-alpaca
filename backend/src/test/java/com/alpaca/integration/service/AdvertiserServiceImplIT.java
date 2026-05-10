package com.alpaca.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.alpaca.entity.Advertiser;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.resources.AdvertiserProvider;
import com.alpaca.resources.UserProvider;
import com.alpaca.service.IUserService;
import com.alpaca.service.impl.AdvertiserServiceImpl;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link AdvertiserServiceImpl} */
@SpringBootTest
@Transactional
@DisplayName("AdvertiserServiceImpl Integration Tests")
class AdvertiserServiceImplIT {

    @Autowired private IUserService userService;
    @Autowired private AdvertiserServiceImpl service;

    private Instant now;

    @BeforeEach
    void setup() {
        // Prepare timestamp reference - No repository saves in setup per rules
        now = Instant.now();
    }

    // -------------------------------------------------------------------------
    // Specialized Methods
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findAllPageByIndexedTrue: Should return only advertisers where indexed is true")
    @Transactional
    void findAllPageByIndexedTrue_ShouldReturnFilteredResults() {
        // Arrange
        User user1 = userService.save(UserProvider.singleTemplate());
        User user2 = userService.save(UserProvider.alternativeTemplate());

        Advertiser indexed = AdvertiserProvider.singleTemplate();
        indexed.setUser(user1);
        indexed.setIndexed(true);
        indexed.setCreatedAt(now);

        Advertiser notIndexed = AdvertiserProvider.alternativeTemplate();
        notIndexed.setUser(user2);
        notIndexed.setIndexed(false);
        notIndexed.setCreatedAt(now);

        service.save(indexed);
        service.save(notIndexed);

        // Act
        Page<Advertiser> result = service.findAllPageByIndexedTrue(PageRequest.of(0, 10));

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().isIndexed()).isTrue();
    }

    // -------------------------------------------------------------------------
    // CRUD Operations (Inherited from GenericServiceImpl)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save: Should persist advertiser and manually set audit properties")
    @Transactional
    void save_ShouldPersistAdvertiser() {
        // Arrange
        Advertiser advertiser = AdvertiserProvider.singleTemplate();
        advertiser.setCreatedAt(now); // Critical: Manual audit setup

        // Act
        Advertiser saved = service.save(advertiser);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isAfter(now);
        assertThat(service.findById(saved.getId())).isNotNull();
    }

    @Test
    @DisplayName("findById: Should throw NotFoundException when advertiser does not exist")
    @Transactional
    void findById_ShouldThrowNotFound_WhenMissing() {
        UUID randomId = UUID.randomUUID();
        assertThatThrownBy(() -> service.findById(randomId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Advertiser with ID " + randomId + " not found");
    }

    @Test
    @DisplayName("updateById: Should update fields correctly and preserve ID")
    @Transactional
    void updateById_ShouldUpdateExistingAdvertiser() {
        // Arrange
        User user = userService.save(UserProvider.singleTemplate());

        Advertiser original = AdvertiserProvider.singleTemplate();
        original.setUser(user);
        original.setCreatedAt(now);
        Advertiser saved = service.save(original);

        Advertiser updateData = AdvertiserProvider.alternativeTemplate();
        updateData.setUser(user);
        updateData.setTitle("New Updated Name");

        // Act
        Advertiser updated = service.updateById(updateData, saved.getId());

        // Assert
        assertThat(updated.getId()).isEqualTo(saved.getId());
        assertThat(updated.getTitle()).isEqualTo("New Updated Name");
    }

    @Test
    @DisplayName("deleteById: Should remove advertiser from database")
    @Transactional
    void deleteById_ShouldRemoveEntity() {
        // Arrange
        User user = userService.save(UserProvider.singleTemplate());

        Advertiser advertiser = AdvertiserProvider.singleTemplate();
        advertiser.setUser(user);
        advertiser.setCreatedAt(now);
        Advertiser saved = service.save(advertiser);

        // Act
        service.deleteById(saved.getId());

        // Assert
        assertThat(service.existsById(saved.getId())).isFalse();
    }

    // -------------------------------------------------------------------------
    // Edge Cases & Branch Logic
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save: Should throw BadRequestException when advertiser is null")
    @Transactional
    void save_ShouldThrowBadRequest_WhenNull() {
        assertThatThrownBy(() -> service.save(null)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("findAllByIds: Should throw NotFoundException if one ID in the list is missing")
    @Transactional
    void findAllByIds_ShouldThrowNotFound_WhenOneIdIsMissing() {
        // Arrange
        User user = userService.save(UserProvider.singleTemplate());

        Advertiser a1 = AdvertiserProvider.singleTemplate();
        a1.setUser(user);
        a1.setCreatedAt(now);
        Advertiser saved = service.save(a1);

        List<UUID> ids = List.of(saved.getId(), UUID.randomUUID());

        // Act & Assert
        assertThatThrownBy(() -> service.findAllByIds(ids)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("existsByUniqueProperties: Should return correct boolean state")
    @Transactional
    void existsByUniqueProperties_ShouldWorkCorrectly() {
        // Arrange
        User user = userService.save(UserProvider.singleTemplate());

        Advertiser advertiser = AdvertiserProvider.singleTemplate();
        advertiser.setUser(user);
        advertiser.setCreatedAt(now);
        service.save(advertiser);

        // Act & Assert
        assertThat(service.existsByUniqueProperties(advertiser)).isTrue();

        Advertiser dummy = AdvertiserProvider.alternativeTemplate();
        assertThat(service.existsByUniqueProperties(dummy)).isFalse();
    }
}
