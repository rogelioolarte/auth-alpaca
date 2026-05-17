package com.alpaca.unit.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.alpaca.entity.Role;
import com.alpaca.persistence.impl.RoleDAOImpl;
import com.alpaca.repository.RoleRepo;
import com.alpaca.resources.RoleProvider;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
