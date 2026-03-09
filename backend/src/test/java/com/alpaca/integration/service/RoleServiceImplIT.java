package com.alpaca.integration.service;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.entity.Role;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.resources.RoleProvider;
import com.alpaca.service.impl.RoleServiceImpl;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link RoleServiceImpl}. */
@SpringBootTest
@Transactional
class RoleServiceImplIT {

    @Autowired private RoleServiceImpl service;

    private Role firstTemplate;
    private Role alternativeTemplate;

    @BeforeEach
    void setup() {
        firstTemplate = RoleProvider.singleTemplate();
        alternativeTemplate = RoleProvider.alternativeTemplate();
    }

    // --- getUserRoles ---

    @Test
    @Transactional
    void getUserRoles_whenNoUserRole_thenThrowNotFound() {
        assertThrows(NotFoundException.class, () -> service.getUserRoles());
    }

    @Test
    @Transactional
    void getUserRoles_whenUserRoleExists_thenReturnSetContainingUserRole() {
        Role roleToSave = RoleProvider.singleTemplate();
        roleToSave.setRoleName("USER");
        roleToSave.setRoleDescription("Default user role for tests");
        roleToSave.setRolePermissions(new HashSet<>());

        Role saved = service.save(roleToSave);

        Set<Role> expected = new HashSet<>(Set.of(saved));
        assertEquals(expected, service.getUserRoles());
    }

    // --- findByRoleName ---

    @Test
    @Transactional
    void findByRoleName_whenNameIsNull_thenThrowBadRequest() {
        assertThrows(BadRequestException.class, () -> service.findByRoleName(null));
    }

    @Test
    @Transactional
    void findByRoleName_whenNameIsBlank_thenThrowBadRequest() {
        assertThrows(BadRequestException.class, () -> service.findByRoleName("   "));
    }

    @Test
    @Transactional
    void findByRoleName_whenNotFound_thenThrowNotFound() {
        assertThrows(
                NotFoundException.class,
                () -> service.findByRoleName(alternativeTemplate.getRoleName()));
    }

    @Test
    @Transactional
    void findByRoleName_whenExists_thenReturnRole() {
        Role toSave = RoleProvider.singleTemplate();
        toSave.setRoleName(firstTemplate.getRoleName());
        toSave.setRoleDescription(firstTemplate.getRoleDescription());
        toSave.setRolePermissions(new HashSet<>());

        Role saved = service.save(toSave);

        Role found = service.findByRoleName(firstTemplate.getRoleName());
        assertEquals(saved, found);
    }
}
