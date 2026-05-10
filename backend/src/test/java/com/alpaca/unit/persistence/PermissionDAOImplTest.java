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
        @DisplayName("findById: Should return entity when ID matches")
        void findById_Success() {
            when(repo.findById(firstEntity.getId())).thenReturn(Optional.of(firstEntity));
            Optional<Permission> result = dao.findById(firstEntity.getId());
            assertThat(result).isPresent().contains(firstEntity);
        }

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
        @DisplayName("save: Should persist and return the entity")
        void save_Success() {
            when(repo.save(firstEntity)).thenReturn(firstEntity);
            assertThat(dao.save(firstEntity)).isEqualTo(firstEntity);
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
        @DisplayName("updateIfNotNull: Should handle all scenarios for generic updates")
        void updateIfNotNull_Coverage() {
            AtomicReference<String> target = new AtomicReference<>("Initial");

            dao.updateIfNotNull("Initial", null, target::set);
            assertThat(target.get()).isEqualTo("Initial");

            dao.updateIfNotNull("Initial", "Initial", target::set);
            assertThat(target.get()).isEqualTo("Initial");

            dao.updateIfNotNull("Initial", "New", target::set);
            assertThat(target.get()).isEqualTo("New");
        }

        @Test
        @DisplayName("updateTextIfExists: Should handle text-specific update logic")
        void updateTextIfExists_Coverage() {
            AtomicReference<String> target = new AtomicReference<>("Old");

            dao.updateTextIfExists("Old", "", target::set);
            assertThat(target.get()).isEqualTo("Old");

            dao.updateTextIfExists("Old", "Old", target::set);
            assertThat(target.get()).isEqualTo("Old");

            dao.updateTextIfExists("Old", "Updated", target::set);
            assertThat(target.get()).isEqualTo("Updated");
        }

        @Test
        @DisplayName("updateIfDifferent: Should handle all Boolean cases")
        void updateIfDifferent_Coverage() {
            AtomicReference<Boolean> target = new AtomicReference<>(true);

            dao.updateIfDifferent(true, true, target::set);
            assertThat(target.get()).isTrue();

            dao.updateIfDifferent(true, false, target::set);
            assertThat(target.get()).isFalse();
        }

        @Test
        @DisplayName("deleteById: Should call repository delete method")
        void deleteById_FullCoverage() {
            dao.deleteById(firstEntity.getId());
            verify(repo, times(1)).deleteById(firstEntity.getId());
        }

        @Test
        @DisplayName("findAllPage: Should handle paginated requests correctly")
        void findAllPage_FullCoverage() {
            Pageable pageable = Pageable.ofSize(5).withPage(0);
            Page<Permission> expectedPage = new PageImpl<>(entities, pageable, entities.size());

            when(repo.findAll(pageable)).thenReturn(expectedPage);

            Page<Permission> result = dao.findAllPage(pageable);

            assertThat(result).isNotNull();
            verify(repo).findAll(pageable);
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
                when(repo.findById(firstEntity.getId())).thenReturn(Optional.empty());

                assertThrows(
                        NotFoundException.class,
                        () -> dao.updateById(firstEntity, firstEntity.getId()));
                verify(repo, never()).save(any());
            }

            @Test
            @DisplayName("Should update name when it is valid and different")
            void updateById_SuccessfulUpdate() {
                Permission existing = new Permission();
                existing.setId(firstEntity.getId());
                existing.setName("OLD_NAME");

                when(repo.findById(firstEntity.getId())).thenReturn(Optional.of(existing));
                when(repo.save(existing)).thenReturn(existing);

                Permission result = dao.updateById(firstEntity, firstEntity.getId());

                assertThat(result.getName()).isEqualTo(firstEntity.getName());
                verify(repo).save(existing);
            }
        }

        @Nested
        @DisplayName("existsByUniqueProperties Logic")
        class ExistsByUniquePropertiesTests {

            @Test
            @DisplayName("Should return false for invalid name inputs")
            void existsByUniqueProperties_InvalidInputs() {
                Permission p = new Permission();

                p.setName(null);
                assertThat(dao.existsByUniqueProperties(p)).isFalse();

                p.setName("   ");
                assertThat(dao.existsByUniqueProperties(p)).isFalse();

                verifyNoInteractions(repo);
            }

            @Test
            @DisplayName("Should return repository response for valid name")
            void existsByUniqueProperties_ValidName() {
                when(repo.existsByName(firstEntity.getName())).thenReturn(true);
                assertThat(dao.existsByUniqueProperties(firstEntity)).isTrue();
            }
        }

        @Test
        @DisplayName("findByPermissionName: Should return expected optional")
        void findByPermissionName_Logic() {
            when(repo.findByName(firstEntity.getName())).thenReturn(Optional.of(firstEntity));
            assertThat(dao.findByPermissionName(firstEntity.getName()))
                    .isPresent()
                    .contains(firstEntity);
        }

        @Test
        @DisplayName("existsAllByIds: Should compare input size with repository count")
        void existsAllByIds_Coverage() {
            when(repo.countByIds(ids)).thenReturn((long) ids.size());
            assertThat(dao.existsAllByIds(ids)).isTrue();

            when(repo.countByIds(ids)).thenReturn(0L);
            assertThat(dao.existsAllByIds(ids)).isFalse();
        }
    }
}
