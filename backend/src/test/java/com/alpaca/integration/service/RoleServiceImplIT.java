package com.alpaca.integration.service;

import com.alpaca.entity.Role;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.resources.RoleProvider;
import com.alpaca.service.impl.RoleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Integration tests for {@link RoleServiceImpl} */
@SpringBootTest
@ExtendWith(SpringExtension.class)
class RoleServiceImplIT {

    @Autowired private RoleServiceImpl service;

    private Role firstEntity;
    private Role secondEntity;

    @BeforeEach
    void setup() {
        firstEntity = RoleProvider.singleEntity();
        secondEntity = RoleProvider.alternativeEntity();
    }

    // --- getUserRoles ---
    @Test
    @Transactional
    void getUserRolesCaseOne() {
        assertThrows(NotFoundException.class, () -> service.getUserRoles());
    }

    @Test
    @Transactional
    void getUserRolesCaseTwo() {
        Role role =
                service.save(
                        new Role(
                                secondEntity.getRoleName(),
                                secondEntity.getRoleDescription(),
                                new HashSet<>()));
        assertEquals(new HashSet<>(Set.of(role)), service.getUserRoles());
    }

    // --- findByRoleName ---
    @Test
    @Transactional
    void findByRoleNameCaseOne() {
        assertThrows(BadRequestException.class, () -> service.findByRoleName(null));
    }

    @Test
    @Transactional
    void findByRoleNameCaseTwo() {
        assertThrows(BadRequestException.class, () -> service.findByRoleName("  "));
    }

    @Test
    @Transactional
    void findByRoleNameCaseThree() {
        assertThrows(
                NotFoundException.class, () -> service.findByRoleName(secondEntity.getRoleName()));
    }

    @Test
    @Transactional
    void findByRoleNameCaseFour() {
        Role role =
                service.save(
                        new Role(
                                firstEntity.getRoleName(),
                                firstEntity.getRoleDescription(),
                                new HashSet<>()));
        assertEquals(role, service.findByRoleName(firstEntity.getRoleName()));
    }
}
