package com.alpaca.unit.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.alpaca.entity.Advertiser;
import com.alpaca.entity.Profile;
import com.alpaca.entity.Role;
import com.alpaca.entity.User;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.impl.UserDAOImpl;
import com.alpaca.repository.UserRepo;
import com.alpaca.resources.RoleProvider;
import com.alpaca.resources.UserProvider;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    // --- updateById Tests (The Core of Coverage) ---

    @Test
    @DisplayName("Should throw NotFoundException when updating non-existent user")
    void updateById_WhenUserNotFound_ThrowsException() {
        when(repo.findById(id)).thenReturn(Optional.empty());
        User updateData = new User();

        assertThrows(NotFoundException.class, () -> dao.updateById(updateData, id));
        verify(repo).findById(id);
    }

    @Test
    @DisplayName("Should update all complex fields and primitives when data is different")
    void updateById_WhenAllFieldsAreNew_UpdatesEverything() {
        // Arrange
        User existingUser = new User();
        existingUser.setId(id);
        existingUser.setEmail("old@test.com");
        existingUser.setEnabled(false);
        existingUser.setProfile(null);
        existingUser.setAdvertiser(null);

        User updateData = new User();
        updateData.setEmail("new@test.com");
        updateData.setPassword("newPass");
        updateData.setEnabled(true);
        updateData.setAccountNoLocked(true);
        updateData.setAccountNoExpired(true);
        updateData.setCredentialNoExpired(true);
        updateData.setEmailVerified(true);
        updateData.setGoogleConnected(true);

        // Roles
        Set<Role> roles = Set.of(RoleProvider.singleEntity());
        updateData.setUserRoles(roles);

        // Profile & Advertiser with IDs
        Profile profile = new Profile();
        profile.setId(UUID.randomUUID());
        updateData.setProfile(profile);

        Advertiser advertiser = new Advertiser();
        advertiser.setId(UUID.randomUUID());
        updateData.setAdvertiser(advertiser);

        when(repo.findById(id)).thenReturn(Optional.of(existingUser));
        when(repo.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        User result = dao.updateById(updateData, id);

        // Assert
        assertAll(
                () -> assertEquals("new@test.com", result.getEmail()),
                () -> assertEquals("newPass", result.getPassword()),
                () -> assertTrue(result.isEnabled()),
                () -> assertTrue(result.isAccountNoLocked()),
                () -> assertTrue(result.isAccountNoExpired()),
                () -> assertTrue(result.isCredentialNoExpired()),
                () -> assertTrue(result.isEmailVerified()),
                () -> assertTrue(result.isGoogleConnected()),
                () -> assertEquals(roles.size(), result.getUserRoles().size()),
                () -> assertEquals(profile.getId(), result.getProfile().getId()),
                () -> assertEquals(advertiser.getId(), result.getAdvertiser().getId()));
        verify(repo).save(existingUser);
    }

    @Test
    @DisplayName("Should not update Profile or Advertiser if their IDs are null or identical")
    void updateById_WhenObjectsAreIdenticalOrInvalid_DoesNotUpdate() {
        // Arrange
        UUID profileId = UUID.randomUUID();
        Profile existingProfile = new Profile();
        existingProfile.setId(profileId);

        User existingUser = new User();
        existingUser.setProfile(existingProfile);

        User updateData = new User();
        // Case 1: Null profile in updateData -> no change
        updateData.setProfile(null);
        // Case 2: Profile with null ID -> no change
        updateData.setAdvertiser(new Advertiser());

        when(repo.findById(id)).thenReturn(Optional.of(existingUser));
        when(repo.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        User result = dao.updateById(updateData, id);

        // Assert
        assertEquals(profileId, result.getProfile().getId());
        assertNull(result.getAdvertiser());
        verify(repo).save(existingUser);
    }

    @Test
    @DisplayName("Should skip role update if roles are identical to existing ones")
    void updateById_WhenRolesAreIdentical_SkipsUpdate() {
        Set<Role> roles = Set.of(RoleProvider.singleEntity());
        User existingUser = new User();
        existingUser.setUserRoles(roles);

        User updateData = new User();
        updateData.setUserRoles(new HashSet<>(roles)); // Same content, different instance

        when(repo.findById(id)).thenReturn(Optional.of(existingUser));
        when(repo.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = dao.updateById(updateData, id);

        assertEquals(roles.stream().toList(), result.getRoles());
        verify(repo).save(existingUser);
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
        when(repo.findByEmailWithAuthorities(email)).thenReturn(Optional.of(firstEntity));

        Optional<User> result = dao.findByEmailWithAuthorities(email);

        assertTrue(result.isPresent());
        verify(repo).findByEmailWithAuthorities(email);
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
}
