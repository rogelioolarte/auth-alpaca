package com.alpaca.unit.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.alpaca.entity.Permission;
import com.alpaca.entity.Role;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.impl.RoleDAOImpl;
import com.alpaca.repository.RoleRepo;
import com.alpaca.resources.PermissionProvider;
import com.alpaca.resources.RoleProvider;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link RoleDAOImpl}. */
@ExtendWith(MockitoExtension.class)
class RoleDAOImplTest {

    @Mock private RoleRepo repo;

    @InjectMocks private RoleDAOImpl dao;

    private Role firstEntity;
    private final UUID TEST_ID = UUID.fromString("e87ce3ba-fe71-4cf1-b302-94446a3684ca");

    @BeforeEach
    void setup() {
        firstEntity = RoleProvider.singleEntity();
    }

    @Nested
    @DisplayName("findByRoleName Logic")
    class FindByRoleNameTests {

        @Test
        @DisplayName("Should return empty for all invalid role name branches")
        void findByRoleName_Branches() {
            assertThat(dao.findByRoleName(null)).isEmpty(); // null branch
            assertThat(dao.findByRoleName("")).isEmpty(); // empty branch
            assertThat(dao.findByRoleName("   ")).isEmpty(); // blank branch
            verifyNoInteractions(repo);
        }

        @Test
        @DisplayName("Should call repository when role name is valid")
        void findByRoleName_Valid() {
            String name = "USER";
            when(repo.findByName(name)).thenReturn(Optional.of(firstEntity));
            assertThat(dao.findByRoleName(name)).isPresent();
        }
    }

    @Nested
    @DisplayName("updateById Logic")
    class UpdateByIdTests {

        @Test
        @DisplayName("Should throw NotFoundException and cover error message logic")
        void updateById_NotFound() {
            UUID id = UUID.randomUUID();
            when(repo.findById(id)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> dao.updateById(new Role(), id));
        }

        @Nested
        @DisplayName("updateById Branch Isolation")
        class UpdateByIdTestsBranches {

            @Test
            @DisplayName("Branch 1: rolePermissions is null -> Should skip assignment")
            void updateById_PermissionsNull() {
                Role existing = new Role();
                existing.setRolePermissions(new HashSet<>());

                Role incoming = new Role();
                incoming.setRolePermissions(null); // Gate 1 fails (A is false)

                when(repo.findById(TEST_ID)).thenReturn(Optional.of(existing));
                when(repo.save(any(Role.class))).thenAnswer(i -> i.getArguments()[0]);

                dao.updateById(incoming, TEST_ID);

                assertThat(existing.getRolePermissions()).isEmpty();
                verify(repo).save(existing);
            }

            @Test
            @DisplayName(
                    "Branch 2: rolePermissions is not null but empty -> Should skip assignment")
            void updateById_PermissionsEmpty() {
                Role existing = new Role();
                existing.setRolePermissions(new HashSet<>());

                Role incoming = new Role();
                incoming.setRolePermissions(
                        new HashSet<>()); // Gate 1 true, Gate 2 fails (B is false)

                when(repo.findById(TEST_ID)).thenReturn(Optional.of(existing));
                when(repo.save(any(Role.class))).thenAnswer(i -> i.getArguments()[0]);

                dao.updateById(incoming, TEST_ID);

                assertThat(existing.getRolePermissions()).isEmpty();
                verify(repo).save(existing);
            }

            @Test
            @DisplayName("Branch 3: rolePermissions has data -> Should execute assignment")
            void updateById_PermissionsSuccess() {
                Role existing = new Role();
                existing.setRolePermissions(new HashSet<>());

                Permission p = PermissionProvider.singleEntity();
                Role incoming = new Role();
                incoming.setRolePermissions(new HashSet<>(Set.of(p))); // Gate 1 true, Gate 2 true

                when(repo.findById(TEST_ID)).thenReturn(Optional.of(existing));
                when(repo.save(any(Role.class))).thenAnswer(i -> i.getArguments()[0]);

                dao.updateById(incoming, TEST_ID);

                assertThat(existing.getRolePermissions()).hasSize(1);
                verify(repo).save(existing);
            }
        }
    }

    @Nested
    @DisplayName("existsByUniqueProperties Logic")
    class ExistsByUniquePropertiesTests {

        @Test
        @DisplayName("Should cover every single logical branch of the validation IF")
        void existsByUniqueProperties_FullBranchCoverage() {
            Role role = new Role();

            // 1. Name is null
            role.setName(null);
            role.setDescription("Valid");
            assertThat(dao.existsByUniqueProperties(role)).isFalse();

            // 2. Name is blank (Empty/Spaces)
            role.setName("   ");
            assertThat(dao.existsByUniqueProperties(role)).isFalse();

            // 3. Description is null
            role.setName("Valid");
            role.setDescription(null);
            assertThat(dao.existsByUniqueProperties(role)).isFalse();

            // 4. Description is blank
            role.setDescription("  ");
            assertThat(dao.existsByUniqueProperties(role)).isFalse();

            verifyNoInteractions(repo);
        }

        @Test
        @DisplayName("Should call repository when all unique properties are present")
        void existsByUniqueProperties_Valid() {
            Role validRole = new Role();
            validRole.setName("ADMIN");
            validRole.setDescription("Administrator");

            when(repo.existsByName("ADMIN")).thenReturn(true);
            assertThat(dao.existsByUniqueProperties(validRole)).isTrue();
        }
    }

    @Test
    @DisplayName("existsAllByIds: Should compare input size with repository count")
    void existsAllByIds_Coverage() {
        List<UUID> ids = RoleProvider.listEntities().stream().map(Role::getId).toList();
        when(repo.countByIds(ids)).thenReturn((long) ids.size());
        assertThat(dao.existsAllByIds(ids)).isTrue();

        when(repo.countByIds(ids)).thenReturn(0L);
        assertThat(dao.existsAllByIds(ids)).isFalse();
    }
}
