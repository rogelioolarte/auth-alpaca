package com.alpaca.integration.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.alpaca.entity.Permission;
import com.alpaca.entity.Role;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IRoleDAO;
import com.alpaca.repository.PermissionRepo;
import com.alpaca.repository.RoleRepo;
import com.alpaca.repository.intermediate.RolePermissionRepo;
import com.alpaca.resources.RoleProvider;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link com.alpaca.persistence.impl.RoleDAOImpl} */
@SpringBootTest
@ExtendWith(SpringExtension.class)
class RoleDAOImplIT {

    @Autowired private IRoleDAO dao;

    @Autowired private RoleRepo repo;

    @Autowired private PermissionRepo permissionRepo;
    @Autowired private RolePermissionRepo rolePermissionRepo;

    private Role saved;

    @BeforeEach
    void initData() {
        // prepare a role for findByRoleName and updateById
        saved = repo.save(RoleProvider.singleTemplate());
    }

    @Test
    @DisplayName("findByRoleName returns empty for null/blank and finds existing by name")
    @Transactional
    void findByRoleName() {
        // null or blank
        assertTrue(dao.findByRoleName(null).isEmpty());
        assertTrue(dao.findByRoleName("  ").isEmpty());

        Optional<Role> found = dao.findByRoleName(saved.getRoleName());
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    @DisplayName("updateById updates non-null/blank fields only and throws if missing")
    @Transactional
    void updateById() {
        UUID id = saved.getId();
        // full update
        Role request = new Role();
        request.setRoleName("NEW_ROLE_NAME");
        request.setRoleDescription("New description");
        Permission newPermission = permissionRepo.save(new Permission("PERM1"));
        request.setRolePermissions(Set.of(newPermission));

        Role out = dao.updateById(request, id);
        assertEquals(id, out.getId());
        assertEquals("NEW_ROLE_NAME", out.getRoleName());
        assertEquals("New description", out.getRoleDescription());
        assertEquals(newPermission, out.getRolePermissions().iterator().next().getPermission());

        // partial update: blank and null ignored
        Role partial = new Role();
        partial.setRoleName("PARTIAL_NAME");
        // description left null, permissions empty
        Role outPartial = dao.updateById(partial, id);
        assertEquals("PARTIAL_NAME", outPartial.getRoleName());
        assertEquals(out.getRoleDescription(), outPartial.getRoleDescription());
        assertEquals(out.getRolePermissions(), outPartial.getRolePermissions());

        // non-existing id
        assertThrows(NotFoundException.class, () -> dao.updateById(request, UUID.randomUUID()));
    }

    @Test
    @DisplayName("existsByUniqueProperties returns false for invalid and true when exists")
    @Transactional
    void existsByUniqueProperties() {
        // missing name or desc
        Role r1 = new Role();
        r1.setRoleName(null);
        r1.setRoleDescription("Desc");
        assertFalse(dao.existsByUniqueProperties(r1));

        Role r2 = new Role();
        r2.setRoleName("Name");
        r2.setRoleDescription("  ");
        assertFalse(dao.existsByUniqueProperties(r2));

        // valid but not saved
        Role r3 = new Role();
        r3.setRoleName("UNKNOWN");
        r3.setRoleDescription("Some desc");
        assertFalse(dao.existsByUniqueProperties(r3));

        // saved case
        Role exists = new Role();
        exists.setRoleName(saved.getRoleName());
        exists.setRoleDescription(saved.getRoleDescription());
        assertTrue(dao.existsByUniqueProperties(exists));
    }
}
