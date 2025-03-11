package com.alpaca.unit.persistence;

import com.alpaca.entity.Role;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.impl.RoleDAOImpl;
import com.alpaca.repository.RoleRepo;
import com.alpaca.resources.RoleProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleDAOImplTest {

    @Mock
    private RoleRepo repo;

    @InjectMocks
    private RoleDAOImpl dao;
    
    @Test
    void findByRoleName() {
        Role firstRole = new Role();
        firstRole.setRoleName(null);
        assertEquals(dao.findByRoleName(firstRole.getRoleName()), Optional.empty());

        Role secondRole = new Role();
        secondRole.setRoleName("  ");
        assertEquals(dao.findByRoleName(secondRole.getRoleName()), Optional.empty());

        Role roleSecond = RoleProvider.alternativeEntity();
        when(repo.findByRoleName(roleSecond.getRoleName())).thenReturn(Optional.of(roleSecond));
        Role roleFoundSecond = dao.findByRoleName(roleSecond.getRoleName())
                .orElseGet(Role::new);
        assertEquals(roleFoundSecond, roleSecond);
        verify(repo).findByRoleName(roleSecond.getRoleName());

        Role role = RoleProvider.singleEntity();
        when(repo.findByRoleName(role.getRoleName())).thenReturn(Optional.of(role));
        Role roleFound = dao.findByRoleName(role.getRoleName())
                .orElseGet(Role::new);
        assertEquals(roleFound, role);
        verify(repo).findByRoleName(role.getRoleName());
    }

    @Test
    void updateById() {
        UUID initialId = RoleProvider.alternativeEntity().getId();
        Role initialRole = RoleProvider.alternativeEntity();
        when(repo.findById(initialId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> dao.updateById(initialRole, initialId));
        verify(repo).findById(initialId);

        UUID idSecond = RoleProvider.alternativeEntity().getId();
        Role roleSecond = RoleProvider.alternativeEntity();
        Role newRoleSecond = new Role();
        newRoleSecond.setRoleName(null);
        when(repo.findById(idSecond)).thenReturn(Optional.of(roleSecond));
        when(repo.save(roleSecond)).thenReturn(roleSecond);
        Role roleUpdatedSecond = dao.updateById(newRoleSecond, idSecond);
        assertNotNull(roleUpdatedSecond);
        assertEquals(roleSecond.getId(), roleUpdatedSecond.getId());
        assertNotEquals(newRoleSecond.getRoleName(), roleUpdatedSecond.getRoleName());
        assertNotEquals(newRoleSecond.getId(), roleUpdatedSecond.getId());
        verify(repo, times(2)).findById(idSecond);
        verify(repo).save(roleSecond);

        UUID idThird = RoleProvider.alternativeEntity().getId();
        Role roleThird = RoleProvider.alternativeEntity();
        Role newRoleThird = new Role();
        newRoleThird.setRoleName(" ");
        when(repo.findById(idThird)).thenReturn(Optional.of(roleThird));
        when(repo.save(roleThird)).thenReturn(roleThird);
        Role roleUpdatedThird = dao.updateById(newRoleThird, idThird);
        assertNotNull(roleUpdatedThird);
        assertEquals(roleThird.getId(), roleUpdatedThird.getId());
        assertNotEquals(newRoleThird.getRoleName(), roleUpdatedThird.getRoleName());
        assertNotEquals(newRoleThird.getId(), roleUpdatedThird.getId());
        verify(repo, times(3)).findById(idThird);
        verify(repo).save(roleThird);

        UUID id = RoleProvider.singleEntity().getId();
        Role role = RoleProvider.singleEntity();
        Role newRole = RoleProvider.alternativeEntity();
        when(repo.findById(id)).thenReturn(Optional.of(role));
        when(repo.save(role)).thenReturn(role);
        Role roleUpdated = dao.updateById(newRole, id);
        assertNotNull(roleUpdated);
        assertEquals(role.getId(), roleUpdated.getId());
        assertEquals(newRole.getRoleName(), roleUpdated.getRoleName());
        assertNotEquals(newRole.getId(), roleUpdated.getId());
        verify(repo).findById(id);
        verify(repo).save(role);
    }

    @Test
    void existsByUniqueProperties() {
        Role firstRole = new Role();
        firstRole.setRoleName(null);
        assertFalse(dao.existsByUniqueProperties(firstRole));

        Role secondRole = new Role();
        secondRole.setRoleName("  ");
        assertFalse(dao.existsByUniqueProperties(secondRole));

        Role thirdRole = new Role();
        thirdRole.setRoleDescription(null);
        assertFalse(dao.existsByUniqueProperties(thirdRole));

        Role fourthRole = new Role();
        fourthRole.setRoleDescription("  ");
        assertFalse(dao.existsByUniqueProperties(fourthRole));

        Role roleSecond = RoleProvider.alternativeEntity();
        when(repo.existsByRoleName(roleSecond.getRoleName())).thenReturn(false);
        assertFalse(dao.existsByUniqueProperties(roleSecond));
        verify(repo).existsByRoleName(roleSecond.getRoleName());

        Role role = RoleProvider.singleEntity();
        when(repo.existsByRoleName(role.getRoleName())).thenReturn(true);
        assertTrue(dao.existsByUniqueProperties(role));
        verify(repo).existsByRoleName(role.getRoleName());
    }
}