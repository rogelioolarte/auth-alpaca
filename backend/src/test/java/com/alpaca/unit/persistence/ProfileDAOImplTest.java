package com.alpaca.unit.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alpaca.entity.Profile;
import com.alpaca.entity.User;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.impl.ProfileDAOImpl;
import com.alpaca.repository.ProfileRepo;
import com.alpaca.resources.ProfileProvider;
import com.alpaca.resources.UserProvider;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link ProfileDAOImpl}. */
@ExtendWith(MockitoExtension.class)
class ProfileDAOImplTest {

    @Mock private ProfileRepo repo;

    @InjectMocks private ProfileDAOImpl dao;

    private Profile firstEntity;
    private final UUID profileId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        firstEntity = ProfileProvider.singleEntity();
        firstEntity.setId(profileId);
    }

    // --- updateById Tests ---

    @Test
    @DisplayName("Should throw NotFoundException when profile to update does not exist")
    void updateById_WhenNotFound_ThrowsException() {
        when(repo.findById(profileId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> dao.updateById(firstEntity, profileId));
        verify(repo).findById(profileId);
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("Should update all text fields and User when valid new data is provided")
    void updateById_WhenValidData_UpdatesAllFields() {
        // Arrange
        Profile existingProfile = ProfileProvider.singleEntity();
        existingProfile.setId(profileId);
        existingProfile.setUser(null); // Case: current user is null

        Profile updateData = new Profile();
        updateData.setFirstName("NewName");
        updateData.setLastName("NewLastName");
        updateData.setAddress("New Address 123");
        updateData.setAvatarUrl("https://new-avatar.com/1.png");

        User newUser = UserProvider.singleEntity();
        newUser.setId(UUID.randomUUID());
        updateData.setUser(newUser);

        when(repo.findById(profileId)).thenReturn(Optional.of(existingProfile));
        when(repo.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Profile result = dao.updateById(updateData, profileId);

        // Assert
        assertAll(
                () -> assertEquals("NewName", result.getFirstName()),
                () -> assertEquals("NewLastName", result.getLastName()),
                () -> assertEquals("New Address 123", result.getAddress()),
                () -> assertEquals("https://new-avatar.com/1.png", result.getAvatarUrl()),
                () -> assertEquals(newUser.getId(), result.getUser().getId()));
        verify(repo).save(existingProfile);
    }

    @Test
    @DisplayName("Should not update fields when input is null or blank")
    void updateById_WhenInputIsNullOrEmpty_DoesNotUpdateFields() {
        // Arrange
        String originalName = "Original";
        Profile existingProfile = new Profile();
        existingProfile.setId(profileId);
        existingProfile.setFirstName(originalName);

        Profile updateData = new Profile();
        updateData.setFirstName(null);
        updateData.setLastName("");
        updateData.setAddress("   ");
        updateData.setAvatarUrl(null);

        when(repo.findById(profileId)).thenReturn(Optional.of(existingProfile));
        when(repo.save(any(Profile.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Profile result = dao.updateById(updateData, profileId);

        // Assert
        assertEquals(originalName, result.getFirstName());
        verify(repo).save(existingProfile);
    }

    @Test
    @DisplayName("Should skip User update if provided User ID is identical to existing one")
    void updateById_WhenUserIdIsIdentical_SkipsUserUpdate() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User existingUser = new User();
        existingUser.setId(userId);

        Profile existingProfile = new Profile();
        existingProfile.setId(profileId);
        existingProfile.setUser(existingUser);

        Profile updateData = new Profile();
        User sameUser = new User();
        sameUser.setId(userId);
        updateData.setUser(sameUser);

        when(repo.findById(profileId)).thenReturn(Optional.of(existingProfile));
        when(repo.save(any(Profile.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Profile result = dao.updateById(updateData, profileId);

        // Assert
        assertSame(
                existingUser,
                result.getUser(),
                "Should keep the same object reference if IDs match");
        verify(repo).save(existingProfile);
    }

    // --- existsByUniqueProperties Tests ---

    @Test
    @DisplayName("Should return false when profile has no user or user has no ID")
    void existsByUniqueProperties_WhenUserOrIdIsNull_ReturnsFalse() {
        Profile profileNoUser = new Profile();
        assertFalse(dao.existsByUniqueProperties(profileNoUser));

        Profile profileNoId = new Profile();
        profileNoId.setUser(new User());
        assertFalse(dao.existsByUniqueProperties(profileNoId));
    }

    @Test
    @DisplayName("Should return true when repository counts at least one profile for the user ID")
    void existsByUniqueProperties_WhenUserExists_ReturnsTrue() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        Profile profile = new Profile();
        profile.setUser(user);

        when(repo.countByUserId(userId)).thenReturn(1L);

        assertTrue(dao.existsByUniqueProperties(profile));
        verify(repo).countByUserId(userId);
    }

    @Test
    @DisplayName("Should return false when repository counts zero profiles for the user ID")
    void existsByUniqueProperties_WhenUserDoesNotExist_ReturnsFalse() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        Profile profile = new Profile();
        profile.setUser(user);

        when(repo.countByUserId(userId)).thenReturn(0L);

        assertFalse(dao.existsByUniqueProperties(profile));
        verify(repo).countByUserId(userId);
    }

    @Test
    @DisplayName("existsAllByIds: Should compare input size with repository count")
    void existsAllByIds_Coverage() {
        List<UUID> ids = ProfileProvider.listEntities().stream().map(Profile::getId).toList();
        when(repo.countByIds(ids)).thenReturn((long) ids.size());
        assertThat(dao.existsAllByIds(ids)).isTrue();

        when(repo.countByIds(ids)).thenReturn(0L);
        assertThat(dao.existsAllByIds(ids)).isFalse();
    }
}
