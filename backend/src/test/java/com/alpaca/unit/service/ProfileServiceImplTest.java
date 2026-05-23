package com.alpaca.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alpaca.entity.Profile;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IProfileDAO;
import com.alpaca.resources.ProfileProvider;
import com.alpaca.resources.UserProvider;
import com.alpaca.service.impl.ProfileServiceImpl;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link ProfileServiceImpl}. */
@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock private IProfileDAO dao;

    @InjectMocks private ProfileServiceImpl service;

    private Profile firstProfile;

    @BeforeEach
    void setup() {
        firstProfile = ProfileProvider.singleEntity();
    }

    // --- updateById ---

    @Test
    void updateByIdShouldThrowBadRequestExceptionWhenProfileIsNull() {
        UUID profileId = firstProfile.getId();

        assertThrows(BadRequestException.class, () -> service.updateById(null, profileId));

        verifyNoInteractions(dao);
    }

    @Test
    void updateByIdShouldThrowBadRequestExceptionWhenIdIsNull() {
        assertThrows(BadRequestException.class, () -> service.updateById(firstProfile, null));

        verifyNoInteractions(dao);
    }

    @Test
    void updateByIdShouldThrowNotFoundExceptionWhenProfileDoesNotExist() {
        UUID profileId = firstProfile.getId();

        when(dao.findById(profileId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.updateById(firstProfile, profileId));

        verify(dao).findById(profileId);
        verify(dao, never()).save(any(Profile.class));
    }

    @Test
    void updateByIdShouldUpdateUserWhenUserIsDifferent() {
        Profile existingProfile = ProfileProvider.singleEntity();
        Profile incomingProfile = ProfileProvider.alternativeEntity();

        UUID profileId = existingProfile.getId();

        when(dao.findById(profileId)).thenReturn(Optional.of(existingProfile));
        when(dao.save(existingProfile)).thenReturn(existingProfile);

        Profile result = service.updateById(incomingProfile, profileId);

        assertNotNull(result);
        assertEquals(incomingProfile.getUser(), result.getUser());

        verify(dao).findById(profileId);

        ArgumentCaptor<Profile> profileCaptor = ArgumentCaptor.forClass(Profile.class);

        verify(dao).save(profileCaptor.capture());

        assertEquals(incomingProfile.getUser().getId(), profileCaptor.getValue().getUser().getId());
    }

    @Test
    void updateByIdShouldNotUpdateUserWhenIncomingUserIsNull() {
        Profile existingProfile = ProfileProvider.singleEntity();
        Profile incomingProfile = ProfileProvider.alternativeEntity();

        incomingProfile.setUser(null);

        UUID profileId = existingProfile.getId();
        User existingUser = existingProfile.getUser();

        when(dao.findById(profileId)).thenReturn(Optional.of(existingProfile));
        when(dao.save(existingProfile)).thenReturn(existingProfile);

        Profile result = service.updateById(incomingProfile, profileId);

        assertNotNull(result);
        assertEquals(existingUser, result.getUser());

        verify(dao).save(existingProfile);
    }

    @Test
    void updateByIdShouldNotUpdateUserWhenIncomingUserIdIsNull() {
        Profile existingProfile = ProfileProvider.singleEntity();
        Profile incomingProfile = ProfileProvider.alternativeEntity();

        User userWithoutId = UserProvider.alternativeEntity();
        userWithoutId.setId(null);

        incomingProfile.setUser(userWithoutId);

        UUID profileId = existingProfile.getId();
        User existingUser = existingProfile.getUser();

        when(dao.findById(profileId)).thenReturn(Optional.of(existingProfile));
        when(dao.save(existingProfile)).thenReturn(existingProfile);

        Profile result = service.updateById(incomingProfile, profileId);

        assertNotNull(result);
        assertEquals(existingUser, result.getUser());

        verify(dao).save(existingProfile);
    }

    @Test
    void updateByIdShouldNotUpdateUserWhenUserIdIsEqual() {
        User user = UserProvider.singleEntity();
        Profile existingProfile = ProfileProvider.singleEntity();
        existingProfile.setUser(user);
        Profile incomingProfile = ProfileProvider.alternativeEntity();

        User sameUser = UserProvider.alternativeEntity();
        sameUser.setId(existingProfile.getUser().getId());

        incomingProfile.setUser(sameUser);

        UUID profileId = existingProfile.getId();
        User existingUser = existingProfile.getUser();

        when(dao.findById(profileId)).thenReturn(Optional.of(existingProfile));
        when(dao.save(existingProfile)).thenReturn(existingProfile);

        Profile result = service.updateById(incomingProfile, profileId);

        assertNotNull(result);
        assertEquals(existingUser, result.getUser());

        verify(dao).save(existingProfile);
    }

    @Test
    void updateByIdShouldUpdateUserWhenExistingUserIsNull() {
        Profile existingProfile = ProfileProvider.singleEntity();
        existingProfile.setUser(null);

        Profile incomingProfile = ProfileProvider.alternativeEntity();

        UUID profileId = existingProfile.getId();

        when(dao.findById(profileId)).thenReturn(Optional.of(existingProfile));
        when(dao.save(existingProfile)).thenReturn(existingProfile);

        Profile result = service.updateById(incomingProfile, profileId);

        assertNotNull(result);
        assertEquals(incomingProfile.getUser(), result.getUser());

        verify(dao).save(existingProfile);
    }

    @Test
    void updateByIdShouldUpdateAllTextFieldsSuccessfully() {
        Profile existingProfile = ProfileProvider.singleEntity();
        Profile incomingProfile = ProfileProvider.alternativeEntity();

        incomingProfile.setUser(existingProfile.getUser());

        UUID profileId = existingProfile.getId();

        when(dao.findById(profileId)).thenReturn(Optional.of(existingProfile));
        when(dao.save(existingProfile)).thenReturn(existingProfile);

        Profile result = service.updateById(incomingProfile, profileId);

        assertNotNull(result);
        assertEquals(incomingProfile.getFirstName(), result.getFirstName());
        assertEquals(incomingProfile.getLastName(), result.getLastName());
        assertEquals(incomingProfile.getAddress(), result.getAddress());
        assertEquals(incomingProfile.getAvatarUrl(), result.getAvatarUrl());

        verify(dao).save(existingProfile);
    }

    @Test
    void updateByIdShouldNotUpdateTextFieldsWhenIncomingValuesAreBlank() {
        Profile existingProfile = ProfileProvider.singleEntity();

        String originalFirstName = existingProfile.getFirstName();
        String originalLastName = existingProfile.getLastName();
        String originalAddress = existingProfile.getAddress();
        String originalAvatarUrl = existingProfile.getAvatarUrl();

        Profile incomingProfile = ProfileProvider.alternativeEntity();

        incomingProfile.setUser(existingProfile.getUser());
        incomingProfile.setFirstName(" ");
        incomingProfile.setLastName(" ");
        incomingProfile.setAddress(" ");
        incomingProfile.setAvatarUrl(" ");

        UUID profileId = existingProfile.getId();

        when(dao.findById(profileId)).thenReturn(Optional.of(existingProfile));
        when(dao.save(existingProfile)).thenReturn(existingProfile);

        Profile result = service.updateById(incomingProfile, profileId);

        assertNotNull(result);
        assertEquals(originalFirstName, result.getFirstName());
        assertEquals(originalLastName, result.getLastName());
        assertEquals(originalAddress, result.getAddress());
        assertEquals(originalAvatarUrl, result.getAvatarUrl());

        verify(dao).save(existingProfile);
    }

    @Test
    void updateByIdShouldNotUpdateTextFieldsWhenValuesAreEqual() {
        Profile existingProfile = ProfileProvider.singleEntity();

        Profile incomingProfile = ProfileProvider.alternativeEntity();

        incomingProfile.setUser(existingProfile.getUser());
        incomingProfile.setFirstName(existingProfile.getFirstName());
        incomingProfile.setLastName(existingProfile.getLastName());
        incomingProfile.setAddress(existingProfile.getAddress());
        incomingProfile.setAvatarUrl(existingProfile.getAvatarUrl());

        UUID profileId = existingProfile.getId();

        when(dao.findById(profileId)).thenReturn(Optional.of(existingProfile));
        when(dao.save(existingProfile)).thenReturn(existingProfile);

        Profile result = service.updateById(incomingProfile, profileId);

        assertNotNull(result);
        assertEquals(existingProfile.getFirstName(), result.getFirstName());
        assertEquals(existingProfile.getLastName(), result.getLastName());
        assertEquals(existingProfile.getAddress(), result.getAddress());
        assertEquals(existingProfile.getAvatarUrl(), result.getAvatarUrl());

        verify(dao).save(existingProfile);
    }
}
