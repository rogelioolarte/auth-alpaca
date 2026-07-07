package com.alpaca.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.alpaca.entity.Permission;
import com.alpaca.entity.Role;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IPermissionDAO;
import com.alpaca.resources.provider.PermissionProvider;
import com.alpaca.resources.provider.RoleProvider;
import com.alpaca.resources.utility.BaseIntegrationTests;
import com.alpaca.service.impl.RoleServiceImpl;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link RoleServiceImpl} */
@DisplayName("RoleServiceImpl Integration Tests")
class RoleServiceImplIT extends BaseIntegrationTests {

    @Autowired private RoleServiceImpl service;
    @Autowired private IPermissionDAO permissionDAO;

    private Instant now;

    @BeforeEach
    void setup() {
        now = Instant.now();
    }

    private Role buildSingleRole() {
        Role role = RoleProvider.singleTemplate();
        role.setCreatedAt(now);

        return role;
    }

    private Role buildAlternativeRole() {
        Role role = RoleProvider.alternativeTemplate();
        role.setCreatedAt(now);

        return role;
    }

    private Permission buildPermission() {
        Permission permission = PermissionProvider.singleTemplate();
        permission.setCreatedAt(now);

        return permission;
    }

    // ------------------------------------------------
    // getUserRoles
    // ------------------------------------------------

    @Test
    @Transactional
    @DisplayName("getUserRoles returns TEST_USER role")
    void getUserRoles_ShouldReturnUserRole() {
        Set<Role> result = service.getUserRoles();

        assertThat(result).hasSize(1);

        Role found = result.iterator().next();

        assertThat(found.getName()).isEqualTo("USER");
    }

    // ------------------------------------------------
    // findByRoleName
    // ------------------------------------------------

    @Test
    @Transactional
    @DisplayName("findByRoleName validates invalid role names")
    void findByRoleName_ShouldValidateInvalidRoleNames() {

        assertThatThrownBy(() -> service.findByRoleName(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Role cannot be found");

        assertThatThrownBy(() -> service.findByRoleName(" "))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Role cannot be found");
    }

    @Test
    @Transactional
    @DisplayName("findByRoleName throws NotFoundException when role does not exist")
    void findByRoleName_ShouldThrowNotFound_WhenRoleDoesNotExist() {

        String roleName = "SUPER_ADMIN";

        assertThatThrownBy(() -> service.findByRoleName(roleName))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Role with Name " + roleName + " not found");
    }

    @Test
    @Transactional
    @DisplayName("findByRoleName returns matching role")
    void findByRoleName_ShouldReturnMatchingRole() {

        Role saved = service.save(buildSingleRole());

        Role result = service.findByRoleName(saved.getName());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(saved.getId());
        assertThat(result.getName()).isEqualTo(saved.getName());
    }

    // ------------------------------------------------
    // save
    // ------------------------------------------------

    @Test
    @Transactional
    @DisplayName("save throws BadRequestException when role is null")
    void save_ShouldThrowBadRequest_WhenRoleIsNull() {

        assertThatThrownBy(() -> service.save(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Role cannot be created");
    }

    @Test
    @Transactional
    @DisplayName("save throws BadRequestException when role already exists")
    void save_ShouldThrowBadRequest_WhenRoleAlreadyExists() {

        Role role = buildSingleRole();

        service.save(role);

        Role duplicate = RoleProvider.singleTemplate();
        duplicate.setCreatedAt(now);

        assertThatThrownBy(() -> service.save(duplicate))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Role already exists");
    }

    @Test
    @Transactional
    @DisplayName("save persists role successfully")
    void save_ShouldPersistRoleSuccessfully() {

        Role result = service.save(buildSingleRole());

        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isNotBlank();
    }

    // ------------------------------------------------
    // saveAll
    // ------------------------------------------------

    @Test
    @Transactional
    @DisplayName("saveAll validates invalid collections")
    void saveAll_ShouldValidateInvalidCollections() {

        assertThatThrownBy(() -> service.saveAll(null)).isInstanceOf(BadRequestException.class);

        Throwable thrown = catchThrowable(() -> service.saveAll(Collections.emptyList()));
        assertThat(thrown).as("Role(s) cannot be created").isInstanceOf(BadRequestException.class);

        List<Role> roles = new ArrayList<>();
        roles.add(buildSingleRole());
        roles.add(null);

        assertThatThrownBy(() -> service.saveAll(roles)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @Transactional
    @DisplayName("saveAll persists all roles")
    void saveAll_ShouldPersistAllRoles() {

        Role first = buildSingleRole();

        Role second = buildAlternativeRole();

        List<Role> result = service.saveAll(List.of(first, second));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Role::getId).doesNotContainNull();
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
                .hasMessageContaining("Role cannot be found");
    }

    @Test
    @Transactional
    @DisplayName("findById throws NotFoundException when role does not exist")
    void findById_ShouldThrowNotFound_WhenRoleDoesNotExist() {

        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Role with ID " + id + " not found");
    }

    @Test
    @Transactional
    @DisplayName("findById returns existing role")
    void findById_ShouldReturnExistingRole() {

        Role saved = service.save(buildSingleRole());

        Role result = service.findById(saved.getId());

        assertThat(result.getId()).isEqualTo(saved.getId());
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
        assertThat(thrown).as("Role(s) cannot be found").isInstanceOf(BadRequestException.class);

        List<UUID> ids = new ArrayList<>();
        ids.add(UUID.randomUUID());
        ids.add(null);

        assertThatThrownBy(() -> service.findAllByIds(ids)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @Transactional
    @DisplayName("findAllByIds throws NotFoundException when some ids are missing")
    void findAllByIds_ShouldThrowNotFound_WhenSomeIdsAreMissing() {

        Role saved = service.save(buildSingleRole());

        Throwable thrown =
                catchThrowable(
                        () -> service.findAllByIds(List.of(saved.getId(), UUID.randomUUID())));
        assertThat(thrown).as("Some Role(s) cannot be found").isInstanceOf(NotFoundException.class);
    }

    @Test
    @Transactional
    @DisplayName("findAllByIds returns all matching roles")
    void findAllByIds_ShouldReturnAllMatchingRoles() {

        Role first = service.save(buildSingleRole());

        Role second = service.save(buildAlternativeRole());

        List<Role> result = service.findAllByIds(List.of(first.getId(), second.getId()));

        assertThat(result).hasSize(2);
    }

    // ------------------------------------------------
    // updateById
    // ------------------------------------------------

    @Test
    @Transactional
    @DisplayName("updateById validates invalid inputs")
    void updateById_ShouldValidateInvalidInputs() {

        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.updateById(null, id))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Role with ID " + id + " cannot be updated");

        Throwable thrown = catchThrowable(() -> service.updateById(buildSingleRole(), null));
        assertThat(thrown)
                .as("Role with ID null cannot be updated")
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @Transactional
    @DisplayName("updateById throws NotFoundException when role does not exist")
    void updateById_ShouldThrowNotFound_WhenRoleDoesNotExist() {

        Role update = buildAlternativeRole();

        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.updateById(update, id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Role with ID " + id + " not found");
    }

    @Test
    @Transactional
    @DisplayName("updateById updates role fields successfully")
    void updateById_ShouldUpdateRoleFieldsSuccessfully() {

        Role saved = service.save(buildSingleRole());

        Role update = buildAlternativeRole();

        Role result = service.updateById(update, saved.getId());

        assertThat(result.getName()).isEqualTo(update.getName());
        assertThat(result.getDescription()).isEqualTo(update.getDescription());
    }

    @Test
    @Transactional
    @DisplayName("updateById does not update blank values")
    void updateById_ShouldNotUpdateBlankValues() {

        Role saved = service.save(buildSingleRole());

        String originalName = saved.getName();
        String originalDescription = saved.getDescription();

        Role update = buildAlternativeRole();
        update.setName(" ");
        update.setDescription(" ");

        Role result = service.updateById(update, saved.getId());

        assertThat(result.getName()).isEqualTo(originalName);
        assertThat(result.getDescription()).isEqualTo(originalDescription);
    }

    @Test
    @Transactional
    @DisplayName("updateById does not update identical values")
    void updateById_ShouldNotUpdateIdenticalValues() {

        Role saved = service.save(buildSingleRole());

        Role update = buildAlternativeRole();
        update.setName(saved.getName());
        update.setDescription(saved.getDescription());

        Role result = service.updateById(update, saved.getId());

        assertThat(result.getName()).isEqualTo(saved.getName());
        assertThat(result.getDescription()).isEqualTo(saved.getDescription());
    }

    @Test
    @Transactional
    @DisplayName("updateById updates permissions when role permissions differ")
    void updateById_ShouldUpdatePermissions_WhenPermissionsDiffer() {

        Role saved = service.save(buildSingleRole());

        Permission permission = permissionDAO.save(buildPermission());

        Role update = buildAlternativeRole();
        update.setRolePermissions(new HashSet<>(Set.of(permission)));

        Role result = service.updateById(update, saved.getId());

        assertThat(result.getPermissions()).isEqualTo(update.getPermissions());
    }

    // ------------------------------------------------
    // deleteById
    // ------------------------------------------------

    @Test
    @Transactional
    @DisplayName("deleteById validates invalid id")
    void deleteById_ShouldValidateInvalidIds() {
        assertThatThrownBy(() -> service.deleteById(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Role cannot be deleted");
    }

    @Test
    @Transactional
    @DisplayName("deleteById removes role successfully")
    void deleteById_ShouldRemoveRoleSuccessfully() {

        Role saved = service.save(buildSingleRole());

        service.deleteById(saved.getId());

        assertThat(service.existsById(saved.getId())).isFalse();
    }

    // ------------------------------------------------
    // findAll
    // ------------------------------------------------

    @Test
    @Transactional
    @DisplayName("findAll returns persisted roles")
    void findAll_ShouldReturnPersistedRoles() {

        service.save(buildSingleRole());

        service.save(buildAlternativeRole());

        List<Role> result = service.findAll();

        assertAll(
                () -> assertThat(result).isNotEmpty(),
                () -> assertThat(result).hasSizeGreaterThanOrEqualTo(2));
    }

    // ------------------------------------------------
    // findAllPage
    // ------------------------------------------------

    @Test
    @Transactional
    @DisplayName("findAllPage validates null pageable")
    void findAllPage_ShouldValidateNullPageable() {

        assertThatThrownBy(() -> service.findAllPage(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Role(s) Page cannot be created");
    }

    @Test
    @Transactional
    @DisplayName("findAllPage returns paginated roles")
    void findAllPage_ShouldReturnPaginatedRoles() {

        service.save(buildSingleRole());

        Page<Role> result = service.findAllPage(Pageable.ofSize(10));

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotEmpty();
    }

    // ------------------------------------------------
    // existsById
    // ------------------------------------------------

    @Test
    @Transactional
    @DisplayName("existsById returns false for null id")
    void existsById_ShouldReturnFalse_ForNullId() {

        assertThat(service.existsById(null)).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("existsById returns correct existence result")
    void existsById_ShouldReturnCorrectExistenceResult() {

        Role saved = service.save(buildSingleRole());

        assertThat(service.existsById(saved.getId())).isTrue();

        assertThat(service.existsById(UUID.randomUUID())).isFalse();
    }

    // ------------------------------------------------
    // existsAllByIds
    // ------------------------------------------------

    @Test
    @Transactional
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

        Role first = service.save(buildSingleRole());

        Role second = service.save(buildAlternativeRole());

        assertThat(service.existsAllByIds(List.of(first.getId(), second.getId()))).isTrue();

        assertThat(service.existsAllByIds(List.of(first.getId(), UUID.randomUUID()))).isFalse();
    }

    // ------------------------------------------------
    // existsByUniqueProperties
    // ------------------------------------------------

    @Test
    @Transactional
    @DisplayName("existsByUniqueProperties returns false when role is null")
    void existsByUniqueProperties_ShouldReturnFalse_WhenRoleIsNull() {

        assertThat(service.existsByUniqueProperties(null)).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("existsByUniqueProperties returns correct existence result")
    void existsByUniqueProperties_ShouldReturnCorrectExistenceResult() {

        service.save(buildSingleRole());

        Role duplicate = RoleProvider.singleTemplate();
        duplicate.setCreatedAt(now);

        assertThat(service.existsByUniqueProperties(duplicate)).isTrue();

        Role nonExisting = RoleProvider.alternativeTemplate();
        nonExisting.setCreatedAt(now);

        assertThat(service.existsByUniqueProperties(nonExisting)).isFalse();
    }
}
