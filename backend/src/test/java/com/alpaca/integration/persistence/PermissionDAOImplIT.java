package com.alpaca.integration.persistence;

import com.alpaca.entity.Permission;
import com.alpaca.persistence.impl.PermissionDAOImpl;
import com.alpaca.repository.PermissionRepo;
import com.alpaca.resources.utility.DataJpaIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Integration tests for {@link PermissionDAOImpl}. */
@DataJpaIntegrationTest
@Import(PermissionDAOImpl.class)
@DisplayName("PermissionDAOImpl Integration Tests")
class PermissionDAOImplIT {

    @Autowired private PermissionDAOImpl dao;

    @Autowired private PermissionRepo repo;

    private Permission singleEntity;
    private Permission alternativeEntity;
    private Instant now;

    @BeforeEach
    void setup() {
        now = Instant.now();
        singleEntity = new Permission(null, "PERM_SINGLE", Collections.emptySet());
        singleEntity.setCreatedAt(now);
        singleEntity.setCreatedBy(UUID.randomUUID().toString());
        alternativeEntity = new Permission(null, "PERM_ALTERNATIVE", Collections.emptySet());
        alternativeEntity.setCreatedAt(now);
        alternativeEntity.setCreatedBy(UUID.randomUUID().toString());
    }

    @Test
    @DisplayName("findById returns empty Optional when not found and populated when exists")
    @Transactional
    void findById() {
        UUID randomId = UUID.randomUUID();
        Optional<Permission> missing = dao.findById(randomId);
        assertTrue(missing.isEmpty(), "Non-existent id should yield empty optional");

        Permission persisted = dao.save(singleEntity);
        Optional<Permission> found = dao.findById(persisted.getId());
        assertTrue(found.isPresent(), "Saved entity must be found by id");
        assertEquals(persisted, found.get());
    }

    @Test
    @DisplayName("findAllByIds returns only existing entities")
    @Transactional
    void findAllByIds() {
        Permission a = repo.save(singleEntity);
        Permission b = repo.save(alternativeEntity);

        List<Permission> results =
                dao.findAllByIds(Arrays.asList(a.getId(), b.getId(), UUID.randomUUID()));
        assertEquals(2, results.size(), "Should return only the two persisted permissions");
        assertTrue(results.containsAll(Arrays.asList(a, b)));
    }

    @Test
    @DisplayName("save new and saveAll multiple entities")
    @Transactional
    void saveAndSaveAll() {
        // single save
        Permission p = new Permission(null, "NEW_PERMISSION", Collections.emptySet());
        Permission saved = dao.save(p);
        assertNotNull(saved.getId(), "Saved entity must have an id");
        assertEquals("NEW_PERMISSION", saved.getName());

        // saveAll
        Permission x = new Permission(null, "X_PERM", Collections.emptySet());
        Permission y = new Permission(null, "Y_PERM", Collections.emptySet());
        List<Permission> batch = dao.saveAll(Arrays.asList(x, y));
        assertEquals(2, batch.size());
        batch.forEach(item -> assertNotNull(item.getId()));
    }

    @Test
    @DisplayName("deleteById removes entity and existsById reflects absent")
    @Transactional
    void deleteByIdAndExists() {
        Permission toDelete = repo.save(singleEntity);
        UUID id = toDelete.getId();
        assertTrue(dao.existsById(id), "existsById should be true for persisted entity");

        dao.deleteById(id);
        assertFalse(dao.existsById(id), "After deletion, existsById must be false");

        // deleting a non-existing id should not throw (idempotent delete)
        UUID nonExisting = UUID.randomUUID();
        assertThrows(
                EmptyResultDataAccessException.class,
                () -> dao.deleteById(nonExisting),
                "deleteById on absent id should throw");
    }

    @Test
    @DisplayName("findAll and findAllPage return all persisted entities")
    @Transactional
    void findAllAndPage() {
        repo.save(singleEntity);
        repo.save(alternativeEntity);

        List<Permission> all = dao.findAll();
        assertEquals(6, all.size()); // 2 new + 4 by defult

        Page<Permission> page = dao.findAllPage(Pageable.unpaged());
        assertFalse(page.isEmpty());
        assertEquals(all, page.getContent());
    }

    @Test
    @DisplayName("existsAllByIds checks count of existing ids")
    @Transactional
    void existsAllByIds() {
        Permission e1 = repo.save(singleEntity);
        Permission e2 = repo.save(alternativeEntity);

        boolean allExist = dao.existsAllByIds(Arrays.asList(e1.getId(), e2.getId()));
        assertTrue(allExist);

        boolean notAll = dao.existsAllByIds(Arrays.asList(e1.getId(), UUID.randomUUID()));
        assertFalse(notAll);
    }

    @Test
    @DisplayName("existsByUniqueProperties returns false for blank or null and true when exists")
    @Transactional
    void existsByUniqueProperties() {
        Permission blank = new Permission(null, "", Collections.emptySet());
        assertFalse(dao.existsByUniqueProperties(blank));

        Permission nullName = new Permission(null, null, Collections.emptySet());
        assertFalse(dao.existsByUniqueProperties(nullName));

        Permission unsaved = new Permission(null, "UNIQUE_TEST", Collections.emptySet());
        unsaved.setCreatedAt(now);
        unsaved.setCreatedBy(UUID.randomUUID().toString());
        assertFalse(dao.existsByUniqueProperties(unsaved));

        repo.save(unsaved);
        Permission check = new Permission();
        check.setName("UNIQUE_TEST");
        assertTrue(dao.existsByUniqueProperties(check));
    }

    @Test
    @DisplayName("findByPermissionName returns matching entity and handles missing or blank names")
    @Transactional
    void findByPermissionName() {
        Permission saved = repo.save(singleEntity);
        Optional<Permission> opt = dao.findByPermissionName(saved.getName());
        assertTrue(opt.isPresent());
        assertEquals(saved, opt.get());

        Optional<Permission> missing = dao.findByPermissionName("NO_SUCH");
        assertTrue(missing.isEmpty());

        // null and blank input should return empty
        assertTrue(dao.findByPermissionName(null).isEmpty());
        assertTrue(dao.findByPermissionName("   ").isEmpty());
    }

    @Test
    @DisplayName("save with existing id updates entity")
    @Transactional
    void saveWithExistingIdUpdates() {
        Permission saved = repo.save(singleEntity);
        UUID id = saved.getId();

        // change name and save using DAO.save (should perform update)
        saved.setName("SAVED_UPDATED");
        Permission updated = dao.save(saved);
        assertEquals(id, updated.getId());
        assertEquals("SAVED_UPDATED", updated.getName());

        // ensure repository reflects the updated name
        Optional<Permission> reload = repo.findById(id);
        assertTrue(reload.isPresent());
        assertEquals("SAVED_UPDATED", reload.get().getName());
    }

    @Test
    @DisplayName("saveAll with empty list returns empty list and does not fail")
    @Transactional
    void saveAllEmptyListReturnsEmpty() {
        List<Permission> result = dao.saveAll(Collections.emptyList());
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Saving empty collection must return empty list");
    }
}
