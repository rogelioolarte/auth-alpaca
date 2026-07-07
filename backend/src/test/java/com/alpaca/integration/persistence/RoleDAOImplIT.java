package com.alpaca.integration.persistence;

import com.alpaca.entity.Role;
import com.alpaca.persistence.IRoleDAO;
import com.alpaca.persistence.impl.PermissionDAOImpl;
import com.alpaca.persistence.impl.RoleDAOImpl;
import com.alpaca.repository.RoleRepo;
import com.alpaca.resources.provider.RoleProvider;
import com.alpaca.resources.utility.DataJpaIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Integration tests for {@link RoleDAOImpl}. */
@DataJpaIntegrationTest
@Import({RoleDAOImpl.class, PermissionDAOImpl.class})
@DisplayName("RoleDAOImpl Integration Tests")
class RoleDAOImplIT {

    @Autowired private IRoleDAO dao;
    @Autowired private RoleRepo repo;

    private Role saved;

    @BeforeEach
    void initData() {
        // Create and persist a base role entity for tests
        Instant now = Instant.now();
        String createdUserId = UUID.randomUUID().toString();
        Role role = RoleProvider.singleTemplate();
        role.setCreatedAt(now);
        role.setCreatedBy(createdUserId);
        saved = repo.save(role);
    }

    @Test
    @DisplayName("findByRoleName returns empty for null/blank and finds existing by name")
    @Transactional
    void findByRoleName() {
        // null or blank
        assertTrue(dao.findByRoleName(null).isEmpty(), "null should return empty Optional");
        assertTrue(dao.findByRoleName("  ").isEmpty(), "blank should return empty Optional");

        Optional<Role> found = dao.findByRoleName(saved.getName());
        assertTrue(found.isPresent(), "existing role name must be found");
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    @DisplayName("existsByUniqueProperties returns false for invalid and true when exists")
    @Transactional
    void existsByUniqueProperties() {
        // missing name or desc -> false
        Role r1 = new Role();
        r1.setName(null);
        r1.setDescription("Desc");
        assertFalse(dao.existsByUniqueProperties(r1));

        Role r2 = new Role();
        r2.setName("Name");
        r2.setDescription("  ");
        assertFalse(dao.existsByUniqueProperties(r2));

        // valid but not saved -> false
        Role r3 = new Role();
        r3.setName("UNKNOWN");
        r3.setDescription("Some desc");
        assertFalse(dao.existsByUniqueProperties(r3));

        // saved case -> true
        Role exists = new Role();
        exists.setName(saved.getName());
        exists.setDescription(saved.getDescription());
        assertTrue(dao.existsByUniqueProperties(exists));
    }

    @Test
    @DisplayName("existsAllByIds: verifies presence of multiple IDs")
    @Transactional
    void existsAllByIds_ShouldVerifyCount() {
        Role role2 = RoleProvider.alternativeTemplate();
        role2.setCreatedAt(saved.getCreatedAt());
        Role t1 = saved;
        Role t2 = repo.saveAndFlush(role2);

        assertTrue(dao.existsAllByIds(List.of(t1.getId(), t2.getId())));
        assertFalse(dao.existsAllByIds(List.of(t1.getId(), UUID.randomUUID())));
    }
}
