package com.alpaca.integration.persistence;

import com.alpaca.entity.Permission;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.impl.PermissionDAOImpl;
import com.alpaca.repository.PermissionRepo;
import com.alpaca.resources.PermissionProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Integration tests for {@link com.alpaca.persistence.impl.PermissionDAOImpl} */
@SpringBootTest
@ExtendWith(SpringExtension.class)
class PermissionDAOImplIT {

    @Autowired private PermissionDAOImpl dao;

    @Autowired private PermissionRepo repo;

    private Permission singleEntity;
    private Permission alternativeEntity;

    @BeforeEach
    void setup() {
        singleEntity = new Permission(PermissionProvider.singleEntity().getPermissionName());
        alternativeEntity =
                new Permission(PermissionProvider.alternativeEntity().getPermissionName());
    }

    @Test
    @DisplayName("findById returns empty Optional when not found and populated when exists")
    @Transactional
    void findById() {
        UUID randomId = UUID.randomUUID();
        Optional<Permission> missing = dao.findById(randomId);
        assertTrue(missing.isEmpty());

        Permission persisted = dao.save(singleEntity);
        Optional<Permission> found = dao.findById(persisted.getId());
        assertTrue(found.isPresent());
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
        assertEquals(2, results.size());
        assertTrue(results.containsAll(Arrays.asList(a, b)));
    }

    @Test
    @DisplayName("save new and saveAll multiple entities")
    @Transactional
    void saveAndSaveAll() {
        // single save
        Permission p = new Permission(null, "NEW_PERMISSION", Collections.emptySet());
        Permission saved = dao.save(p);
        assertNotNull(saved.getId());
        assertEquals("NEW_PERMISSION", saved.getPermissionName());

        // saveAll
        Permission x = new Permission(null, "X_PERM", Collections.emptySet());
        Permission y = new Permission(null, "Y_PERM", Collections.emptySet());
        List<Permission> batch = dao.saveAll(Arrays.asList(x, y));
        assertEquals(2, batch.size());
        batch.forEach(item -> assertNotNull(item.getId()));
    }

    @Test
    @DisplayName("updateById modifies only non-null fields and throws when missing")
    @Transactional
    void updateById() {
        Permission original = repo.save(singleEntity);
        UUID id = original.getId();

        // update existing
        Permission update = new Permission();
        update.setPermissionName("UPDATED_NAME");
        Permission updated = dao.updateById(update, id);
        assertEquals(id, updated.getId());
        assertEquals("UPDATED_NAME", updated.getPermissionName());

        // update non-existing
        UUID missing = UUID.randomUUID();
        assertThrows(NotFoundException.class, () -> dao.updateById(update, missing));
    }

    @Test
    @DisplayName("deleteById removes entity and existsById reflects removal")
    @Transactional
    void deleteByIdAndExists() {
        Permission toDelete = repo.save(singleEntity);
        UUID id = toDelete.getId();
        assertTrue(dao.existsById(id));

        dao.deleteById(id);
        assertFalse(dao.existsById(id));
    }

    @Test
    @DisplayName("findAll and findAllPage return all persisted entities")
    @Transactional
    void findAllAndPage() {
        repo.save(singleEntity);
        repo.save(alternativeEntity);

        List<Permission> all = dao.findAll();
        assertEquals(2, all.size());

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

        Permission unsaved = new Permission(null, "UNIQ_TEST", Collections.emptySet());
        assertFalse(dao.existsByUniqueProperties(unsaved));

        Permission persisted = dao.save(unsaved);
        Permission check = new Permission();
        check.setPermissionName("UNIQ_TEST");
        assertTrue(dao.existsByUniqueProperties(check));
    }

    @Test
    @DisplayName("findByPermissionName returns matching entity")
    @Transactional
    void findByPermissionName() {
        Permission saved = repo.save(singleEntity);
        Optional<Permission> opt = dao.findByPermissionName(saved.getPermissionName());
        assertTrue(opt.isPresent());
        assertEquals(saved, opt.get());

        Optional<Permission> missing = dao.findByPermissionName("NO_SUCH");
        assertTrue(missing.isEmpty());
    }
}
