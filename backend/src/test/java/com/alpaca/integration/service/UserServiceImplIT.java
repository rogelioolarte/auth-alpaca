package com.alpaca.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.resources.UserProvider;
import com.alpaca.service.impl.UserServiceImpl;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link UserServiceImpl} */
@SpringBootTest
@Transactional
@DisplayName("UserServiceImpl Integration Tests")
class UserServiceImplIT {

    @Autowired private UserServiceImpl service;

    private Instant now;

    @BeforeEach
    void setup() {
        now = Instant.now();
    }

    // -------------------------------------------------------------------------
    // Registration & Saving
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("register: Should encode password and persist user when valid")
    @Transactional
    void register_ShouldPersistUser_WhenValid() {
        // Arrange
        User user = UserProvider.singleTemplate();

        // Act
        User saved = service.save(user);

        // Assert
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isAfter(now);
        // Password should be modified by PasswordManager (assuming Argon2id-like output)
        assertThat(saved.getPassword()).isNotEqualTo("rawPassword");
    }

    @Test
    @DisplayName("register: Should throw BadRequestException when user is null")
    @Transactional
    void register_ShouldThrowBadRequest_WhenUserIsNull() {
        assertThatThrownBy(() -> service.save(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("User cannot be created");
    }

    @Test
    @DisplayName("save: Should act as alias for register")
    @Transactional
    void save_ShouldRegisterUser() {
        // Arrange
        User user = UserProvider.alternativeTemplate();
        user.setCreatedAt(now);

        // Act
        User saved = service.save(user);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(service.existsByEmail(user.getEmail())).isTrue();
    }

    // -------------------------------------------------------------------------
    // Update Logic
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("updateById: Should update user when inputs are valid")
    @Transactional
    void updateById_ShouldReturnUpdatedUser() {
        // Arrange
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);
        User saved = service.save(user);

        saved.setEmail("updated@alpaca.com");

        // Act
        User updated = service.updateById(saved, saved.getId());

        // Assert
        assertThat(updated.getEmail()).isEqualTo("updated@alpaca.com");
    }

    @Test
    @DisplayName("updateById: Should throw BadRequestException when user or UUID is null")
    @Transactional
    void updateById_ShouldThrowBadRequest_WhenInputsAreNull() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> service.updateById(null, id))
                .isInstanceOf(BadRequestException.class);

        User user = UserProvider.singleTemplate();
        assertThatThrownBy(() -> service.updateById(user, null))
                .isInstanceOf(BadRequestException.class);
    }

    // -------------------------------------------------------------------------
    // Email Queries
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("existsByEmail: Should return true if user exists")
    @Transactional
    void existsByEmail_ShouldReturnTrue_WhenExists() {
        // Arrange
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);
        service.save(user);

        // Act & Assert
        assertThat(service.existsByEmail(user.getEmail())).isTrue();
        assertThat(service.existsByEmail("non-existent@alpaca.com")).isFalse();
    }

    @Test
    @DisplayName("findByEmail: Should return user when email matches")
    @Transactional
    void findByEmail_ShouldReturnUser_WhenExists() {
        // Arrange
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);
        User saved = service.save(user);

        // Act
        User found = service.findByEmail(saved.getEmail());

        // Assert
        assertThat(found).isNotNull();
        assertThat(found.getEmail()).isEqualTo(saved.getEmail());
    }

    @Test
    @DisplayName("findByEmail: Should throw UsernameNotFoundException when email is not found")
    @Transactional
    void findByEmail_ShouldThrowNotFound_WhenMissing() {
        assertThatThrownBy(() -> service.findByEmail("ghost@alpaca.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("The email does not match any account");
    }

    @Test
    @DisplayName("findByEmail: Should throw BadRequestException when email is null or blank")
    @Transactional
    void findByEmail_ShouldThrowBadRequest_WhenEmailInvalid() {
        assertThatThrownBy(() -> service.findByEmail(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email must not be null or blank");

        assertThatThrownBy(() -> service.findByEmail("")).isInstanceOf(BadRequestException.class);
    }
}
