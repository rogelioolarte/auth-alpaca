package com.alpaca.unit.service;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alpaca.dto.request.PasswordRequestDTO;
import com.alpaca.entity.Role;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.persistence.IUserDAO;
import com.alpaca.resources.provider.RoleProvider;
import com.alpaca.resources.provider.UserProvider;
import com.alpaca.security.manager.PasswordManager;
import com.alpaca.service.impl.UserServiceImpl;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/** Unit tests for {@link UserServiceImpl}. */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private IUserDAO dao;

    @Mock private PasswordManager passwordManager;

    @InjectMocks private UserServiceImpl service;

    private User firstUser;
    private User secondUser;
    private String encodedPassword;

    @BeforeEach
    void setup() {
        firstUser = UserProvider.singleEntity();
        secondUser = UserProvider.alternativeEntity();
        encodedPassword = "encoded-password";
    }

    // --- save ---

    @Test
    void saveShouldThrowBadRequestExceptionWhenUserIsNull() {
        assertThrows(BadRequestException.class, () -> service.save(null));

        verifyNoInteractions(dao);
        verifyNoInteractions(passwordManager);
    }

    @Test
    void saveShouldEncodePasswordAndPersistUserSuccessfully() {
        String rawPassword = firstUser.getPassword();

        when(passwordManager.encodePassword(rawPassword)).thenReturn(encodedPassword);
        when(dao.save(firstUser)).thenReturn(firstUser);

        User result = service.save(firstUser);

        assertNotNull(result);
        assertEquals(firstUser, result);
        assertEquals(encodedPassword, firstUser.getPassword());

        verify(passwordManager).encodePassword(rawPassword);
        verify(dao).save(firstUser);
    }

    // --- updateById ---

    @Test
    void updateByIdShouldThrowBadRequestExceptionWhenUserIsNull() {
        UUID userId = firstUser.getId();

        assertThrows(BadRequestException.class, () -> service.updateById(null, userId));

        verifyNoInteractions(dao);
    }

    @Test
    void updateByIdShouldThrowBadRequestExceptionWhenIdIsNull() {
        assertThrows(BadRequestException.class, () -> service.updateById(firstUser, null));

        verifyNoInteractions(dao);
    }

    @Test
    void updateByIdShouldThrowNotFoundExceptionWhenUserDoesNotExist() {
        UUID userId = firstUser.getId();

        when(dao.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.updateById(firstUser, userId));

        verify(dao).findById(userId);
        verify(dao, never()).save(any(User.class));
    }

    @Test
    void updateByIdShouldUpdatePasswordWhenPasswordIsDifferent() {
        User existingUser = UserProvider.singleEntity();

        User incomingUser = UserProvider.alternativeEntity();
        incomingUser.setRoles(existingUser.getRoles());

        UUID userId = existingUser.getId();

        when(dao.findById(userId)).thenReturn(Optional.of(existingUser));
        when(passwordManager.matches(incomingUser.getPassword(), existingUser.getPassword()))
                .thenReturn(false);
        when(dao.save(existingUser)).thenReturn(existingUser);

        User result = service.updateById(incomingUser, userId);

        assertNotNull(result);
        assertEquals(incomingUser.getPassword(), result.getPassword());

        verify(dao).findById(userId);
        verify(passwordManager).matches(incomingUser.getPassword(), existingUser.getPassword());
        verify(dao).save(existingUser);
    }

    @Test
    void updateByIdShouldNotUpdatePasswordWhenPasswordsMatch() {
        User existingUser = UserProvider.singleEntity();

        User incomingUser = UserProvider.alternativeEntity();
        incomingUser.setRoles(existingUser.getRoles());

        UUID userId = existingUser.getId();
        String originalPassword = existingUser.getPassword();

        when(dao.findById(userId)).thenReturn(Optional.of(existingUser));
        when(passwordManager.matches(incomingUser.getPassword(), existingUser.getPassword()))
                .thenReturn(true);
        when(dao.save(existingUser)).thenReturn(existingUser);

        User result = service.updateById(incomingUser, userId);

        assertNotNull(result);
        assertEquals(originalPassword, result.getPassword());

        verify(passwordManager).matches(incomingUser.getPassword(), originalPassword);
        verify(dao).save(existingUser);
    }

    @Test
    void updateByIdShouldNotUpdatePasswordWhenIncomingPasswordIsBlank() {
        User existingUser = UserProvider.singleEntity();

        User incomingUser = UserProvider.alternativeEntity();
        incomingUser.setPassword(" ");
        incomingUser.setRoles(existingUser.getRoles());

        UUID userId = existingUser.getId();
        String originalPassword = existingUser.getPassword();

        when(dao.findById(userId)).thenReturn(Optional.of(existingUser));
        when(dao.save(existingUser)).thenReturn(existingUser);

        User result = service.updateById(incomingUser, userId);

        assertNotNull(result);
        assertEquals(originalPassword, result.getPassword());

        verify(passwordManager, never()).matches(anyString(), anyString());
        verify(dao).save(existingUser);
    }

    @Test
    void updateByIdShouldNotUpdatePasswordWhenExistingPasswordIsNull() {
        User existingUser = UserProvider.singleEntity();
        existingUser.setPassword(null);

        User incomingUser = UserProvider.alternativeEntity();
        incomingUser.setRoles(existingUser.getRoles());

        UUID userId = existingUser.getId();

        when(dao.findById(userId)).thenReturn(Optional.of(existingUser));
        when(dao.save(existingUser)).thenReturn(existingUser);

        User result = service.updateById(incomingUser, userId);

        assertNotNull(result);
        assertNull(result.getPassword());

        verify(passwordManager, never()).matches(anyString(), anyString());
        verify(dao).save(existingUser);
    }

    @Test
    void updateByIdShouldUpdateRolesWhenRolesAreDifferent() {
        User existingUser = UserProvider.singleEntity();

        User incomingUser = UserProvider.alternativeEntity();

        UUID userId = existingUser.getId();

        when(dao.findById(userId)).thenReturn(Optional.of(existingUser));
        when(passwordManager.matches(incomingUser.getPassword(), existingUser.getPassword()))
                .thenReturn(true);
        when(dao.save(existingUser)).thenReturn(existingUser);

        User result = service.updateById(incomingUser, userId);

        assertNotNull(result);
        assertEquals(incomingUser.getRoles(), result.getRoles());

        verify(dao).save(existingUser);
    }

    @Test
    void updateByIdShouldNotUpdateRolesWhenRolesAreEqual() {
        User existingUser = UserProvider.singleEntity();

        User incomingUser = UserProvider.alternativeEntity();
        incomingUser.setRoles(existingUser.getRoles());

        UUID userId = existingUser.getId();

        when(dao.findById(userId)).thenReturn(Optional.of(existingUser));
        when(passwordManager.matches(incomingUser.getPassword(), existingUser.getPassword()))
                .thenReturn(true);
        when(dao.save(existingUser)).thenReturn(existingUser);

        User result = service.updateById(incomingUser, userId);

        assertNotNull(result);
        assertEquals(existingUser.getRoles(), result.getRoles());

        verify(dao).save(existingUser);
    }

    @Test
    void updateByIdShouldNotUpdateRolesWhenIncomingRolesAreNull() {
        User existingUser = UserProvider.singleEntity();

        User incomingUser = UserProvider.alternativeEntity();
        incomingUser.setRoles(null);

        UUID userId = existingUser.getId();

        when(dao.findById(userId)).thenReturn(Optional.of(existingUser));
        when(passwordManager.matches(incomingUser.getPassword(), existingUser.getPassword()))
                .thenReturn(true);
        when(dao.save(existingUser)).thenReturn(existingUser);

        User result = service.updateById(incomingUser, userId);

        assertNotNull(result);
        assertEquals(existingUser.getRoles(), result.getRoles());

        verify(dao).save(existingUser);
    }

    @Test
    void updateByIdShouldUpdateEmailAndBooleanFlagsSuccessfully() {
        User existingUser = UserProvider.singleEntity();

        User incomingUser = UserProvider.alternativeEntity();
        incomingUser.setRoles(existingUser.getRoles());

        UUID userId = existingUser.getId();

        when(dao.findById(userId)).thenReturn(Optional.of(existingUser));
        when(passwordManager.matches(incomingUser.getPassword(), existingUser.getPassword()))
                .thenReturn(true);
        when(dao.save(existingUser)).thenReturn(existingUser);

        User result = service.updateById(incomingUser, userId);

        assertNotNull(result);
        assertEquals(incomingUser.getEmail(), result.getEmail());
        assertEquals(incomingUser.isEnabled(), result.isEnabled());
        assertEquals(incomingUser.isAccountNonLocked(), result.isAccountNonLocked());
        assertEquals(incomingUser.isAccountNonExpired(), result.isAccountNonExpired());
        assertEquals(incomingUser.isCredentialNonExpired(), result.isCredentialNonExpired());
        assertEquals(incomingUser.isEmailVerified(), result.isEmailVerified());
        assertEquals(incomingUser.isGoogleConnected(), result.isGoogleConnected());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        verify(dao).save(userCaptor.capture());

        assertEquals(incomingUser.getEmail(), userCaptor.getValue().getEmail());
    }

    @Test
    void updateByIdShouldUpdateRolesWhenIncomingRolesAreDifferentAndExistingRolesAreNull() {
        User existingUser = UserProvider.singleEntity();
        existingUser.setRoles(null);

        User incomingUser = UserProvider.alternativeEntity();
        List<Role> roles = RoleProvider.listEntities();
        incomingUser.setRoles(new HashSet<>(roles));

        UUID userId = existingUser.getId();

        when(dao.findById(userId)).thenReturn(Optional.of(existingUser));
        when(passwordManager.matches(incomingUser.getPassword(), existingUser.getPassword()))
                .thenReturn(true);
        when(dao.save(existingUser)).thenReturn(existingUser);

        User result = service.updateById(incomingUser, userId);

        assertNotNull(result);
        assertThat(result.getRoles()).containsExactlyInAnyOrderElementsOf(incomingUser.getRoles());

        verify(dao).findById(userId);
        verify(passwordManager).matches(incomingUser.getPassword(), existingUser.getPassword());
        verify(dao).save(existingUser);
    }

    // --- existsByEmail ---

    @Test
    void existsByEmailShouldReturnTrueWhenUserExists() {
        String email = firstUser.getEmail();

        when(dao.existsByEmail(email)).thenReturn(true);

        boolean result = service.existsByEmail(email);

        assertTrue(result);

        verify(dao).existsByEmail(email);
    }

    @Test
    void existsByEmailShouldReturnFalseWhenUserDoesNotExist() {
        String email = secondUser.getEmail();

        when(dao.existsByEmail(email)).thenReturn(false);

        boolean result = service.existsByEmail(email);

        assertFalse(result);

        verify(dao).existsByEmail(email);
    }

    // --- findByEmail ---

    @Test
    void findByEmailShouldThrowBadRequestExceptionWhenEmailIsNull() {
        assertThrows(BadRequestException.class, () -> service.findByEmail(null));

        verifyNoInteractions(dao);
    }

    @Test
    void findByEmailShouldThrowBadRequestExceptionWhenEmailIsBlank() {
        assertThrows(BadRequestException.class, () -> service.findByEmail(" "));

        verifyNoInteractions(dao);
    }

    @Test
    void findByEmailShouldThrowUsernameNotFoundExceptionWhenUserDoesNotExist() {
        String email = secondUser.getEmail();

        when(dao.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.findByEmail(email));

        verify(dao).findByEmail(email);
    }

    @Test
    void findByEmailShouldReturnUserSuccessfully() {
        String email = firstUser.getEmail();

        when(dao.findByEmail(email)).thenReturn(Optional.of(firstUser));

        User result = service.findByEmail(email);

        assertNotNull(result);
        assertEquals(firstUser, result);

        verify(dao).findByEmail(email);
    }

    // --- changePassword ---

    @Test
    void changePasswordShouldThrowBadRequestExceptionWhenPasswordsDoNotMatch() {
        PasswordRequestDTO requestDTO = new PasswordRequestDTO();
        requestDTO.setNewPassword("new-password");
        requestDTO.setReNewPassword("different-password");

        UserPrincipal principal = new UserPrincipal(firstUser);

        assertThrows(
                BadRequestException.class, () -> service.changePassword(principal, requestDTO));

        verifyNoInteractions(dao);
    }

    @Test
    void changePasswordShouldThrowUsernameNotFoundExceptionWhenUserDoesNotExist() {
        PasswordRequestDTO requestDTO = new PasswordRequestDTO();
        requestDTO.setNewPassword("new-password");
        requestDTO.setReNewPassword("new-password");

        UserPrincipal principal = new UserPrincipal(firstUser);

        when(dao.findById(principal.getUserId())).thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> service.changePassword(principal, requestDTO));

        verify(dao).findById(principal.getUserId());
    }

    @Test
    void
            changePasswordShouldThrowBadRequestExceptionWhenPasswordIsNullAndUserIsNotGoogleConnected() {
        User existingUser = UserProvider.singleEntity();
        existingUser.setPassword(null);
        existingUser.setGoogleConnected(false);

        PasswordRequestDTO requestDTO = new PasswordRequestDTO();
        requestDTO.setNewPassword("new-password");
        requestDTO.setReNewPassword("new-password");

        UserPrincipal principal = new UserPrincipal(existingUser);

        when(dao.findById(principal.getUserId())).thenReturn(Optional.of(existingUser));

        assertThrows(
                BadRequestException.class, () -> service.changePassword(principal, requestDTO));

        verify(dao).findById(principal.getUserId());
        verify(dao, never()).save(any(User.class));
    }

    @Test
    void changePasswordShouldSetPasswordWhenPasswordIsNullAndUserIsGoogleConnected() {
        User existingUser = UserProvider.singleEntity();
        existingUser.setPassword(null);
        existingUser.setGoogleConnected(true);

        PasswordRequestDTO requestDTO = new PasswordRequestDTO();
        requestDTO.setNewPassword("new-password");
        requestDTO.setReNewPassword("new-password");

        UserPrincipal principal = new UserPrincipal(existingUser);

        when(dao.findById(principal.getUserId())).thenReturn(Optional.of(existingUser));
        when(passwordManager.encodePassword(requestDTO.getNewPassword()))
                .thenReturn(encodedPassword);
        when(dao.save(existingUser)).thenReturn(existingUser);

        service.changePassword(principal, requestDTO);

        assertEquals(encodedPassword, existingUser.getPassword());

        verify(passwordManager).encodePassword(requestDTO.getNewPassword());
        verify(dao).save(existingUser);
    }

    @Test
    void changePasswordShouldThrowBadRequestExceptionWhenCurrentPasswordIsBlank() {
        PasswordRequestDTO requestDTO = new PasswordRequestDTO();
        requestDTO.setCurrentPassword(" ");
        requestDTO.setNewPassword("new-password");
        requestDTO.setReNewPassword("new-password");

        User existingUser = UserProvider.singleEntity();

        UserPrincipal principal = new UserPrincipal(existingUser);

        when(dao.findById(principal.getUserId())).thenReturn(Optional.of(existingUser));

        assertThrows(
                BadRequestException.class, () -> service.changePassword(principal, requestDTO));

        verify(dao).findById(principal.getUserId());
    }

    @Test
    void changePasswordShouldThrowBadRequestExceptionWhenCurrentPasswordDoesNotMatch() {
        PasswordRequestDTO requestDTO = new PasswordRequestDTO();
        requestDTO.setCurrentPassword("invalid-password");
        requestDTO.setNewPassword("new-password");
        requestDTO.setReNewPassword("new-password");

        User existingUser = UserProvider.singleEntity();

        UserPrincipal principal = new UserPrincipal(existingUser);

        when(dao.findById(principal.getUserId())).thenReturn(Optional.of(existingUser));
        when(passwordManager.matches(requestDTO.getCurrentPassword(), existingUser.getPassword()))
                .thenReturn(false);

        assertThrows(
                BadRequestException.class, () -> service.changePassword(principal, requestDTO));

        verify(passwordManager)
                .matches(requestDTO.getCurrentPassword(), existingUser.getPassword());
    }

    @Test
    void changePasswordShouldThrowBadRequestExceptionWhenNewPasswordMatchesCurrentPassword() {
        PasswordRequestDTO requestDTO = new PasswordRequestDTO();
        requestDTO.setCurrentPassword("current-password");
        requestDTO.setNewPassword("new-password");
        requestDTO.setReNewPassword("new-password");

        User existingUser = UserProvider.singleEntity();

        UserPrincipal principal = new UserPrincipal(existingUser);

        when(dao.findById(principal.getUserId())).thenReturn(Optional.of(existingUser));
        when(passwordManager.matches(requestDTO.getCurrentPassword(), existingUser.getPassword()))
                .thenReturn(true);
        when(passwordManager.matches(requestDTO.getNewPassword(), existingUser.getPassword()))
                .thenReturn(true);

        assertThrows(
                BadRequestException.class, () -> service.changePassword(principal, requestDTO));

        verify(passwordManager).matches(requestDTO.getNewPassword(), existingUser.getPassword());
    }

    @Test
    void changePasswordShouldUpdatePasswordSuccessfully() {
        PasswordRequestDTO requestDTO = new PasswordRequestDTO();
        requestDTO.setCurrentPassword("current-password");
        requestDTO.setNewPassword("new-password");
        requestDTO.setReNewPassword("new-password");

        User existingUser = UserProvider.singleEntity();

        UserPrincipal principal = new UserPrincipal(existingUser);

        when(dao.findById(principal.getUserId())).thenReturn(Optional.of(existingUser));
        when(passwordManager.matches(requestDTO.getCurrentPassword(), existingUser.getPassword()))
                .thenReturn(true);
        when(passwordManager.matches(requestDTO.getNewPassword(), existingUser.getPassword()))
                .thenReturn(false);
        when(passwordManager.encodePassword(requestDTO.getNewPassword()))
                .thenReturn(encodedPassword);
        when(dao.save(existingUser)).thenReturn(existingUser);

        service.changePassword(principal, requestDTO);

        assertEquals(encodedPassword, existingUser.getPassword());

        verify(passwordManager).encodePassword(requestDTO.getNewPassword());
        verify(dao).save(existingUser);
    }
}
