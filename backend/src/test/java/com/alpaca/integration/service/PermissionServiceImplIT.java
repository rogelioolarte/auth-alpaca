package com.alpaca.integration.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.alpaca.entity.Permission;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.resources.PermissionProvider;
import com.alpaca.service.impl.PermissionServiceImpl;
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

/** Integration tests for {@link PermissionServiceImpl} */
@SpringBootTest
@Transactional
@DisplayName("PermissionServiceImpl Integration Tests")
class PermissionServiceImplIT {

    @Autowired private PermissionServiceImpl service;

    private Instant now;

    @BeforeEach
    void setup() {
        now = Instant.now();
    }

    private Permission buildSinglePermission() {
        Permission permission = PermissionProvider.singleTemplate();
        permission.setCreatedAt(now);

        return permission;
    }

    private Permission buildAlternativePermission() {
        Permission permission = PermissionProvider.alternativeTemplate();
        permission.setCreatedAt(now);

        return permission;
    }

    // ------------------------------------------------
    // findById
    // ------------------------------------------------

    @Test
    @DisplayName("findById throws BadRequestException when id is null")
    void findById_ShouldThrowBadRequest_WhenIdIsNull() {

        assertThatThrownBy(() -> service.findById(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Permission cannot be found");
    }

    @Test
    @DisplayName("findById throws NotFoundException when permission does not exist")
    void findById_ShouldThrowNotFound_WhenPermissionDoesNotExist() {

        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Permission with ID " + id + " not found");
    }

    @Test
    @Transactional
    @DisplayName("findById returns persisted permission")
    void findById_ShouldReturnPersistedPermission() {

        Permission saved = service.save(buildSinglePermission());

        Permission result = service.findById(saved.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(saved.getId());
    }

    // ------------------------------------------------
    // findAllByIds
    // ------------------------------------------------

    @Test
    @DisplayName("findAllByIds validates invalid collections")
    void findAllByIds_ShouldValidateInvalidCollections() {

        assertThatThrownBy(() -> service.findAllByIds(null))
                .isInstanceOf(BadRequestException.class);

        Throwable thrown = catchThrowable(() -> service.findAllByIds(Collections.emptyList()));
        assertThat(thrown)
                .as("Permission(s) cannot be found")
                .isInstanceOf(BadRequestException.class);

        List<UUID> ids = new ArrayList<>();
        ids.add(UUID.randomUUID());
        ids.add(null);

        assertThatThrownBy(() -> service.findAllByIds(ids)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @Transactional
    @DisplayName("findAllByIds throws NotFoundException when some ids are missing")
    void findAllByIds_ShouldThrowNotFound_WhenSomeIdsAreMissing() {

        Permission saved = service.save(buildSinglePermission());

        List<UUID> ids = List.of(saved.getId(), UUID.randomUUID());

        assertThatThrownBy(() -> service.findAllByIds(ids))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Some Permission(s) cannot be found");
    }

    @Test
    @Transactional
    @DisplayName("findAllByIds returns permissions when all ids exist")
    void findAllByIds_ShouldReturnPermissions_WhenAllIdsExist() {

        Permission first = service.save(buildSinglePermission());

        Permission second = service.save(buildAlternativePermission());

        List<Permission> result = service.findAllByIds(List.of(first.getId(), second.getId()));

        assertThat(result)
                .hasSize(2)
                .extracting(Permission::getId)
                .containsExactlyInAnyOrder(first.getId(), second.getId());
    }

    // ------------------------------------------------
    // save
    // ------------------------------------------------

    @Test
    @DisplayName("save throws BadRequestException when permission is null")
    void save_ShouldThrowBadRequest_WhenPermissionIsNull() {

        assertThatThrownBy(() -> service.save(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Permission cannot be created");
    }

    @Test
    @Transactional
    @DisplayName("save throws BadRequestException when permission already exists")
    void save_ShouldThrowBadRequest_WhenPermissionAlreadyExists() {

        Permission permission = buildSinglePermission();

        service.save(permission);

        Permission duplicate = PermissionProvider.singleTemplate();
        duplicate.setCreatedAt(now);

        assertThatThrownBy(() -> service.save(duplicate))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Permission already exists");
    }

    @Test
    @Transactional
    @DisplayName("save persists permission successfully")
    void save_ShouldPersistPermissionSuccessfully() {

        Permission result = service.save(buildSinglePermission());

        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isNotBlank();
    }

    // ------------------------------------------------
    // saveAll
    // ------------------------------------------------

    @Test
    @DisplayName("saveAll validates invalid collections")
    void saveAll_ShouldValidateInvalidCollections() {

        assertThatThrownBy(() -> service.saveAll(null)).isInstanceOf(BadRequestException.class);

        Throwable thrown = catchThrowable(() -> service.saveAll(Collections.emptyList()));
        assertThat(thrown)
                .as("Permission(s) cannot be created")
                .isInstanceOf(BadRequestException.class);

        List<Permission> permissions = new ArrayList<>();
        permissions.add(buildSinglePermission());
        permissions.add(null);

        assertThatThrownBy(() -> service.saveAll(permissions))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @Transactional
    @DisplayName("saveAll persists all permissions")
    void saveAll_ShouldPersistAllPermissions() {

        Permission first = buildSinglePermission();

        Permission second = buildAlternativePermission();

        List<Permission> result = service.saveAll(List.of(first, second));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Permission::getId).doesNotContainNull();
    }

    // ------------------------------------------------
    // updateById
    // ------------------------------------------------

    @Test
    @DisplayName("updateById validates null arguments")
    void updateById_ShouldValidateNullArguments() {

        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.updateById(null, id))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Permission with ID " + id + " cannot be updated");

        Throwable thrown = catchThrowable(() -> service.updateById(buildSinglePermission(), null));
        assertThat(thrown)
                .as("Permission with ID null cannot be updated")
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @Transactional
    @DisplayName("updateById throws NotFoundException when permission does not exist")
    void updateById_ShouldThrowNotFound_WhenPermissionDoesNotExist() {

        Permission update = buildAlternativePermission();

        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.updateById(update, id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Permission with ID " + id + " not found");
    }

    @Test
    @Transactional
    @DisplayName("updateById updates permission name when incoming value differs")
    void updateById_ShouldUpdatePermissionName_WhenIncomingValueDiffers() {

        Permission saved = service.save(buildSinglePermission());

        Permission update = buildAlternativePermission();

        Permission result = service.updateById(update, saved.getId());

        assertThat(result.getId()).isEqualTo(saved.getId());
        assertThat(result.getName()).isEqualTo(update.getName());
    }

    @Test
    @Transactional
    @DisplayName("updateById does not modify name when incoming value is blank")
    void updateById_ShouldNotModifyName_WhenIncomingValueIsBlank() {

        Permission saved = service.save(buildSinglePermission());

        String originalName = saved.getName();

        Permission update = PermissionProvider.alternativeTemplate();
        update.setCreatedAt(now);
        update.setName(" ");

        Permission result = service.updateById(update, saved.getId());

        assertThat(result.getName()).isEqualTo(originalName);
    }

    @Test
    @Transactional
    @DisplayName("updateById does not modify name when incoming value is identical")
    void updateById_ShouldNotModifyName_WhenIncomingValueIsIdentical() {

        Permission saved = service.save(buildSinglePermission());

        Permission update = PermissionProvider.alternativeTemplate();
        update.setCreatedAt(now);
        update.setName(saved.getName());

        Permission result = service.updateById(update, saved.getId());

        assertThat(result.getName()).isEqualTo(saved.getName());
    }

    // ------------------------------------------------
    // deleteById
    // ------------------------------------------------

    @Test
    @DisplayName("deleteById validates invalid arguments")
    void deleteById_ShouldValidateInvalidArguments() {

        assertThatThrownBy(() -> service.deleteById(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Permission cannot be deleted");

        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.deleteById(id))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Permission not exists");
    }

    @Test
    @Transactional
    @DisplayName("deleteById removes permission")
    void deleteById_ShouldRemovePermission() {

        Permission saved = service.save(buildSinglePermission());

        service.deleteById(saved.getId());

        assertThat(service.existsById(saved.getId())).isFalse();
    }

    // ------------------------------------------------
    // findAll
    // ------------------------------------------------

    @Test
    @Transactional
    @DisplayName("findAll returns persisted permissions")
    void findAll_ShouldReturnPersistedPermissions() {

        service.save(buildSinglePermission());

        service.save(buildAlternativePermission());

        List<Permission> result = service.findAll();

        assertAll(
                () -> assertThat(result).isNotEmpty(),
                () -> assertThat(result).hasSizeGreaterThanOrEqualTo(2));
    }

    // ------------------------------------------------
    // findAllPage
    // ------------------------------------------------

    @Test
    @DisplayName("findAllPage throws BadRequestException when pageable is null")
    void findAllPage_ShouldThrowBadRequest_WhenPageableIsNull() {

        assertThatThrownBy(() -> service.findAllPage(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Permission(s) Page cannot be created");
    }

    @Test
    @Transactional
    @DisplayName("findAllPage returns paginated permissions")
    void findAllPage_ShouldReturnPaginatedPermissions() {

        service.save(buildSinglePermission());

        Page<Permission> page = service.findAllPage(Pageable.ofSize(10));

        assertThat(page).isNotNull();
        assertThat(page.getContent()).isNotEmpty();
    }

    // ------------------------------------------------
    // existsById
    // ------------------------------------------------

    @Test
    @DisplayName("existsById returns false when id is null")
    void existsById_ShouldReturnFalse_WhenIdIsNull() {

        assertThat(service.existsById(null)).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("existsById returns correct existence result")
    void existsById_ShouldReturnCorrectExistenceResult() {

        Permission saved = service.save(buildSinglePermission());

        assertThat(service.existsById(saved.getId())).isTrue();
        assertThat(service.existsById(UUID.randomUUID())).isFalse();
    }

    // ------------------------------------------------
    // existsAllByIds
    // ------------------------------------------------

    @Test
    @DisplayName("existsAllByIds returns false for invalid collections")
    void existsAllByIds_ShouldReturnFalse_ForInvalidCollections() {

        assertThat(service.existsAllByIds(null)).isFalse();

        assertThat(service.existsAllByIds(Collections.emptyList())).isFalse();

        List<UUID> ids = new ArrayList<>();
        ids.add(null);

        assertThat(service.existsAllByIds(ids)).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("existsAllByIds returns correct existence result")
    void existsAllByIds_ShouldReturnCorrectExistenceResult() {

        Permission first = service.save(buildSinglePermission());

        Permission second = service.save(buildAlternativePermission());

        assertThat(service.existsAllByIds(List.of(first.getId(), second.getId()))).isTrue();

        assertThat(service.existsAllByIds(List.of(first.getId(), UUID.randomUUID()))).isFalse();
    }

    // ------------------------------------------------
    // existsByUniqueProperties
    // ------------------------------------------------

    @Test
    @DisplayName("existsByUniqueProperties returns false when permission is null")
    void existsByUniqueProperties_ShouldReturnFalse_WhenPermissionIsNull() {

        assertThat(service.existsByUniqueProperties(null)).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("existsByUniqueProperties returns correct existence result")
    void existsByUniqueProperties_ShouldReturnCorrectExistenceResult() {

        service.save(buildSinglePermission());

        Permission duplicate = PermissionProvider.singleTemplate();
        duplicate.setCreatedAt(now);

        assertThat(service.existsByUniqueProperties(duplicate)).isTrue();

        Permission nonExisting = PermissionProvider.alternativeTemplate();
        nonExisting.setCreatedAt(now);

        assertThat(service.existsByUniqueProperties(nonExisting)).isFalse();
    }
}
