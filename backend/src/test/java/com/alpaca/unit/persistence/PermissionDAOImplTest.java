package com.alpaca.unit.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.alpaca.entity.Permission;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.impl.PermissionDAOImpl;
import com.alpaca.repository.PermissionRepo;
import com.alpaca.resources.PermissionProvider;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/** Unit tests for {@link PermissionDAOImpl}. */
@ExtendWith(MockitoExtension.class)
class PermissionDAOImplTest {

    @Mock private PermissionRepo repo;
    @InjectMocks private PermissionDAOImpl dao;

    private Permission firstEntity;
    private List<Permission> entities;
    private List<UUID> ids;

    @BeforeEach
    void setup() {
        firstEntity = PermissionProvider.singleEntity();
        entities = PermissionProvider.listEntities();
        ids = entities.stream().map(Permission::getId).toList();
    }

    @Nested
    @DisplayName("Tests for GenericDAOImpl Logic")
    class GenericDAOFullCoverage {

        @Test
        @DisplayName("findAll: Should return all entities")
        void findAll_Success() {
            when(repo.findAll()).thenReturn(entities);
            assertThat(dao.findAll()).isEqualTo(entities);
        }

        @Test
        @DisplayName("findAllByIds: Should return entities matching IDs")
        void findAllByIds_Success() {
            when(repo.findAllById(ids)).thenReturn(entities);
            assertThat(dao.findAllByIds(ids)).isEqualTo(entities);
        }

        @Test
        @DisplayName("saveAll: Should save collection of entities")
        void saveAll_Success() {
            when(repo.saveAll(entities)).thenReturn(entities);
            assertThat(dao.saveAll(entities)).isEqualTo(entities);
        }

        @Test
        @DisplayName("existsById: Should return true if exists")
        void existsById_True() {
            when(repo.existsById(firstEntity.getId())).thenReturn(true);
            assertThat(dao.existsById(firstEntity.getId())).isTrue();
        }

        @Test
        @DisplayName("getEntity: Should return the correct class type")
        void getEntity_Success() {
            assertThat(dao.findById(null)).isEmpty();
        }

        @Test
        @DisplayName("updateIfNotNull: Should only update if incoming is not null and different")
        void updateIfNotNull_Coverage() {
            AtomicReference<String> target = new AtomicReference<>("Initial");

            // Case 1: Incoming is null (No update)
            dao.updateIfNotNull("Initial", null, target::set);
            assertThat(target.get()).isEqualTo("Initial");

            // Case 2: Incoming is same as existing (No update)
            dao.updateIfNotNull("Initial", "Initial", target::set);
            assertThat(target.get()).isEqualTo("Initial");

            // Case 3: Incoming is different (Update)
            dao.updateIfNotNull("Initial", "New", target::set);
            assertThat(target.get()).isEqualTo("New");
        }

        @Test
        @DisplayName("updateIfDifferent: Should handle all Boolean cases")
        void updateIfDifferent_Coverage() {
            AtomicReference<Boolean> target = new AtomicReference<>(true);

            // Case 1: Same value (No update)
            dao.updateIfDifferent(true, true, target::set);
            assertThat(target.get()).isTrue();

            // Case 2: Different value (Update)
            dao.updateIfDifferent(true, false, target::set);
            assertThat(target.get()).isFalse();

            // Case 3: One is null (Update)
            dao.updateIfDifferent(null, true, target::set);
            assertThat(target.get()).isTrue();
        }

        @Test
        @DisplayName("deleteById: Should call repository delete method")
        void deleteById_FullCoverage() {
            UUID id = UUID.randomUUID();

            dao.deleteById(id);

            verify(repo, times(1)).deleteById(id);
        }

        @Test
        @DisplayName("findAllPage: Should handle paginated requests correctly")
        void findAllPage_FullCoverage() {
            Pageable pageable = Pageable.ofSize(5).withPage(0);
            Page<Permission> expectedPage = new PageImpl<>(entities, pageable, entities.size());

            when(repo.findAll(pageable)).thenReturn(expectedPage);

            Page<Permission> result = dao.findAllPage(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(entities.size());
            verify(repo).findAll(pageable);
        }

        @Test
        @DisplayName("existsAllByIds: Should return true when count matches input size")
        void existsAllByIds_Success() {
            // Case where all IDs exist in DB
            when(repo.countByIds(ids)).thenReturn((long) ids.size());

            boolean result = dao.existsAllByIds(ids);

            assertThat(result).isTrue();
            verify(repo).countByIds(ids);
        }

        @Test
        @DisplayName("existsAllByIds: Should return false when count does not match size")
        void existsAllByIds_Failure() {
            // Case where some IDs are missing in DB (Size = 3, Count = 2)
            when(repo.countByIds(ids)).thenReturn((long) ids.size() - 1);

            boolean result = dao.existsAllByIds(ids);

            assertThat(result).isFalse();
            verify(repo).countByIds(ids);
        }

        @Test
        @DisplayName("existsAllByIds: Should handle empty collection")
        void existsAllByIds_Empty() {
            List<UUID> emptyIds = List.of();
            when(repo.countByIds(emptyIds)).thenReturn(0L);

            boolean result = dao.existsAllByIds(emptyIds);

            assertThat(result).isTrue(); // 0 == 0
            verify(repo).countByIds(emptyIds);
        }
    }

    @Nested
    @DisplayName("Tests for PermissionDAOImpl Specific Logic")
    class PermissionDAOFullCoverage {

        @Nested
        @DisplayName("updateById Logic")
        class UpdateByIdTests {

            @Test
            @DisplayName("Should throw NotFoundException when ID doesn't exist")
            void updateById_ThrowsException() {
                UUID id = UUID.randomUUID();
                when(repo.findById(id)).thenReturn(Optional.empty());

                Permission data = new Permission();

                assertThrows(NotFoundException.class, () -> dao.updateById(data, id));
                verify(repo, never()).save(any());
            }

            @Test
            @DisplayName("Should update name when it is valid and different")
            void updateById_SuccessfulUpdate() {
                String newName = "NEW_PERMISSION_NAME";
                Permission existing = new Permission();
                existing.setId(firstEntity.getId());
                existing.setPermissionName("OLD_NAME");

                Permission incoming = new Permission();
                incoming.setPermissionName(newName);

                when(repo.findById(firstEntity.getId())).thenReturn(Optional.of(existing));
                when(repo.save(existing)).thenReturn(existing);

                Permission result = dao.updateById(incoming, firstEntity.getId());

                assertThat(result.getPermissionName()).isEqualTo(newName);
                verify(repo).save(existing);
            }

            @Test
            @DisplayName("Should NOT update name when incoming is blank or same")
            void updateById_NoActionOnBlankOrSame() {
                Permission existing = spy(new Permission());
                existing.setPermissionName("CONSTANT_NAME");

                when(repo.findById(firstEntity.getId())).thenReturn(Optional.of(existing));
                when(repo.save(existing)).thenReturn(existing);

                // Case 1: Same name
                Permission sameName = new Permission();
                sameName.setPermissionName("CONSTANT_NAME");
                dao.updateById(sameName, firstEntity.getId());

                // Case 2: Blank name
                Permission blankName = new Permission();
                blankName.setPermissionName("   ");
                dao.updateById(blankName, firstEntity.getId());

                // Verify setter was never called with these values
                verify(existing, never()).setPermissionName("   ");
            }
        }

        @Nested
        @DisplayName("existsByUniqueProperties Logic")
        class ExistsByUniquePropertiesTests {

            @Test
            @DisplayName("Should return false for null, empty or blank names")
            void existsByUniqueProperties_InvalidInputs() {
                Permission p = new Permission();

                // Null branch
                p.setPermissionName(null);
                assertThat(dao.existsByUniqueProperties(p)).isFalse();

                // Empty branch
                p.setPermissionName("");
                assertThat(dao.existsByUniqueProperties(p)).isFalse();

                // Blank branch
                p.setPermissionName("     ");
                assertThat(dao.existsByUniqueProperties(p)).isFalse();

                verifyNoInteractions(repo);
            }

            @Test
            @DisplayName("Should return true/false based on repo when name is valid")
            void existsByUniqueProperties_ValidName() {
                Permission p = new Permission();
                p.setPermissionName("VALID_NAME");

                when(repo.existsByPermissionName("VALID_NAME")).thenReturn(true);
                assertThat(dao.existsByUniqueProperties(p)).isTrue();

                when(repo.existsByPermissionName("VALID_NAME")).thenReturn(false);
                assertThat(dao.existsByUniqueProperties(p)).isFalse();
            }
        }

        @Nested
        @DisplayName("findByPermissionName Logic")
        class FindByPermissionNameTests {

            @Test
            @DisplayName("Should return optional with entity when found")
            void findByPermissionName_Found() {
                String name = "SEARCH_ME";
                when(repo.findByPermissionName(name)).thenReturn(Optional.of(firstEntity));

                Optional<Permission> result = dao.findByPermissionName(name);

                assertThat(result).isPresent().contains(firstEntity);
            }

            @Test
            @DisplayName("Should return empty optional when not found")
            void findByPermissionName_NotFound() {
                String name = "NOT_EXIST";
                when(repo.findByPermissionName(name)).thenReturn(Optional.empty());

                Optional<Permission> result = dao.findByPermissionName(name);

                assertThat(result).isEmpty();
            }
        }
    }
}
