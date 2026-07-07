package com.alpaca.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.alpaca.entity.Advertiser;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IUserDAO;
import com.alpaca.resources.provider.AdvertiserProvider;
import com.alpaca.resources.provider.UserProvider;
import com.alpaca.resources.utility.BaseIntegrationTests;
import com.alpaca.service.impl.AdvertiserServiceImpl;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link AdvertiserServiceImpl} */
@DisplayName("AdvertiserServiceImpl Integration Tests")
class AdvertiserServiceImplIT extends BaseIntegrationTests {

    @Autowired private AdvertiserServiceImpl service;
    @Autowired private IUserDAO userDAO;

    private Instant now;

    @BeforeEach
    void setup() {
        now = Instant.now();
    }

    private Advertiser buildSingleAdvertiser() {
        User user = userDAO.save(buildUser());
        Advertiser advertiser = AdvertiserProvider.singleTemplate();
        advertiser.setUser(user);
        advertiser.setCreatedAt(now);

        return advertiser;
    }

    private Advertiser buildAlternativeAdvertiser() {
        User user = userDAO.save(buildAlternativeUser());
        Advertiser advertiser = AdvertiserProvider.alternativeTemplate();
        advertiser.setUser(user);
        advertiser.setCreatedAt(now);

        return advertiser;
    }

    private User buildUser() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);

        return user;
    }

    private User buildAlternativeUser() {
        User user = UserProvider.alternativeTemplate();
        user.setCreatedAt(now);

        return user;
    }

    // ------------------------------------------------
    // save
    // ------------------------------------------------

    @Test
    @Transactional
    @DisplayName("save persists advertiser successfully")
    void save_ShouldPersistAdvertiserSuccessfully() {

        Advertiser advertiser = buildSingleAdvertiser();

        Advertiser saved = service.save(advertiser);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    // ------------------------------------------------
    // updateById
    // ------------------------------------------------

    @Test
    @Transactional
    @DisplayName("updateById throws BadRequestException when advertiser is null")
    void updateById_ShouldThrowBadRequest_WhenAdvertiserIsNull() {

        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.updateById(null, id))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Advertiser with ID " + id + " cannot be updated");
    }

    @Test
    @Transactional
    @DisplayName("updateById throws BadRequestException when id is null")
    void updateById_ShouldThrowBadRequest_WhenIdIsNull() {

        Advertiser advertiser = buildSingleAdvertiser();

        assertThatThrownBy(() -> service.updateById(advertiser, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Advertiser with ID null cannot be updated");
    }

    @Test
    @Transactional
    @DisplayName("updateById throws NotFoundException when advertiser does not exist")
    void updateById_ShouldThrowNotFound_WhenAdvertiserDoesNotExist() {

        Advertiser advertiser = buildAlternativeAdvertiser();

        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.updateById(advertiser, id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Advertiser with ID " + id + " not found");
    }

    @Test
    @Transactional
    @DisplayName("updateById updates all text fields and indexed flag")
    void updateById_ShouldUpdateAllFields() {

        Advertiser saved = service.save(buildSingleAdvertiser());

        Advertiser update = buildAlternativeAdvertiser();
        update.setIndexed(!saved.isIndexed());

        Advertiser result = service.updateById(update, saved.getId());

        assertAll(
                () -> assertThat(result.getTitle()).isEqualTo(update.getTitle()),
                () -> assertThat(result.getDescription()).isEqualTo(update.getDescription()),
                () -> assertThat(result.getAvatarUrl()).isEqualTo(update.getAvatarUrl()),
                () -> assertThat(result.getBannerUrl()).isEqualTo(update.getBannerUrl()),
                () -> assertThat(result.getPublicLocation()).isEqualTo(update.getPublicLocation()),
                () ->
                        assertThat(result.getPublicUrlLocation())
                                .isEqualTo(update.getPublicUrlLocation()),
                () -> assertThat(result.isIndexed()).isEqualTo(update.isIndexed()));
    }

    @Test
    @Transactional
    @DisplayName("updateById updates user when user ids differ")
    void updateById_ShouldUpdateUser_WhenUserIdsDiffer() {
        Advertiser saved = service.save(buildSingleAdvertiser());

        User newUser = userDAO.save(UserProvider.alternativeTemplate());

        Advertiser update = buildAlternativeAdvertiser();
        update.setUser(newUser);

        Advertiser result = service.updateById(update, saved.getId());

        assertThat(result.getUser()).isNotNull();
        assertThat(result.getUser().getId()).isEqualTo(newUser.getId());
    }

    @Test
    @Transactional
    @DisplayName("updateById does not update user when user ids are equal")
    void updateById_ShouldNotUpdateUser_WhenUserIdsAreEqual() {
        Advertiser saved = service.save(buildSingleAdvertiser());

        Advertiser update = buildAlternativeAdvertiser();
        update.setUser(saved.getUser());

        Advertiser result = service.updateById(update, saved.getId());

        assertThat(result.getUser()).isNotNull();
        assertThat(result.getUser().getId()).isEqualTo(saved.getUser().getId());
    }

    @Test
    @Transactional
    @DisplayName("updateById ignores null user")
    void updateById_ShouldIgnoreNullUser() {

        Advertiser saved = service.save(buildSingleAdvertiser());

        UUID originalUserId = saved.getUser() != null ? saved.getUser().getId() : null;

        Advertiser update = buildAlternativeAdvertiser();
        update.setUser(null);

        Advertiser result = service.updateById(update, saved.getId());

        if (originalUserId != null) {
            assertThat(result.getUser()).isNotNull();
            assertThat(result.getUser().getId()).isEqualTo(originalUserId);
        } else {
            assertThat(result.getUser()).isNull();
        }
    }

    @Test
    @Transactional
    @DisplayName("updateById ignores user with null id")
    void updateById_ShouldIgnoreUserWithNullId() {
        Advertiser saved = service.save(buildSingleAdvertiser());
        User savedUser = saved.getUser();

        User updateUser = UserProvider.alternativeTemplate();
        updateUser.setId(null);

        Advertiser update = buildAlternativeAdvertiser();
        update.setUser(updateUser);

        Advertiser result = service.updateById(update, saved.getId());

        assertThat(result.getUser()).isNotNull();
        assertThat(result.getUser().getId()).isEqualTo(savedUser.getId());
    }

    @Test
    @Transactional
    @DisplayName("updateById ignores blank text fields")
    void updateById_ShouldIgnoreBlankTextFields() {

        Advertiser saved = service.save(buildSingleAdvertiser());

        String originalTitle = saved.getTitle();
        String originalDescription = saved.getDescription();
        String originalAvatarUrl = saved.getAvatarUrl();
        String originalBannerUrl = saved.getBannerUrl();
        String originalPublicLocation = saved.getPublicLocation();
        String originalPublicUrlLocation = saved.getPublicUrlLocation();

        Advertiser update = buildAlternativeAdvertiser();
        update.setTitle(" ");
        update.setDescription(" ");
        update.setAvatarUrl(" ");
        update.setBannerUrl(" ");
        update.setPublicLocation(" ");
        update.setPublicUrlLocation(" ");

        Advertiser result = service.updateById(update, saved.getId());

        assertAll(
                () -> assertThat(result.getTitle()).isEqualTo(originalTitle),
                () -> assertThat(result.getDescription()).isEqualTo(originalDescription),
                () -> assertThat(result.getAvatarUrl()).isEqualTo(originalAvatarUrl),
                () -> assertThat(result.getBannerUrl()).isEqualTo(originalBannerUrl),
                () -> assertThat(result.getPublicLocation()).isEqualTo(originalPublicLocation),
                () ->
                        assertThat(result.getPublicUrlLocation())
                                .isEqualTo(originalPublicUrlLocation));
    }

    // ------------------------------------------------
    // findAllByIndexedTrue
    // ------------------------------------------------

    @Test
    @Transactional
    @DisplayName("findAllByIndexedTrue returns only indexed advertisers")
    void findAllByIndexedTrue_ShouldReturnOnlyIndexedAdvertisers() {

        Advertiser indexedAdvertiser = buildSingleAdvertiser();
        indexedAdvertiser.setIndexed(true);

        Advertiser nonIndexedAdvertiser = buildAlternativeAdvertiser();
        nonIndexedAdvertiser.setIndexed(false);

        service.save(indexedAdvertiser);
        service.save(nonIndexedAdvertiser);

        Page<Advertiser> result = service.findAllByIndexedTrue(Pageable.ofSize(10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent()).allMatch(Advertiser::isIndexed);
    }

    // ------------------------------------------------
    // inherited generic methods
    // ------------------------------------------------

    @Test
    @Transactional
    @DisplayName("findById returns persisted advertiser")
    void findById_ShouldReturnPersistedAdvertiser() {

        Advertiser saved = service.save(buildSingleAdvertiser());

        Advertiser result = service.findById(saved.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(saved.getId());
    }

    @Test
    @Transactional
    @DisplayName("findById validates null id")
    void findById_ShouldValidateNullId() {

        assertThatThrownBy(() -> service.findById(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Advertiser cannot be found");
    }

    @Test
    @Transactional
    @DisplayName("existsById returns correct values")
    void existsById_ShouldReturnCorrectValues() {

        Advertiser saved = service.save(buildSingleAdvertiser());

        assertThat(service.existsById(saved.getId())).isTrue();
        assertThat(service.existsById(UUID.randomUUID())).isFalse();
        assertThat(service.existsById(null)).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("findAll returns persisted advertisers")
    void findAll_ShouldReturnPersistedAdvertisers() {

        service.save(buildSingleAdvertiser());
        service.save(buildAlternativeAdvertiser());

        List<Advertiser> result = service.findAll();

        assertAll(
                () -> assertThat(result).isNotEmpty(),
                () -> assertThat(result).hasSizeGreaterThanOrEqualTo(2));
    }

    @Test
    @Transactional
    @DisplayName("findAllPage validates null pageable")
    void findAllPage_ShouldValidateNullPageable() {

        assertThatThrownBy(() -> service.findAllPage(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Advertiser(s) Page cannot be created");
    }

    @Test
    @Transactional
    @DisplayName("findAllPage returns paginated advertisers")
    void findAllPage_ShouldReturnPaginatedAdvertisers() {

        service.save(buildSingleAdvertiser());

        Page<Advertiser> result = service.findAllPage(Pageable.ofSize(10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotEmpty();
    }

    @Test
    @Transactional
    @DisplayName("existsAllByIds handles invalid values")
    void existsAllByIds_ShouldHandleInvalidValues() {

        assertThat(service.existsAllByIds(null)).isFalse();

        assertThat(service.existsAllByIds(Collections.emptyList())).isFalse();

        List<UUID> ids = new ArrayList<>();
        ids.add(null);

        assertThat(service.existsAllByIds(ids)).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("existsByUniqueProperties handles null advertiser")
    void existsByUniqueProperties_ShouldHandleNullAdvertiser() {

        assertThat(service.existsByUniqueProperties(null)).isFalse();
    }
}
