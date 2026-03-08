package com.alpaca.integration.service;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.entity.Permission;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.resources.PermissionProvider;
import com.alpaca.service.impl.PermissionServiceImpl;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link PermissionServiceImpl}. */
@SpringBootTest
@ExtendWith(SpringExtension.class)
class PermissionServiceImplIT {

    @Autowired private PermissionServiceImpl service;

    private Permission permissionTemplate;
    private Permission alternativeTemplate;

    @BeforeEach
    void setup() {
        permissionTemplate = PermissionProvider.singleTemplate();
        alternativeTemplate = PermissionProvider.alternativeTemplate();
    }

    // ------------------------------------------------
    // findById
    // ------------------------------------------------

    @Test
    @Transactional
    void findByIdShouldValidateInputAndReturnEntity() {

        assertThrows(BadRequestException.class, () -> service.findById(null));

        UUID randomId = UUID.randomUUID();
        assertThrows(NotFoundException.class, () -> service.findById(randomId));

        Permission saved = service.save(permissionTemplate);

        Permission found = service.findById(saved.getId());

        assertNotNull(found);
        assertEquals(saved.getId(), found.getId());
        assertEquals(saved, found);
    }

    // ------------------------------------------------
    // findAllByIds
    // ------------------------------------------------

    @Test
    @Transactional
    void findAllByIdsShouldValidateInputs() {

        assertThrows(BadRequestException.class, () -> service.findAllByIds(null));

        assertThrows(
                BadRequestException.class, () -> service.findAllByIds(Collections.emptyList()));

        List<UUID> invalidList = new ArrayList<>();
        invalidList.add(UUID.randomUUID());
        invalidList.add(null);

        assertThrows(BadRequestException.class, () -> service.findAllByIds(invalidList));
    }

    @Test
    @Transactional
    void findAllByIdsShouldFailWhenSomeIdsMissing() {

        Permission saved = service.save(permissionTemplate);

        List<UUID> ids = new ArrayList<>(List.of(saved.getId(), UUID.randomUUID()));

        assertThrows(NotFoundException.class, () -> service.findAllByIds(ids));
    }

    @Test
    @Transactional
    void findAllByIdsShouldReturnEntities() {

        Permission saved = service.save(permissionTemplate);

        List<Permission> result = service.findAllByIds(new ArrayList<>(List.of(saved.getId())));

        assertEquals(1, result.size());
        assertEquals(saved, result.getFirst());
    }

    // ------------------------------------------------
    // findAllByIdsToSet
    // ------------------------------------------------

    @Test
    @Transactional
    void findAllByIdsToSetShouldReturnSet() {

        Permission saved = service.save(permissionTemplate);

        Set<Permission> result = service.findAllByIdsToSet(new ArrayList<>(List.of(saved.getId())));

        assertEquals(1, result.size());
        assertTrue(result.contains(saved));
    }

    // ------------------------------------------------
    // save
    // ------------------------------------------------

    @Test
    @Transactional
    void saveShouldValidateEntity() {

        assertThrows(BadRequestException.class, () -> service.save(null));

        service.save(permissionTemplate);

        assertThrows(BadRequestException.class, () -> service.save(permissionTemplate));
    }

    @Test
    @Transactional
    void saveShouldPersistEntity() {

        Permission saved = service.save(alternativeTemplate);

        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals(alternativeTemplate.getPermissionName(), saved.getPermissionName());
    }

    // ------------------------------------------------
    // saveAll
    // ------------------------------------------------

    @Test
    @Transactional
    void saveAllShouldValidateInput() {

        assertThrows(BadRequestException.class, () -> service.saveAll(null));

        assertThrows(BadRequestException.class, () -> service.saveAll(Collections.emptyList()));

        List<Permission> invalid = new ArrayList<>();
        invalid.add(null);

        assertThrows(BadRequestException.class, () -> service.saveAll(invalid));
    }

    @Test
    @Transactional
    void saveAllShouldPersistEntities() {

        List<Permission> templates =
                new ArrayList<>(
                        List.of(
                                PermissionProvider.singleTemplate(),
                                PermissionProvider.alternativeTemplate()));

        List<Permission> saved = service.saveAll(templates);

        assertEquals(2, saved.size());
    }

    // ------------------------------------------------
    // updateById
    // ------------------------------------------------

    @Test
    @Transactional
    void updateByIdShouldValidateInputs() {

        assertThrows(BadRequestException.class, () -> service.updateById(null, null));
    }

    @Test
    @Transactional
    void updateByIdShouldUpdateEntity() {

        Permission original = service.save(permissionTemplate);
        Permission updateData = service.save(alternativeTemplate);

        Permission updated = service.updateById(updateData, original.getId());

        assertNotNull(updated);
        assertEquals(original.getId(), updated.getId());
        assertEquals(updateData.getPermissionName(), updated.getPermissionName());
    }

    // ------------------------------------------------
    // deleteById
    // ------------------------------------------------

    @Test
    @Transactional
    void deleteByIdShouldValidateInputs() {

        assertThrows(BadRequestException.class, () -> service.deleteById(null));

        assertThrows(BadRequestException.class, () -> service.deleteById(UUID.randomUUID()));
    }

    @Test
    @Transactional
    void deleteByIdShouldDeleteEntity() {

        Permission saved = service.save(permissionTemplate);

        service.deleteById(saved.getId());

        assertFalse(service.existsById(saved.getId()));
    }

    // ------------------------------------------------
    // findAll
    // ------------------------------------------------

    @Test
    @Transactional
    void findAllShouldReturnAllEntities() {

        List<Permission> templates = PermissionProvider.listTemplates();

        List<Permission> saved = templates.stream().map(service::save).collect(Collectors.toList());

        List<Permission> result = service.findAll();

        assertEquals(saved.size(), result.size());
        assertTrue(result.containsAll(saved));
    }

    // ------------------------------------------------
    // findAllPage
    // ------------------------------------------------

    @Test
    void findAllPageShouldValidatePageable() {

        assertThrows(BadRequestException.class, () -> service.findAllPage(null));
    }

    @Test
    @Transactional
    void findAllPageShouldReturnPage() {

        List<Permission> templates = PermissionProvider.listTemplates();

        templates.forEach(service::save);

        Page<Permission> page = service.findAllPage(Pageable.unpaged());

        assertNotNull(page);
        assertTrue(page.getPageable().isUnpaged());
        assertFalse(page.isEmpty());
    }

    // ------------------------------------------------
    // existsById
    // ------------------------------------------------

    @Test
    @Transactional
    void existsByIdShouldWorkCorrectly() {

        Permission saved = service.save(permissionTemplate);

        assertTrue(service.existsById(saved.getId()));

        assertFalse(service.existsById(UUID.randomUUID()));

        assertFalse(service.existsById(null));
    }

    // ------------------------------------------------
    // existsAllByIds
    // ------------------------------------------------

    @Test
    @Transactional
    void existsAllByIdsShouldValidateInputs() {

        assertFalse(service.existsAllByIds(null));

        assertFalse(service.existsAllByIds(Collections.emptyList()));

        List<UUID> invalid = new ArrayList<>();
        invalid.add(null);

        assertFalse(service.existsAllByIds(invalid));
    }

    @Test
    @Transactional
    void existsAllByIdsShouldReturnTrueWhenAllExist() {

        Permission saved = service.save(permissionTemplate);

        boolean result = service.existsAllByIds(new ArrayList<>(List.of(saved.getId())));

        assertTrue(result);
    }

    // ------------------------------------------------
    // existsByUniqueProperties
    // ------------------------------------------------

    @Test
    @Transactional
    void existsByUniquePropertiesShouldWork() {

        Permission saved = service.save(permissionTemplate);

        assertTrue(service.existsByUniqueProperties(saved));

        assertFalse(service.existsByUniqueProperties(null));
    }
}
