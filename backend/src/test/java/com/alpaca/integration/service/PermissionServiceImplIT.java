package com.alpaca.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.alpaca.entity.Permission;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.resources.PermissionProvider;
import com.alpaca.service.impl.PermissionServiceImpl;
import java.time.Instant;
import java.util.*;
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

    // ------------------------------------------------
    // findById
    // ------------------------------------------------

    @Test
    @DisplayName("findById: Throws BadRequestException when ID is null")
    void findById_ShouldThrowBadRequest_WhenIdIsNull() {
        assertThatThrownBy(() -> service.findById(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Permission cannot be found");
    }

    @Test
    @DisplayName("findById: Throws NotFoundException when entity does not exist")
    void findById_ShouldThrowNotFound_WhenEntityMissing() {
        UUID randomId = UUID.randomUUID();
        assertThatThrownBy(() -> service.findById(randomId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Permission with ID " + randomId + " not found");
    }

    @Test
    @DisplayName("findById: Returns entity when found")
    void findById_ShouldReturnEntity_WhenExists() {
        Permission permission = PermissionProvider.singleTemplate();
        permission.setCreatedAt(now);
        Permission saved = service.save(permission);

        Permission found = service.findById(saved.getId());

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(saved.getId());
    }

    // ------------------------------------------------
    // findAllByIds & findAllByIdsToSet
    // ------------------------------------------------

    @Test
    @DisplayName("findAllByIds: Validates null, empty, or list containing null")
    void findAllByIds_ShouldValidateInputs() {
        List<UUID> emptyList = Collections.emptyList();
        assertThatThrownBy(() -> service.findAllByIds(null))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> service.findAllByIds(emptyList))
                .isInstanceOf(BadRequestException.class);

        List<UUID> listWithNull = new ArrayList<>();
        listWithNull.add(UUID.randomUUID());
        listWithNull.add(null);
        assertThatThrownBy(() -> service.findAllByIds(listWithNull))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("findAllByIds: Throws NotFoundException if any ID in the collection is missing")
    void findAllByIds_ShouldThrowNotFound_WhenSomeIdsMissing() {
        Permission p = PermissionProvider.singleTemplate();
        p.setCreatedAt(now);
        Permission saved = service.save(p);

        List<UUID> ids = List.of(saved.getId(), UUID.randomUUID());
        assertThatThrownBy(() -> service.findAllByIds(ids)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("findAllByIdsToSet: Returns a unique set of entities")
    void findAllByIdsToSet_ShouldReturnUniqueSet() {
        Permission p = PermissionProvider.singleTemplate();
        p.setCreatedAt(now);
        Permission saved = service.save(p);

        Set<Permission> result = service.findAllByIdsToSet(List.of(saved.getId()));

        assertThat(result).hasSize(1).contains(saved);
    }

    // ------------------------------------------------
    // save & saveAll
    // ------------------------------------------------

    @Test
    @DisplayName("save: Validates null and duplicate unique properties")
    void save_ShouldValidateInput() {
        assertThatThrownBy(() -> service.save(null)).isInstanceOf(BadRequestException.class);

        Permission p = PermissionProvider.singleTemplate();
        p.setCreatedAt(now);
        service.save(p);

        // Attempting to save same unique properties again
        assertThatThrownBy(() -> service.save(p))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("saveAll: Validates collection constraints and persists list")
    void saveAll_ShouldValidateAndPersist() {
        assertThatThrownBy(() -> service.saveAll(null)).isInstanceOf(BadRequestException.class);

        Permission p1 = PermissionProvider.singleTemplate();
        Permission p2 = PermissionProvider.alternativeTemplate();
        p1.setCreatedAt(now);
        p2.setCreatedAt(now);

        List<Permission> saved = service.saveAll(List.of(p1, p2));
        assertThat(saved).hasSize(2);
    }

    // ------------------------------------------------
    // updateById
    // ------------------------------------------------

    @Test
    @DisplayName("updateById: Throws exception on null inputs or missing entity")
    void updateById_ShouldValidateInputs() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> service.updateById(null, id))
                .isInstanceOf(BadRequestException.class);

        Permission p = PermissionProvider.singleTemplate();
        assertThatThrownBy(() -> service.updateById(p, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("updateById: Persists changes to existing entity")
    void updateById_ShouldUpdateExistingEntity() {
        Permission original = PermissionProvider.singleTemplate();
        original.setCreatedAt(now);
        Permission saved = service.save(original);

        Permission updateData = PermissionProvider.alternativeTemplate();
        // ID is passed separately in updateById signature
        Permission updated = service.updateById(updateData, saved.getId());

        assertThat(updated.getId()).isEqualTo(saved.getId());
        assertThat(updated.getName()).isEqualTo(updateData.getName());
    }

    // ------------------------------------------------
    // deleteById
    // ------------------------------------------------

    @Test
    @DisplayName("deleteById: Validates null and existence before deletion")
    void deleteById_ShouldValidate() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> service.deleteById(null)).isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> service.deleteById(id))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not exists");
    }

    @Test
    @DisplayName("deleteById: Removes entity from database")
    void deleteById_ShouldRemoveEntity() {
        Permission p = PermissionProvider.singleTemplate();
        p.setCreatedAt(now);
        Permission saved = service.save(p);

        service.deleteById(saved.getId());

        assertThat(service.existsById(saved.getId())).isFalse();
    }

    // ------------------------------------------------
    // Pagination & Existence Checks
    // ------------------------------------------------

    @Test
    @DisplayName("findAllPage: Returns paginated results")
    void findAllPage_ShouldReturnPage() {
        Permission p = PermissionProvider.singleTemplate();
        p.setCreatedAt(now);
        service.save(p);

        Page<Permission> page = service.findAllPage(Pageable.ofSize(10));

        assertThat(page).isNotNull();
        assertThat(page.getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("existsAllByIds: Returns false for invalid input collections")
    void existsAllByIds_ShouldHandleEdgeCases() {
        assertThat(service.existsAllByIds(null)).isFalse();
        assertThat(service.existsAllByIds(Collections.emptyList())).isFalse();

        List<UUID> listWithNull = new ArrayList<>();
        listWithNull.add(null);
        assertThat(service.existsAllByIds(listWithNull)).isFalse();
    }

    @Test
    @DisplayName("existsByUniqueProperties: Handles null gracefully")
    void existsByUniqueProperties_ShouldHandleNull() {
        assertThat(service.existsByUniqueProperties(null)).isFalse();
    }
}
