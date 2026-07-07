package com.alpaca.unit.persistence;

import com.alpaca.entity.User;
import com.alpaca.persistence.impl.UserDAOImpl;
import com.alpaca.repository.UserRepo;
import com.alpaca.resources.provider.UserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Unit tests for {@link UserDAOImpl} implementation. */
@ExtendWith(MockitoExtension.class)
class UserDAOImplTest {

    @Mock private UserRepo repo;

    @InjectMocks private UserDAOImpl dao;

    private User firstEntity;
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setup() {
        firstEntity = UserProvider.singleEntity();
        firstEntity.setId(id);
    }

    // --- findByEmail Tests ---

    @Test
    @DisplayName("Should return empty Optional when email is null or blank")
    void findByEmail_WhenInvalidEmail_ReturnsEmpty() {
        assertTrue(dao.findByEmail(null).isEmpty());
        assertTrue(dao.findByEmail("").isEmpty());
        assertTrue(dao.findByEmail("   ").isEmpty());
    }

    @Test
    @DisplayName("Should return Optional of User when email exists")
    void findByEmail_WhenEmailExists_ReturnsUser() {
        String email = "test@alpaca.com";
        when(repo.findByEmail(email)).thenReturn(Optional.of(firstEntity));

        Optional<User> result = dao.findByEmail(email);

        assertTrue(result.isPresent());
        assertEquals(firstEntity, result.get());
        verify(repo).findByEmail(email);
    }

    // --- existsByUniqueProperties Tests ---

    @Test
    @DisplayName("Should delegate to existsByEmail using user's email")
    void existsByUniqueProperties_ValidUser_ReturnsRepoResult() {
        String email = "unique@test.com";
        User user = new User();
        user.setEmail(email);

        when(repo.existsByEmail(email)).thenReturn(true);

        assertTrue(dao.existsByUniqueProperties(user));
        verify(repo).existsByEmail(email);
    }

    // --- existsByEmail Tests ---

    @Test
    @DisplayName("Should return false for null, empty or blank email check")
    void existsByEmail_WhenInvalidInput_ReturnsFalse() {
        assertFalse(dao.existsByEmail(null));
        assertFalse(dao.existsByEmail(""));
        assertFalse(dao.existsByEmail("   "));
        verifyNoInteractions(repo);
    }

    @Test
    @DisplayName("Should return repo value for valid email check")
    void existsByEmail_WhenValidEmail_CallsRepo() {
        when(repo.existsByEmail("exists@test.com")).thenReturn(true);
        assertTrue(dao.existsByEmail("exists@test.com"));
    }

    // --- Specialized Fetching Tests ---

    @Test
    @DisplayName("Should find user with authorities by email")
    void findByEmailWithAuthorities_ReturnsUser() {
        String email = "auth@test.com";
        when(repo.findByEmail(email)).thenReturn(Optional.of(firstEntity));

        Optional<User> result = dao.findByEmail(email);

        assertTrue(result.isPresent());
        verify(repo).findByEmail(email);
    }

    @Test
    @DisplayName("Should find user by ID with pessimistic locking")
    void lockFindUserById_ReturnsUser() {
        when(repo.lockFindUserById(id)).thenReturn(Optional.of(firstEntity));

        Optional<User> result = dao.lockFindUserById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
        verify(repo).lockFindUserById(id);
    }

    @Test
    @DisplayName("existsAllByIds: Should compare input size with repository count")
    void existsAllByIds_Coverage() {
        List<UUID> ids = UserProvider.listEntities().stream().map(User::getId).toList();
        when(repo.countEntitiesIds(ids)).thenReturn((long) ids.size());
        assertThat(dao.existsAllByIds(ids)).isTrue();

        when(repo.countEntitiesIds(ids)).thenReturn(0L);
        assertThat(dao.existsAllByIds(ids)).isFalse();
    }
}
