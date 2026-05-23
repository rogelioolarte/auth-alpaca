package com.alpaca.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.alpaca.entity.Profile;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IUserDAO;
import com.alpaca.resources.ProfileProvider;
import com.alpaca.resources.UserProvider;
import com.alpaca.service.impl.ProfileServiceImpl;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link ProfileServiceImpl} */
@SpringBootTest
@Transactional
@DisplayName("ProfileServiceImpl Integration Tests")
class ProfileServiceImplIT {

    @Autowired private ProfileServiceImpl service;
    @Autowired private IUserDAO userDAO;

    private Instant now;

    @BeforeEach
    void setup() {
        now = Instant.now();
    }

    private Profile buildSingleProfile() {
        User user = userDAO.save(UserProvider.singleTemplate());
        Profile profile = ProfileProvider.singleTemplate();
        profile.setUser(user);
        profile.setCreatedAt(now);

        return profile;
    }

    private Profile createSingleProfile() {
        Profile profile = ProfileProvider.singleTemplate();
        profile.setCreatedAt(now);

        return profile;
    }

    private Profile buildAlternativeProfile() {
        User user = userDAO.save(UserProvider.alternativeTemplate());
        Profile profile = ProfileProvider.alternativeTemplate();
        profile.setUser(user);
        profile.setCreatedAt(now);

        return profile;
    }

    // ------------------------------------------------
    // save
    // ------------------------------------------------

    @Test
    @Transactional
    @DisplayName("save persists profile successfully")
    void save_ShouldPersistProfileSuccessfully() {

        Profile profile = buildSingleProfile();

        Profile saved = service.save(profile);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @Transactional
    @DisplayName("save throws exception when profile is null")
    void save_ShouldThrowBadRequest_WhenProfileIsNull() {

        assertThatThrownBy(() -> service.save(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Profile cannot be created");
    }

    @Test
    @Transactional
    @DisplayName("save throws exception when duplicated profile exists")
    void save_ShouldThrowBadRequest_WhenDuplicateExists() {

        Profile profile = buildSingleProfile();

        service.save(profile);

        Profile duplicated = createSingleProfile();
        duplicated.setUser(profile.getUser());

        assertThatThrownBy(() -> service.save(duplicated))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Profile already exists");
    }

    // ------------------------------------------------
    // findById
    // ------------------------------------------------

    @Test
    @Transactional
    @DisplayName("findById validates null id")
    void findById_ShouldValidateNullId() {

        assertThatThrownBy(() -> service.findById(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Profile cannot be found");
    }

    @Test
    @Transactional
    @DisplayName("findById throws exception when entity does not exist")
    void findById_ShouldThrowNotFound_WhenEntityDoesNotExist() {

        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Profile with ID " + id + " not found");
    }

    @Test
    @Transactional
    @DisplayName("findById returns existing profile")
    void findById_ShouldReturnExistingProfile() {

        Profile saved = service.save(buildSingleProfile());

        Profile found = service.findById(saved.getId());

        assertThat(found.getId()).isEqualTo(saved.getId());
    }

    // ------------------------------------------------
    // findAllByIds
    // ------------------------------------------------

    @Test
    @Transactional
    @DisplayName("findAllByIds validates invalid collections")
    void findAllByIds_ShouldValidateInvalidCollections() {

        assertThatThrownBy(() -> service.findAllByIds(null))
                .isInstanceOf(BadRequestException.class);

        Throwable thrown = catchThrowable(() -> service.findAllByIds(Collections.emptyList()));
        assertThat(thrown).as("Profile(s) cannot be found").isInstanceOf(BadRequestException.class);

        List<UUID> ids = new ArrayList<>();
        ids.add(UUID.randomUUID());
        ids.add(null);
        Throwable thrown2 = catchThrowable(() -> service.findAllByIds(ids));
        assertThat(thrown2)
                .as("Profile(s) cannot be found")
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @Transactional
    @DisplayName("findAllByIds throws exception when some ids are missing")
    void findAllByIds_ShouldThrowNotFound_WhenSomeIdsMissing() {

        Profile saved = service.save(buildSingleProfile());

        List<UUID> ids = List.of(saved.getId(), UUID.randomUUID());

        assertThatThrownBy(() -> service.findAllByIds(ids))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Some Profile(s) cannot be found");
    }

    @Test
    @Transactional
    @DisplayName("findAllByIds returns profiles successfully")
    void findAllByIds_ShouldReturnProfilesSuccessfully() {

        Profile profileOne = service.save(buildSingleProfile());

        Profile profileTwo = service.save(buildAlternativeProfile());

        List<Profile> result =
                service.findAllByIds(List.of(profileOne.getId(), profileTwo.getId()));

        assertThat(result).hasSize(2);
    }

    // ------------------------------------------------
    // updateById
    // ------------------------------------------------

    @Test
    @Transactional
    @DisplayName("updateById validates null inputs")
    void updateById_ShouldValidateNullInputs() {

        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.updateById(null, id))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Profile with ID " + id + " cannot be updated");

        Throwable thrown = catchThrowable(() -> service.updateById(buildSingleProfile(), null));
        assertThat(thrown)
                .as("Profile with ID null cannot be updated")
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @Transactional
    @DisplayName("updateById throws exception when profile does not exist")
    void updateById_ShouldThrowNotFound_WhenProfileDoesNotExist() {

        UUID id = UUID.randomUUID();

        Profile update = buildAlternativeProfile();

        assertThatThrownBy(() -> service.updateById(update, id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Profile with ID " + id + " not found");
    }

    @Test
    @Transactional
    @DisplayName("updateById updates all text fields")
    void updateById_ShouldUpdateAllTextFields() {

        Profile saved = service.save(buildSingleProfile());

        Profile update = buildAlternativeProfile();

        Profile updated = service.updateById(update, saved.getId());

        assertThat(updated.getFirstName()).isEqualTo(update.getFirstName());
        assertThat(updated.getLastName()).isEqualTo(update.getLastName());
        assertThat(updated.getAddress()).isEqualTo(update.getAddress());
        assertThat(updated.getAvatarUrl()).isEqualTo(update.getAvatarUrl());
    }

    @Test
    @Transactional
    @DisplayName("updateById ignores blank text values")
    void updateById_ShouldIgnoreBlankTextValues() {

        Profile saved = service.save(buildSingleProfile());

        String originalFirstName = saved.getFirstName();
        String originalLastName = saved.getLastName();
        String originalAddress = saved.getAddress();
        String originalAvatarUrl = saved.getAvatarUrl();

        Profile update = buildAlternativeProfile();
        update.setFirstName(" ");
        update.setLastName("");
        update.setAddress(" ");
        update.setAvatarUrl("");

        Profile updated = service.updateById(update, saved.getId());

        assertThat(updated.getFirstName()).isEqualTo(originalFirstName);
        assertThat(updated.getLastName()).isEqualTo(originalLastName);
        assertThat(updated.getAddress()).isEqualTo(originalAddress);
        assertThat(updated.getAvatarUrl()).isEqualTo(originalAvatarUrl);
    }

    @Test
    @Transactional
    @DisplayName("updateById updates user when user differs")
    void updateById_ShouldUpdateUser_WhenUserDiffers() {

        Profile saved = service.save(buildSingleProfile());

        Profile update = buildAlternativeProfile();

        Profile updated = service.updateById(update, saved.getId());

        assertThat(updated.getUser()).isEqualTo(saved.getUser());
    }

    @Test
    @Transactional
    @DisplayName("updateById ignores user when user ids are equal")
    void updateById_ShouldIgnoreUser_WhenUserIdsAreEqual() {

        Profile profile = buildSingleProfile();

        Profile saved = service.save(profile);
        User user = saved.getUser();

        Profile update = buildAlternativeProfile();

        User sameUser = update.getUser();
        sameUser.setId(profile.getUser().getId());
        update.setUser(sameUser);

        Profile updated = service.updateById(update, saved.getId());

        assertThat(updated.getUser().getId()).isEqualTo(user.getId());
        assertThat(updated.getUser().getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    @Transactional
    @DisplayName("updateById ignores null user")
    void updateById_ShouldIgnoreNullUser() {

        Profile profile = buildSingleProfile();

        Profile saved = service.save(profile);
        User user = saved.getUser();

        Profile update = buildAlternativeProfile();
        update.setUser(null);

        Profile updated = service.updateById(update, saved.getId());

        assertThat(updated.getUser()).isEqualTo(user);
    }

    @Test
    @Transactional
    @DisplayName("updateById ignores user without id")
    void updateById_ShouldIgnoreUserWithoutId() {

        Profile profile = buildSingleProfile();

        Profile saved = service.save(profile);
        User user = saved.getUser();

        Profile update = buildAlternativeProfile();

        User invalidUser = update.getUser();
        invalidUser.setId(null);

        update.setUser(invalidUser);

        Profile updated = service.updateById(update, saved.getId());

        assertThat(updated.getUser()).isEqualTo(user);
    }

    // ------------------------------------------------
    // deleteById
    // ------------------------------------------------

    @Test
    @Transactional
    @DisplayName("deleteById validates null id")
    void deleteById_ShouldValidateNullId() {

        assertThatThrownBy(() -> service.deleteById(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Profile cannot be deleted");
    }

    @Test
    @Transactional
    @DisplayName("deleteById validates missing entity")
    void deleteById_ShouldValidateMissingEntity() {

        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.deleteById(id))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Profile not exists");
    }

    @Test
    @Transactional
    @DisplayName("deleteById removes entity successfully")
    void deleteById_ShouldRemoveEntitySuccessfully() {

        Profile saved = service.save(buildSingleProfile());

        service.deleteById(saved.getId());

        assertThat(service.existsById(saved.getId())).isFalse();
    }

    // ------------------------------------------------
    // findAll / pagination
    // ------------------------------------------------

    @Test
    @Transactional
    @DisplayName("findAll returns persisted profiles")
    void findAll_ShouldReturnPersistedProfiles() {

        service.save(buildSingleProfile());

        service.save(buildAlternativeProfile());

        List<Profile> result = service.findAll();

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
                .hasMessageContaining("Profile(s) Page cannot be created");
    }

    @Test
    @Transactional
    @DisplayName("findAllPage returns paginated result")
    void findAllPage_ShouldReturnPaginatedResult() {

        service.save(buildSingleProfile());

        Page<Profile> result = service.findAllPage(Pageable.ofSize(10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotEmpty();
    }

    // ------------------------------------------------
    // existence methods
    // ------------------------------------------------

    @Test
    @Transactional
    @DisplayName("existsById handles all branches")
    void existsById_ShouldHandleAllBranches() {

        Profile saved = service.save(buildSingleProfile());

        assertThat(service.existsById(saved.getId())).isTrue();

        assertThat(service.existsById(UUID.randomUUID())).isFalse();

        assertThat(service.existsById(null)).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("existsAllByIds handles edge cases")
    void existsAllByIds_ShouldHandleEdgeCases() {

        assertThat(service.existsAllByIds(null)).isFalse();

        assertThat(service.existsAllByIds(Collections.emptyList())).isFalse();

        List<UUID> ids = new ArrayList<>();
        ids.add(null);
        assertThat(service.existsAllByIds(ids)).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("existsAllByIds validates persisted ids")
    void existsAllByIds_ShouldValidatePersistedIds() {

        Profile profileOne = service.save(buildSingleProfile());

        Profile profileTwo = service.save(buildAlternativeProfile());

        assertThat(service.existsAllByIds(List.of(profileOne.getId(), profileTwo.getId())))
                .isTrue();

        assertThat(service.existsAllByIds(List.of(profileOne.getId(), UUID.randomUUID())))
                .isFalse();
    }

    @Test
    @Transactional
    @DisplayName("existsByUniqueProperties handles null entity")
    void existsByUniqueProperties_ShouldHandleNullEntity() {

        assertThat(service.existsByUniqueProperties(null)).isFalse();
    }
}
