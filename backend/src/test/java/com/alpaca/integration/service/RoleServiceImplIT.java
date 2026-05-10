package com.alpaca.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.alpaca.entity.Role;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.resources.RoleProvider;
import com.alpaca.service.impl.RoleServiceImpl;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link RoleServiceImpl} */
@SpringBootTest
@Transactional
@DisplayName("RoleServiceImpl Integration Tests")
class RoleServiceImplIT {

    @Autowired private RoleServiceImpl service;

    private Instant now;

    @BeforeEach
    void setup() {
        now = Instant.now();
    }

    // -------------------------------------------------------------------------
    // getUserRoles
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getUserRoles: Should throw NotFoundException when 'USER' role is missing")
    @Transactional
    void getUserRoles_ShouldThrowNotFound_WhenDefaultRoleMissing() {
        assertThatThrownBy(() -> service.getUserRoles())
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Role with Name USER not found");
    }

    @Test
    @DisplayName("getUserRoles: Should return a set containing the 'USER' role when it exists")
    @Transactional
    void getUserRoles_ShouldReturnSet_WhenUserRoleExists() {
        // Arrange
        Role userRole = RoleProvider.singleTemplate();
        userRole.setName("USER");
        userRole.setCreatedAt(now); // Critical: Manual audit setup
        service.save(userRole);

        // Act
        Set<Role> result = service.getUserRoles();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.iterator().next().getName()).isEqualTo("USER");
    }

    // -------------------------------------------------------------------------
    // findByRoleName
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findByRoleName: Should throw BadRequestException when name is null or blank")
    @Transactional
    void findByRoleName_ShouldThrowBadRequest_WhenNameInvalid() {
        assertThatThrownBy(() -> service.findByRoleName(null))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> service.findByRoleName("   "))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("findByRoleName: Should throw NotFoundException when role name does not exist")
    @Transactional
    void findByRoleName_ShouldThrowNotFound_WhenRoleDoesNotExist() {
        String nonExistentName = "SUPER_ADMIN_99";
        assertThatThrownBy(() -> service.findByRoleName(nonExistentName))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Role with Name " + nonExistentName + " not found");
    }

    @Test
    @DisplayName("findByRoleName: Should return the role when a matching name is found")
    @Transactional
    void findByRoleName_ShouldReturnRole_WhenExists() {
        // Arrange
        Role template = RoleProvider.alternativeTemplate();
        template.setCreatedAt(now);
        Role saved = service.save(template);

        // Act
        Role found = service.findByRoleName(saved.getName());

        // Assert
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo(saved.getName());
        assertThat(found.getId()).isEqualTo(saved.getId());
    }

    // -------------------------------------------------------------------------
    // Inherited Generic CRUD Coverage (Examples)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save: Should persist role and set audit fields correctly")
    @Transactional
    void save_ShouldPersistRole() {
        // Arrange
        Role role = RoleProvider.singleTemplate();

        // Act
        Role saved = service.save(role);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isAfter(now);
    }

    @Test
    @DisplayName("deleteById: Should remove role and verify non-existence")
    @Transactional
    void deleteById_ShouldRemoveRole() {
        // Arrange
        Role role = RoleProvider.singleTemplate();
        role.setCreatedAt(now);
        Role saved = service.save(role);

        // Act
        service.deleteById(saved.getId());

        // Assert
        assertThat(service.existsById(saved.getId())).isFalse();
    }
}
