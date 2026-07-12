package com.alpaca.unit.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alpaca.entity.Profile;
import com.alpaca.entity.User;
import com.alpaca.persistence.impl.ProfileDAOImpl;
import com.alpaca.repository.ProfileRepo;
import com.alpaca.resources.provider.ProfileProvider;
import java.util.List;
import java.util.UUID;
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
    @DisplayName(
            "Should return true when repository counts at least one profile for the user Email")
    void existsByUniqueProperties_WhenUserExists_ReturnsTrue() {
        UUID userId = UUID.randomUUID();
        String email = "test@test.com";
        User user = new User();
        user.setId(userId);
        user.setEmail(email);
        Profile profile = new Profile();
        profile.setUser(user);

        when(repo.countByUserEmail(email)).thenReturn(1L);

        assertTrue(dao.existsByUniqueProperties(profile));
        verify(repo).countByUserEmail(email);
    }

    @Test
    @DisplayName("Should return false when repository counts zero profiles for the user ID")
    void existsByUniqueProperties_WhenUserDoesNotExist_ReturnsFalse() {
        UUID userId = UUID.randomUUID();
        String email = "test@test.com";
        User user = new User();
        user.setEmail(email);
        user.setId(userId);
        Profile profile = new Profile();
        profile.setUser(user);

        when(repo.countByUserEmail(email)).thenReturn(0L);

        assertFalse(dao.existsByUniqueProperties(profile));
        verify(repo).countByUserEmail(email);
    }

    @Test
    @DisplayName("existsAllByIds: Should compare input size with repository count")
    void existsAllByIds_Coverage() {
        List<UUID> ids = ProfileProvider.listEntities().stream().map(Profile::getId).toList();
        when(repo.countEntitiesIds(ids)).thenReturn((long) ids.size());
        assertThat(dao.existsAllByIds(ids)).isTrue();

        when(repo.countEntitiesIds(ids)).thenReturn(0L);
        assertThat(dao.existsAllByIds(ids)).isFalse();
    }
}
