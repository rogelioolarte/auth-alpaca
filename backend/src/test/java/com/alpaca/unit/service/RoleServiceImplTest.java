package com.alpaca.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alpaca.entity.Role;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.impl.RoleDAOImpl;
import com.alpaca.resources.RoleProvider;
import com.alpaca.service.impl.RoleServiceImpl;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link RoleServiceImpl} */
@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock private RoleDAOImpl dao;

    @InjectMocks private RoleServiceImpl service;

    private Role firstEntity;
    private Role secondEntity;

    @BeforeEach
    void setup() {
        firstEntity = RoleProvider.singleEntity();
        secondEntity = RoleProvider.alternativeEntity();
    }

    // --- getUserRoles ---
    @Test
    void getUserRolesCaseOne() {
        when(dao.findByRoleName(secondEntity.getRoleName())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.getUserRoles());
        verify(dao).findByRoleName(secondEntity.getRoleName());
    }

    @Test
    void getUserRolesCaseTwo() {
        when(dao.findByRoleName(secondEntity.getRoleName())).thenReturn(Optional.of(secondEntity));
        Set<Role> entitiesFound = service.getUserRoles();
        assertEquals(new HashSet<>(Set.of(secondEntity)), entitiesFound);
        verify(dao).findByRoleName(secondEntity.getRoleName());
    }

    // --- findByRoleName ---
    @Test
    void findByRoleNameCaseOne() {
        assertThrows(BadRequestException.class, () -> service.findByRoleName(null));
    }

    @Test
    void findByRoleNameCaseTwo() {
        assertThrows(BadRequestException.class, () -> service.findByRoleName("  "));
    }

    @Test
    void findByRoleNameCaseThree() {
        when(dao.findByRoleName(secondEntity.getRoleName())).thenReturn(Optional.empty());
        assertThrows(
                NotFoundException.class, () -> service.findByRoleName(secondEntity.getRoleName()));
        verify(dao).findByRoleName(secondEntity.getRoleName());
    }

    @Test
    void findByRoleNameCaseFour() {
        when(dao.findByRoleName(firstEntity.getRoleName())).thenReturn(Optional.of(firstEntity));
        Role entityFound = service.findByRoleName(firstEntity.getRoleName());
        assertEquals(firstEntity, entityFound);
        verify(dao).findByRoleName(firstEntity.getRoleName());
    }
}
