package com.alpaca.unit.service;

import com.alpaca.entity.Role;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.impl.RoleDAOImpl;
import com.alpaca.resources.RoleProvider;
import com.alpaca.service.impl.RoleServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleDAOImpl dao;

    @InjectMocks
    private RoleServiceImpl service;

    @Test
    void getUserRoles() {
        Role entitySecond = RoleProvider.alternativeEntity();
        when(dao.findByRoleName(entitySecond.getRoleName())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.getUserRoles());
        verify(dao).findByRoleName(entitySecond.getRoleName());

        Role entity = RoleProvider.alternativeEntity();
        when(dao.findByRoleName(entity.getRoleName())).thenReturn(Optional.of(entity));
        Set<Role> entitiesFound = service.getUserRoles();
        assertEquals(new HashSet<>(Set.of(entity)), entitiesFound);
        verify(dao, times(2)).findByRoleName(entity.getRoleName());
    }

    @Test
    void findByRoleName() {
        assertThrows(BadRequestException.class, () -> service.findByRoleName(null));

        assertThrows(BadRequestException.class, () -> service.findByRoleName("  "));

        Role entitySecond = RoleProvider.alternativeEntity();
        when(dao.findByRoleName(entitySecond.getRoleName())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.findByRoleName(entitySecond.getRoleName()));
        verify(dao).findByRoleName(entitySecond.getRoleName());

        Role entity = RoleProvider.singleEntity();
        when(dao.findByRoleName(entity.getRoleName())).thenReturn(Optional.of(entity));
        Role entityFound = service.findByRoleName(entity.getRoleName());
        assertEquals(entity, entityFound);
        verify(dao).findByRoleName(entity.getRoleName());
    }
}