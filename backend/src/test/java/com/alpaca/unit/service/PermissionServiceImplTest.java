package com.alpaca.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alpaca.entity.Permission;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.impl.PermissionDAOImpl;
import com.alpaca.resources.PermissionProvider;
import com.alpaca.service.impl.PermissionServiceImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/** Unit tests for {@link PermissionServiceImpl}. */
@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

    @Mock private PermissionDAOImpl dao;

    @InjectMocks private PermissionServiceImpl service;

    private Permission firstEntity;
    private Permission secondEntity;
    private List<Permission> entities;
    private List<UUID> ids;

    @BeforeEach
    void setup() {
        firstEntity = PermissionProvider.singleEntity();
        secondEntity = PermissionProvider.alternativeEntity();
        entities = PermissionProvider.listEntities();
        ids = entities.stream().map(Permission::getId).toList();
    }

    // --- findById ---

    @Test
    void findByIdShouldThrowBadRequestWhenIdIsNull() {
        assertThrows(BadRequestException.class, () -> service.findById(null));

        verifyNoInteractions(dao);
    }

    @Test
    void findByIdShouldThrowNotFoundWhenEntityDoesNotExist() {
        UUID permissionId = firstEntity.getId();

        when(dao.findById(permissionId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.findById(permissionId));

        verify(dao).findById(permissionId);
    }

    @Test
    void findByIdShouldReturnEntityWhenEntityExists() {
        UUID permissionId = secondEntity.getId();

        when(dao.findById(permissionId)).thenReturn(Optional.of(secondEntity));

        Permission result = service.findById(permissionId);

        assertNotNull(result);
        assertEquals(secondEntity, result);

        verify(dao).findById(permissionId);
    }

    // --- findAllByIds ---

    @Test
    void findAllByIdsShouldThrowBadRequestWhenIdsAreNull() {
        assertThrows(BadRequestException.class, () -> service.findAllByIds(null));

        verifyNoInteractions(dao);
    }

    @Test
    void findAllByIdsShouldThrowBadRequestWhenIdsAreEmpty() {
        List<UUID> emptyIds = Collections.emptyList();

        assertThrows(BadRequestException.class, () -> service.findAllByIds(emptyIds));

        verifyNoInteractions(dao);
    }

    @Test
    void findAllByIdsShouldThrowBadRequestWhenIdsContainNull() {
        List<UUID> invalidIds = new ArrayList<>(ids);
        invalidIds.add(null);

        assertThrows(BadRequestException.class, () -> service.findAllByIds(invalidIds));

        verifyNoInteractions(dao);
    }

    @Test
    void findAllByIdsShouldThrowNotFoundWhenSomeEntitiesDoNotExist() {
        List<UUID> permissionIds = List.of(firstEntity.getId(), secondEntity.getId());

        when(dao.findAllByIds(permissionIds)).thenReturn(List.of(firstEntity));

        assertThrows(NotFoundException.class, () -> service.findAllByIds(permissionIds));

        verify(dao).findAllByIds(permissionIds);
    }

    @Test
    void findAllByIdsShouldReturnEntitiesWhenAllExist() {
        List<UUID> permissionIds = List.of(firstEntity.getId());

        when(dao.findAllByIds(permissionIds)).thenReturn(List.of(firstEntity));

        List<Permission> result = service.findAllByIds(permissionIds);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(firstEntity, result.getFirst());

        verify(dao).findAllByIds(permissionIds);
    }

    // --- save ---

    @Test
    void saveShouldThrowBadRequestWhenEntityIsNull() {
        assertThrows(BadRequestException.class, () -> service.save(null));

        verifyNoInteractions(dao);
    }

    @Test
    void saveShouldThrowBadRequestWhenEntityAlreadyExists() {
        when(dao.existsByUniqueProperties(firstEntity)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.save(firstEntity));

        verify(dao).existsByUniqueProperties(firstEntity);
        verify(dao, never()).save(any(Permission.class));
    }

    @Test
    void saveShouldPersistEntityWhenEntityDoesNotExist() {
        when(dao.existsByUniqueProperties(secondEntity)).thenReturn(false);
        when(dao.save(secondEntity)).thenReturn(secondEntity);

        Permission result = service.save(secondEntity);

        assertNotNull(result);
        assertEquals(secondEntity, result);

        ArgumentCaptor<Permission> permissionCaptor = ArgumentCaptor.forClass(Permission.class);

        verify(dao).existsByUniqueProperties(secondEntity);
        verify(dao).save(permissionCaptor.capture());

        assertEquals(secondEntity.getId(), permissionCaptor.getValue().getId());
    }

    // --- saveAll ---

    @Test
    void saveAllShouldThrowBadRequestWhenEntitiesAreNull() {
        assertThrows(BadRequestException.class, () -> service.saveAll(null));

        verifyNoInteractions(dao);
    }

    @Test
    void saveAllShouldThrowBadRequestWhenEntitiesAreEmpty() {
        List<Permission> emptyPermissions = Collections.emptyList();

        assertThrows(BadRequestException.class, () -> service.saveAll(emptyPermissions));

        verifyNoInteractions(dao);
    }

    @Test
    void saveAllShouldThrowBadRequestWhenEntitiesContainNull() {
        List<Permission> invalidPermissions = new ArrayList<>(entities);
        invalidPermissions.add(null);

        assertThrows(BadRequestException.class, () -> service.saveAll(invalidPermissions));

        verifyNoInteractions(dao);
    }

    @Test
    void saveAllShouldPersistEntitiesSuccessfully() {
        when(dao.saveAll(entities)).thenReturn(entities);

        List<Permission> result = service.saveAll(entities);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(entities, result);

        verify(dao).saveAll(entities);
    }

    // --- updateById ---

    @Test
    void updateByIdShouldThrowBadRequestWhenPermissionAndIdAreNull() {
        assertThrows(BadRequestException.class, () -> service.updateById(null, null));

        verifyNoInteractions(dao);
    }

    @Test
    void updateByIdShouldThrowBadRequestWhenPermissionIsNull() {
        UUID permissionId = firstEntity.getId();

        assertThrows(BadRequestException.class, () -> service.updateById(null, permissionId));

        verifyNoInteractions(dao);
    }

    @Test
    void updateByIdShouldThrowBadRequestWhenIdIsNull() {
        assertThrows(BadRequestException.class, () -> service.updateById(firstEntity, null));

        verifyNoInteractions(dao);
    }

    @Test
    void updateByIdShouldThrowNotFoundWhenPermissionDoesNotExist() {
        UUID permissionId = firstEntity.getId();

        when(dao.findById(permissionId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.updateById(firstEntity, permissionId));

        verify(dao).findById(permissionId);
        verify(dao, never()).save(any(Permission.class));
    }

    @Test
    void updateByIdShouldUpdateNameWhenIncomingNameIsDifferent() {
        Permission existingPermission = PermissionProvider.singleEntity();

        Permission incomingPermission = PermissionProvider.singleEntity();
        incomingPermission.setName(secondEntity.getName());

        UUID permissionId = existingPermission.getId();

        when(dao.findById(permissionId)).thenReturn(Optional.of(existingPermission));
        when(dao.save(existingPermission)).thenReturn(existingPermission);

        Permission result = service.updateById(incomingPermission, permissionId);

        assertNotNull(result);
        assertEquals(incomingPermission.getName(), result.getName());

        ArgumentCaptor<Permission> permissionCaptor = ArgumentCaptor.forClass(Permission.class);

        verify(dao).findById(permissionId);
        verify(dao).save(permissionCaptor.capture());

        assertEquals(incomingPermission.getName(), permissionCaptor.getValue().getName());
    }

    @Test
    void updateByIdShouldNotUpdateNameWhenIncomingNameIsBlank() {
        Permission existingPermission = PermissionProvider.singleEntity();

        Permission incomingPermission = PermissionProvider.singleEntity();
        incomingPermission.setName(" ");

        UUID permissionId = existingPermission.getId();
        String originalName = existingPermission.getName();

        when(dao.findById(permissionId)).thenReturn(Optional.of(existingPermission));
        when(dao.save(existingPermission)).thenReturn(existingPermission);

        Permission result = service.updateById(incomingPermission, permissionId);

        assertNotNull(result);
        assertEquals(originalName, result.getName());

        verify(dao).findById(permissionId);
        verify(dao).save(existingPermission);
    }

    @Test
    void updateByIdShouldNotUpdateNameWhenIncomingNameIsSame() {
        Permission existingPermission = PermissionProvider.singleEntity();

        Permission incomingPermission = PermissionProvider.singleEntity();
        incomingPermission.setName(existingPermission.getName());

        UUID permissionId = existingPermission.getId();

        when(dao.findById(permissionId)).thenReturn(Optional.of(existingPermission));
        when(dao.save(existingPermission)).thenReturn(existingPermission);

        Permission result = service.updateById(incomingPermission, permissionId);

        assertNotNull(result);
        assertEquals(existingPermission.getName(), result.getName());

        verify(dao).findById(permissionId);
        verify(dao).save(existingPermission);
    }

    // --- deleteById ---

    @Test
    void deleteByIdShouldThrowBadRequestWhenIdIsNull() {
        assertThrows(BadRequestException.class, () -> service.deleteById(null));

        verifyNoInteractions(dao);
    }

    @Test
    void deleteByIdShouldThrowBadRequestWhenEntityDoesNotExist() {
        UUID permissionId = firstEntity.getId();

        when(dao.existsById(permissionId)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> service.deleteById(permissionId));

        verify(dao).existsById(permissionId);
        verify(dao, never()).deleteById(permissionId);
    }

    @Test
    void deleteByIdShouldDeleteEntityWhenEntityExists() {
        UUID permissionId = secondEntity.getId();

        when(dao.existsById(permissionId)).thenReturn(true);

        service.deleteById(permissionId);

        ArgumentCaptor<UUID> idCaptor = ArgumentCaptor.forClass(UUID.class);

        verify(dao).existsById(permissionId);
        verify(dao).deleteById(idCaptor.capture());

        assertEquals(permissionId, idCaptor.getValue());
    }

    // --- findAll ---

    @Test
    void findAllShouldReturnAllEntities() {
        when(dao.findAll()).thenReturn(entities);

        List<Permission> result = service.findAll();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(entities, result);

        verify(dao).findAll();
    }

    // --- findAllPage ---

    @Test
    void findAllPageShouldThrowBadRequestWhenPageableIsNull() {
        assertThrows(BadRequestException.class, () -> service.findAllPage(null));

        verifyNoInteractions(dao);
    }

    @Test
    void findAllPageShouldReturnPagedEntities() {
        Pageable pageable = Pageable.unpaged();
        Page<Permission> expectedPage = new PageImpl<>(entities);

        when(dao.findAllPage(pageable)).thenReturn(expectedPage);

        Page<Permission> result = service.findAllPage(pageable);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(entities, result.getContent());

        verify(dao).findAllPage(pageable);
    }

    // --- existsById ---

    @Test
    void existsByIdShouldReturnFalseWhenIdIsNull() {
        boolean result = service.existsById(null);

        assertFalse(result);

        verifyNoInteractions(dao);
    }

    @Test
    void existsByIdShouldReturnFalseWhenEntityDoesNotExist() {
        UUID permissionId = firstEntity.getId();

        when(dao.existsById(permissionId)).thenReturn(false);

        boolean result = service.existsById(permissionId);

        assertFalse(result);

        verify(dao).existsById(permissionId);
    }

    @Test
    void existsByIdShouldReturnTrueWhenEntityExists() {
        UUID permissionId = secondEntity.getId();

        when(dao.existsById(permissionId)).thenReturn(true);

        boolean result = service.existsById(permissionId);

        assertTrue(result);

        verify(dao).existsById(permissionId);
    }

    // --- existsAllByIds ---

    @Test
    void existsAllByIdsShouldReturnFalseWhenIdsAreNull() {
        boolean result = service.existsAllByIds(null);

        assertFalse(result);

        verifyNoInteractions(dao);
    }

    @Test
    void existsAllByIdsShouldReturnFalseWhenIdsAreEmpty() {
        boolean result = service.existsAllByIds(Collections.emptyList());

        assertFalse(result);

        verifyNoInteractions(dao);
    }

    @Test
    void existsAllByIdsShouldReturnFalseWhenIdsContainNull() {
        List<UUID> invalidIds = new ArrayList<>(ids);
        invalidIds.add(null);

        boolean result = service.existsAllByIds(invalidIds);

        assertFalse(result);

        verifyNoInteractions(dao);
    }

    @Test
    void existsAllByIdsShouldReturnFalseWhenNotAllEntitiesExist() {
        List<UUID> permissionIds = List.of(firstEntity.getId());

        when(dao.existsAllByIds(permissionIds)).thenReturn(false);

        boolean result = service.existsAllByIds(permissionIds);

        assertFalse(result);

        verify(dao).existsAllByIds(permissionIds);
    }

    @Test
    void existsAllByIdsShouldReturnTrueWhenAllEntitiesExist() {
        List<UUID> permissionIds = List.of(secondEntity.getId());

        when(dao.existsAllByIds(permissionIds)).thenReturn(true);

        boolean result = service.existsAllByIds(permissionIds);

        assertTrue(result);

        verify(dao).existsAllByIds(permissionIds);
    }

    // --- existsByUniqueProperties ---

    @Test
    void existsByUniquePropertiesShouldReturnFalseWhenEntityIsNull() {
        boolean result = service.existsByUniqueProperties(null);

        assertFalse(result);

        verifyNoInteractions(dao);
    }

    @Test
    void existsByUniquePropertiesShouldReturnFalseWhenEntityDoesNotExist() {
        when(dao.existsByUniqueProperties(firstEntity)).thenReturn(false);

        boolean result = service.existsByUniqueProperties(firstEntity);

        assertFalse(result);

        verify(dao).existsByUniqueProperties(firstEntity);
    }

    @Test
    void existsByUniquePropertiesShouldReturnTrueWhenEntityExists() {
        when(dao.existsByUniqueProperties(secondEntity)).thenReturn(true);

        boolean result = service.existsByUniqueProperties(secondEntity);

        assertTrue(result);

        verify(dao).existsByUniqueProperties(secondEntity);
    }

    // --- utility methods from GenericServiceImpl ---

    @Test
    void invalidCollectionShouldReturnTrueWhenCollectionIsNull() {
        boolean result = service.invalidCollection(null);

        assertTrue(result);
    }

    @Test
    void invalidCollectionShouldReturnTrueWhenCollectionIsEmpty() {
        boolean result = service.invalidCollection(Collections.emptyList());

        assertTrue(result);
    }

    @Test
    void invalidCollectionShouldReturnTrueWhenCollectionContainsNull() {
        List<String> values = new ArrayList<>();
        values.add("permission");
        values.add(null);

        boolean result = service.invalidCollection(values);

        assertTrue(result);
    }

    @Test
    void invalidCollectionShouldReturnFalseWhenCollectionIsValid() {
        List<String> values = List.of("read", "write");

        boolean result = service.invalidCollection(values);

        assertFalse(result);
    }

    @Test
    void hasNullShouldReturnTrueWhenCollectionContainsNull() {
        List<String> values = new ArrayList<>();
        values.add("admin");
        values.add(null);

        boolean result = service.hasNull(values);

        assertTrue(result);
    }

    @Test
    void hasNullShouldReturnFalseWhenCollectionDoesNotContainNull() {
        List<String> values = List.of("admin", "user");

        boolean result = service.hasNull(values);

        assertFalse(result);
    }

    @Test
    void updateIfNotNullShouldUpdateValueWhenIncomingIsDifferent() {
        List<String> values = new ArrayList<>();

        service.updateIfNotNull("READ", "WRITE", values::add);

        assertEquals(List.of("WRITE"), values);
    }

    @Test
    void updateIfNotNullShouldNotUpdateValueWhenIncomingIsNull() {
        List<String> values = new ArrayList<>();

        service.updateIfNotNull("READ", null, values::add);

        assertTrue(values.isEmpty());
    }

    @Test
    void updateIfNotNullShouldNotUpdateValueWhenIncomingIsEqual() {
        List<String> values = new ArrayList<>();

        service.updateIfNotNull("READ", "READ", values::add);

        assertTrue(values.isEmpty());
    }

    @Test
    void updateTextIfExistsShouldUpdateTextWhenIncomingHasTextAndIsDifferent() {
        List<String> values = new ArrayList<>();

        service.updateTextIfExists("READ", "WRITE", values::add);

        assertEquals(List.of("WRITE"), values);
    }

    @Test
    void updateTextIfExistsShouldNotUpdateTextWhenIncomingIsBlank() {
        List<String> values = new ArrayList<>();

        service.updateTextIfExists("READ", " ", values::add);

        assertTrue(values.isEmpty());
    }

    @Test
    void updateTextIfExistsShouldNotUpdateTextWhenIncomingIsEqual() {
        List<String> values = new ArrayList<>();

        service.updateTextIfExists("READ", "READ", values::add);

        assertTrue(values.isEmpty());
    }

    @Test
    void updateIfDifferentShouldUpdateBooleanWhenValuesAreDifferent() {
        List<Boolean> values = new ArrayList<>();

        service.updateIfDifferent(true, false, values::add);

        assertEquals(List.of(false), values);
    }

    @Test
    void updateIfDifferentShouldNotUpdateBooleanWhenValuesAreEqual() {
        List<Boolean> values = new ArrayList<>();

        service.updateIfDifferent(true, true, values::add);

        assertTrue(values.isEmpty());
    }
}
