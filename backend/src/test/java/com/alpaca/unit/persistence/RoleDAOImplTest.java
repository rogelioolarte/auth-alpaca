package com.alpaca.unit.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alpaca.entity.Permission;
import com.alpaca.entity.Role;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.impl.RoleDAOImpl;
import com.alpaca.repository.RoleRepo;
import com.alpaca.resources.PermissionProvider;
import com.alpaca.resources.RoleProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link RoleDAOImpl} */
@ExtendWith(MockitoExtension.class)
class RoleDAOImplTest {

    @Mock private RoleRepo repo;

    @InjectMocks private RoleDAOImpl dao;

    private Role firstEntity;
    private Role secondEntity;
    private Role thirdEntity;

    @BeforeEach
    void setup() {
        firstEntity = RoleProvider.singleEntity();
        secondEntity = RoleProvider.alternativeEntity();
        thirdEntity = RoleProvider.alternativeEntity();
    }

    // --- findByRoleName ---
    @Test
    void findByRoleNameCaseOne() {
        Role entityWithNullRoleName = new Role();
        entityWithNullRoleName.setRoleName(null);
        assertEquals(dao.findByRoleName(entityWithNullRoleName.getRoleName()), Optional.empty());
    }

    @Test
    void findByRoleNameCaseTwo() {
        Role entityWithEmptyRoleName = new Role();
        entityWithEmptyRoleName.setRoleName("  ");
        assertEquals(dao.findByRoleName(entityWithEmptyRoleName.getRoleName()), Optional.empty());
    }

    @Test
    void findByRoleNameCaseThree() {
        when(repo.findByRoleName(secondEntity.getRoleName())).thenReturn(Optional.empty());
        Role entityFound = dao.findByRoleName(secondEntity.getRoleName()).orElseGet(Role::new);
        assertNotEquals(entityFound, secondEntity);
        verify(repo).findByRoleName(secondEntity.getRoleName());
    }

    @Test
    void findByRoleNameCaseFour() {
        when(repo.findByRoleName(firstEntity.getRoleName())).thenReturn(Optional.of(firstEntity));
        Role entityFound = dao.findByRoleName(firstEntity.getRoleName()).orElseGet(Role::new);
        assertEquals(entityFound, firstEntity);
        verify(repo).findByRoleName(firstEntity.getRoleName());
    }

    // --- updateById ---
    @Test
    void updateByIdCaseOne() {
        when(repo.findById(secondEntity.getId())).thenReturn(Optional.empty());
        assertThrows(
                NotFoundException.class, () -> dao.updateById(secondEntity, secondEntity.getId()));
        verify(repo).findById(secondEntity.getId());
    }

    @Test
    void updateByIdCaseTwo() {
        Role newEntitySecond = new Role();
        newEntitySecond.setRoleName(null);
        newEntitySecond.setRoleDescription(null);
        newEntitySecond.setRolePermissions(null);
        when(repo.findById(secondEntity.getId())).thenReturn(Optional.of(secondEntity));
        when(repo.save(secondEntity)).thenReturn(secondEntity);
        Role updatedEntity = dao.updateById(newEntitySecond, secondEntity.getId());
        assertNotNull(updatedEntity);
        assertEquals(secondEntity.getId(), updatedEntity.getId());
        assertNotEquals(newEntitySecond.getRoleName(), updatedEntity.getRoleName());
        verify(repo).findById(secondEntity.getId());
        verify(repo).save(secondEntity);
    }

    @Test
    void updateByIdCaseThree() {
        Role newEntityThird = new Role();
        newEntityThird.setRoleName("  ");
        newEntityThird.setRoleDescription("  ");
        newEntityThird.setRolePermissions(Collections.emptySet());
        when(repo.findById(thirdEntity.getId())).thenReturn(Optional.of(thirdEntity));
        when(repo.save(thirdEntity)).thenReturn(thirdEntity);
        Role updatedEntity = dao.updateById(newEntityThird, thirdEntity.getId());
        assertNotNull(updatedEntity);
        assertEquals(thirdEntity.getId(), updatedEntity.getId());
        assertNotEquals(newEntityThird.getRoleName(), updatedEntity.getRoleName());
        verify(repo).findById(thirdEntity.getId());
        verify(repo).save(thirdEntity);
    }

    @Test
    void updateByIdCaseFour() {
        Permission permission = PermissionProvider.singleEntity();
        secondEntity.setRolePermissions(new HashSet<>(Set.of(permission)));
        when(repo.findById(firstEntity.getId())).thenReturn(Optional.of(firstEntity));
        when(repo.save(firstEntity)).thenReturn(firstEntity);
        Role updatedEntity = dao.updateById(secondEntity, firstEntity.getId());
        assertNotNull(updatedEntity);
        assertEquals(firstEntity.getId(), updatedEntity.getId());
        assertEquals(secondEntity.getRoleName(), updatedEntity.getRoleName());
        assertNotEquals(secondEntity.getId(), updatedEntity.getId());
        verify(repo).findById(firstEntity.getId());
        verify(repo).save(firstEntity);
    }

    // --- existsByUniqueProperties ---
    @Test
    void existsByUniquePropertiesCaseOne() {
        Role entityWithNullRoleName = new Role();
        entityWithNullRoleName.setRoleName(null);
        assertFalse(dao.existsByUniqueProperties(entityWithNullRoleName));
    }

    @Test
    void existsByUniquePropertiesCaseTwo() {
        Role entityWithEmptyRoleName = new Role();
        entityWithEmptyRoleName.setRoleName("  ");
        assertFalse(dao.existsByUniqueProperties(entityWithEmptyRoleName));
    }

    @Test
    void existsByUniquePropertiesCaseThree() {
        Role entityWithEmptyRoleName = new Role();
        entityWithEmptyRoleName.setRoleName("valid name");
        entityWithEmptyRoleName.setRoleDescription(null);
        assertFalse(dao.existsByUniqueProperties(entityWithEmptyRoleName));
    }

    @Test
    void existsByUniquePropertiesCaseFour() {
        Role entityWithEmptyRoleName = new Role();
        entityWithEmptyRoleName.setRoleName("valid name");
        entityWithEmptyRoleName.setRoleDescription("  ");
        assertFalse(dao.existsByUniqueProperties(entityWithEmptyRoleName));
    }

    @Test
    void existsByUniquePropertiesCaseFive() {
        when(repo.existsByRoleName(secondEntity.getRoleName())).thenReturn(false);
        assertFalse(dao.existsByUniqueProperties(secondEntity));
        verify(repo).existsByRoleName(secondEntity.getRoleName());
    }

    @Test
    void existsByUniquePropertiesCaseSix() {
        when(repo.existsByRoleName(firstEntity.getRoleName())).thenReturn(true);
        assertTrue(dao.existsByUniqueProperties(firstEntity));
        verify(repo).existsByRoleName(firstEntity.getRoleName());
    }
}
