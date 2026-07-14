package com.alpaca.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.alpaca.dto.request.PasswordRequestDTO;
import com.alpaca.entity.Profile;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.persistence.IProfileDAO;
import com.alpaca.persistence.IUserDAO;
import com.alpaca.resources.provider.ProfileProvider;
import com.alpaca.resources.provider.UserProvider;
import com.alpaca.security.manager.PasswordManager;
import com.alpaca.security.oauth2.userinfo.OAuth2UserInfo;
import com.alpaca.service.IRoleService;
import com.alpaca.service.impl.UserServiceImpl;
import java.util.HashSet;
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
    @Mock private IProfileDAO profileService;
    @Mock private IRoleService roleService;
    @Mock private PasswordManager passwordManager;
    @Mock private OAuth2UserInfo userInfo;

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

    @Test
    void saveShouldThrowBadRequestExceptionWhenUserIsNull() {
        assertThatThrownBy(() -> service.save(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("User cannot be created");

        verifyNoInteractions(dao, passwordManager);
    }

    @Test
    void saveShouldEncodePasswordAndPersistUserSuccessfully() {
        String rawPassword = firstUser.getPassword();

        when(passwordManager.encodePassword(rawPassword)).thenReturn(encodedPassword);
        when(dao.save(firstUser)).thenReturn(firstUser);

        User result = service.save(firstUser);

        assertThat(result).isSameAs(firstUser);
        assertThat(result.getPassword()).isEqualTo(encodedPassword);

        verify(passwordManager).encodePassword(rawPassword);
        verify(dao).save(firstUser);
    }

    @Test
    void registerOAuth2UserShouldReturnExistingUserWhenEmailAlreadyExists() {
        String email = firstUser.getEmail();
        firstUser.setGoogleConnected(true);

        when(userInfo.getEmail()).thenReturn(email);
        when(userInfo.getEmailVerified()).thenReturn(firstUser.isEmailVerified());
        when(dao.existsByEmail(email)).thenReturn(true);
        when(dao.findByEmail(email)).thenReturn(Optional.of(firstUser));

        User result = service.registerOAuth2User(userInfo);

        assertThat(result).isSameAs(firstUser);

        verify(dao).existsByEmail(email);
        verify(dao).findByEmail(email);
        verify(dao, never()).save(any(User.class));
        verifyNoInteractions(profileService, roleService);
    }

    @Test
    void registerOAuth2UserShouldCreateUserAndProfileWhenEmailDoesNotExist() {
        String email = firstUser.getEmail();
        Profile profile = ProfileProvider.singleEntity();
        firstUser.setProfile(profile);
        String firstName = firstUser.getProfile().getFirstName();
        String lastName = firstUser.getProfile().getLastName();
        String imageUrl = firstUser.getProfile().getAvatarUrl();
        boolean emailVerified = firstUser.isEmailVerified();

        when(userInfo.getEmail()).thenReturn(email);
        when(userInfo.getFirstName()).thenReturn(firstName);
        when(userInfo.getLastName()).thenReturn(lastName);
        when(userInfo.getImageUrl()).thenReturn(imageUrl);
        when(userInfo.getEmailVerified()).thenReturn(emailVerified);
        when(dao.existsByEmail(email)).thenReturn(false);
        when(roleService.getUserRoles()).thenReturn(new HashSet<>(firstUser.getRoles()));
        when(dao.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(profileService.save(any(Profile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = service.registerOAuth2User(userInfo);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<Profile> profileCaptor = ArgumentCaptor.forClass(Profile.class);

        verify(dao).save(userCaptor.capture());
        verify(profileService).save(profileCaptor.capture());

        User savedUser = userCaptor.getValue();
        Profile savedProfile = profileCaptor.getValue();

        assertThat(result).isSameAs(savedUser);
        assertThat(savedUser.getEmail()).isEqualTo(email);
        assertThat(savedUser.isEmailVerified()).isEqualTo(emailVerified);
        assertThat(savedUser.isGoogleConnected()).isTrue();
        assertThat(savedUser.getRoles()).isEqualTo(firstUser.getRoles());
        assertThat(savedProfile.getFirstName()).isEqualTo(firstName);
        assertThat(savedProfile.getLastName()).isEqualTo(lastName);
        assertThat(savedProfile.getAvatarUrl()).isEqualTo(imageUrl);
        assertThat(savedProfile.getUser()).isSameAs(savedUser);

        verify(dao).existsByEmail(email);
        verify(roleService).getUserRoles();
    }

    @Test
    void checkExistingUserShouldThrowUnauthorizedExceptionWhenUserIsNotAllowed() {
        firstUser.setEnabled(false);
        boolean emailVerified = firstUser.isEmailVerified();

        UnauthorizedException exception =
                assertThrows(
                        UnauthorizedException.class,
                        () -> service.checkExistingUser(firstUser, emailVerified));

        assertEquals("The account has been deactivated or blocked", exception.getReason());
        verifyNoInteractions(dao);
    }

    @Test
    void checkExistingUserShouldConnectGoogleWhenNotConnected() {
        firstUser.setGoogleConnected(false);
        boolean emailVerified = firstUser.isEmailVerified();

        when(dao.save(firstUser)).thenReturn(firstUser);

        User result = service.checkExistingUser(firstUser, emailVerified);

        assertThat(result).isSameAs(firstUser);
        assertThat(result.isGoogleConnected()).isTrue();

        verify(dao).save(firstUser);
    }

    @Test
    void checkExistingUserShouldUpdateEmailVerificationWhenDifferent() {
        firstUser.setGoogleConnected(true);
        boolean emailVerified = !firstUser.isEmailVerified();

        when(dao.save(firstUser)).thenReturn(firstUser);

        User result = service.checkExistingUser(firstUser, emailVerified);

        assertThat(result).isSameAs(firstUser);
        assertThat(result.isEmailVerified()).isEqualTo(emailVerified);

        verify(dao).save(firstUser);
    }

    @Test
    void checkExistingUserShouldSaveTwiceWhenGoogleConnectionAndEmailVerificationChange() {
        firstUser.setGoogleConnected(false);
        boolean emailVerified = !firstUser.isEmailVerified();

        when(dao.save(firstUser)).thenReturn(firstUser);

        User result = service.checkExistingUser(firstUser, emailVerified);

        assertThat(result).isSameAs(firstUser);
        assertThat(result.isGoogleConnected()).isTrue();
        assertThat(result.isEmailVerified()).isEqualTo(emailVerified);

        verify(dao, times(2)).save(firstUser);
    }

    @Test
    void checkExistingUserShouldNotSaveWhenNoChangesAreRequired() {
        firstUser.setGoogleConnected(true);
        boolean emailVerified = firstUser.isEmailVerified();

        User result = service.checkExistingUser(firstUser, emailVerified);

        assertThat(result).isSameAs(firstUser);

        verifyNoInteractions(dao);
    }

    @Test
    void updateByIdShouldThrowBadRequestExceptionWhenUserIsNull() {
        UUID userId = firstUser.getId();

        assertThatThrownBy(() -> service.updateById(null, userId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("User cannot be created");

        verifyNoInteractions(dao);
    }

    @Test
    void updateByIdShouldThrowBadRequestExceptionWhenIdIsNull() {
        assertThatThrownBy(() -> service.updateById(firstUser, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("User cannot be created");

        verifyNoInteractions(dao);
    }

    @Test
    void updateByIdShouldThrowNotFoundExceptionWhenUserDoesNotExist() {
        UUID userId = firstUser.getId();

        when(dao.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateById(firstUser, userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User with ID %s not found", userId);

        verify(dao).findById(userId);
        verify(dao, never()).save(any(User.class));
    }

    @Test
    void updateByIdShouldUpdateMutablePropertiesSuccessfully() {
        User existingUser = firstUser;
        User incomingUser = secondUser;
        UUID userId = existingUser.getId();
        String existingPassword = existingUser.getPassword();

        when(dao.findById(userId)).thenReturn(Optional.of(existingUser));
        when(passwordManager.matches(incomingUser.getPassword(), existingPassword))
                .thenReturn(false);
        when(dao.save(existingUser)).thenReturn(existingUser);

        User result = service.updateById(incomingUser, userId);

        assertThat(result).isSameAs(existingUser);
        assertThat(result.getPassword()).isEqualTo(incomingUser.getPassword());
        assertThat(result.getRoles()).isEqualTo(incomingUser.getRoles());
        assertThat(result.getEmail()).isEqualTo(incomingUser.getEmail());
        assertThat(result.isEnabled()).isEqualTo(incomingUser.isEnabled());
        assertThat(result.isAccountNonLocked()).isEqualTo(incomingUser.isAccountNonLocked());
        assertThat(result.isAccountNonExpired()).isEqualTo(incomingUser.isAccountNonExpired());
        assertThat(result.isCredentialNonExpired())
                .isEqualTo(incomingUser.isCredentialNonExpired());
        assertThat(result.isEmailVerified()).isEqualTo(incomingUser.isEmailVerified());
        assertThat(result.isGoogleConnected()).isEqualTo(incomingUser.isGoogleConnected());

        verify(passwordManager).matches(incomingUser.getPassword(), existingPassword);
        verify(dao).save(existingUser);
    }

    @Test
    void updateByIdShouldNotUpdatePasswordWhenPasswordsMatch() {
        User existingUser = firstUser;
        User incomingUser = secondUser;
        incomingUser.setRoles(existingUser.getRoles());

        UUID userId = existingUser.getId();
        String existingPassword = existingUser.getPassword();

        when(dao.findById(userId)).thenReturn(Optional.of(existingUser));
        when(passwordManager.matches(incomingUser.getPassword(), existingPassword))
                .thenReturn(true);
        when(dao.save(existingUser)).thenReturn(existingUser);

        User result = service.updateById(incomingUser, userId);

        assertThat(result.getPassword()).isEqualTo(existingPassword);

        verify(passwordManager).matches(incomingUser.getPassword(), existingPassword);
        verify(dao).save(existingUser);
    }

    @Test
    void updateByIdShouldNotCheckPasswordWhenIncomingPasswordIsBlank() {
        User existingUser = firstUser;
        User incomingUser = secondUser;
        incomingUser.setPassword(" ");
        incomingUser.setRoles(existingUser.getRoles());

        UUID userId = existingUser.getId();
        String existingPassword = existingUser.getPassword();

        when(dao.findById(userId)).thenReturn(Optional.of(existingUser));
        when(dao.save(existingUser)).thenReturn(existingUser);

        User result = service.updateById(incomingUser, userId);

        assertThat(result.getPassword()).isEqualTo(existingPassword);

        verifyNoInteractions(passwordManager);
        verify(dao).save(existingUser);
    }

    @Test
    void updateByIdShouldNotCheckPasswordWhenExistingPasswordIsNull() {
        User existingUser = firstUser;
        User incomingUser = secondUser;
        existingUser.setPassword(null);
        incomingUser.setRoles(existingUser.getRoles());

        UUID userId = existingUser.getId();

        when(dao.findById(userId)).thenReturn(Optional.of(existingUser));
        when(dao.save(existingUser)).thenReturn(existingUser);

        User result = service.updateById(incomingUser, userId);

        assertThat(result.getPassword()).isNull();

        verifyNoInteractions(passwordManager);
        verify(dao).save(existingUser);
    }

    @Test
    void updateByIdShouldNotUpdateRolesWhenIncomingRolesAreNull() {
        User existingUser = firstUser;
        User incomingUser = secondUser;
        incomingUser.setRoles(null);

        UUID userId = existingUser.getId();

        when(dao.findById(userId)).thenReturn(Optional.of(existingUser));
        when(passwordManager.matches(incomingUser.getPassword(), existingUser.getPassword()))
                .thenReturn(true);
        when(dao.save(existingUser)).thenReturn(existingUser);

        User result = service.updateById(incomingUser, userId);

        assertThat(result.getRoles()).isEqualTo(existingUser.getRoles());

        verify(dao).save(existingUser);
    }

    @Test
    void updateByIdShouldNotUpdateRolesWhenRolesAreEqual() {
        User existingUser = firstUser;
        User incomingUser = secondUser;
        incomingUser.setRoles(existingUser.getRoles());

        UUID userId = existingUser.getId();

        when(dao.findById(userId)).thenReturn(Optional.of(existingUser));
        when(passwordManager.matches(incomingUser.getPassword(), existingUser.getPassword()))
                .thenReturn(true);
        when(dao.save(existingUser)).thenReturn(existingUser);

        User result = service.updateById(incomingUser, userId);

        assertThat(result.getRoles()).isEqualTo(existingUser.getRoles());

        verify(dao).save(existingUser);
    }

    @Test
    void existsByEmailShouldReturnTrueWhenUserExists() {
        String email = firstUser.getEmail();

        when(dao.existsByEmail(email)).thenReturn(true);

        boolean result = service.existsByEmail(email);

        assertThat(result).isTrue();

        verify(dao).existsByEmail(email);
    }

    @Test
    void existsByEmailShouldReturnFalseWhenUserDoesNotExist() {
        String email = secondUser.getEmail();

        when(dao.existsByEmail(email)).thenReturn(false);

        boolean result = service.existsByEmail(email);

        assertThat(result).isFalse();

        verify(dao).existsByEmail(email);
    }

    @Test
    void findByEmailShouldThrowBadRequestExceptionWhenEmailIsNull() {
        assertThatThrownBy(() -> service.findByEmail(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email must not be null or blank");

        verifyNoInteractions(dao);
    }

    @Test
    void findByEmailShouldThrowBadRequestExceptionWhenEmailIsBlank() {
        String email = " ";

        assertThatThrownBy(() -> service.findByEmail(email))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email must not be null or blank");

        verifyNoInteractions(dao);
    }

    @Test
    void findByEmailShouldThrowUsernameNotFoundExceptionWhenUserDoesNotExist() {
        String email = secondUser.getEmail();

        when(dao.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByEmail(email))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("The email does not match any account");

        verify(dao).findByEmail(email);
    }

    @Test
    void findByEmailShouldReturnUserSuccessfully() {
        String email = firstUser.getEmail();

        when(dao.findByEmail(email)).thenReturn(Optional.of(firstUser));

        User result = service.findByEmail(email);

        assertThat(result).isSameAs(firstUser);

        verify(dao).findByEmail(email);
    }

    @Test
    void changePasswordShouldThrowBadRequestExceptionWhenNewPasswordsDoNotMatch() {
        PasswordRequestDTO requestDTO = new PasswordRequestDTO();
        requestDTO.setNewPassword(firstUser.getPassword());
        requestDTO.setReNewPassword("unused password");

        UserPrincipal principal = new UserPrincipal(firstUser);

        assertThatThrownBy(() -> service.changePassword(principal, requestDTO))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("New password mismatch the ReType password");

        verifyNoInteractions(dao, passwordManager);
    }

    @Test
    void changePasswordShouldThrowUsernameNotFoundExceptionWhenUserDoesNotExist() {
        PasswordRequestDTO requestDTO = new PasswordRequestDTO();
        requestDTO.setNewPassword(secondUser.getPassword());
        requestDTO.setReNewPassword(secondUser.getPassword());

        UserPrincipal principal = new UserPrincipal(firstUser);

        when(dao.findById(principal.getUserId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changePassword(principal, requestDTO))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("The User does not exist.");

        verify(dao).findById(principal.getUserId());
        verifyNoInteractions(passwordManager);
    }

    @Test
    void changePasswordShouldRejectPasswordCreationForNonGoogleUser() {
        User existingUser = firstUser;
        existingUser.setPassword(null);
        existingUser.setGoogleConnected(false);

        PasswordRequestDTO requestDTO = new PasswordRequestDTO();
        requestDTO.setNewPassword(secondUser.getPassword());
        requestDTO.setReNewPassword(secondUser.getPassword());

        UserPrincipal principal = new UserPrincipal(existingUser);

        when(dao.findById(principal.getUserId())).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> service.changePassword(principal, requestDTO))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot change the password. Contact the Administrator");

        verify(dao).findById(principal.getUserId());
        verify(dao, never()).save(any(User.class));
        verifyNoInteractions(passwordManager);
    }

    @Test
    void changePasswordShouldCreatePasswordForGoogleConnectedUser() {
        User existingUser = firstUser;
        existingUser.setPassword(null);
        existingUser.setGoogleConnected(true);

        PasswordRequestDTO requestDTO = new PasswordRequestDTO();
        requestDTO.setNewPassword(secondUser.getPassword());
        requestDTO.setReNewPassword(secondUser.getPassword());

        UserPrincipal principal = new UserPrincipal(existingUser);

        when(dao.findById(principal.getUserId())).thenReturn(Optional.of(existingUser));
        when(passwordManager.encodePassword(requestDTO.getNewPassword()))
                .thenReturn(encodedPassword);
        when(dao.save(existingUser)).thenReturn(existingUser);

        service.changePassword(principal, requestDTO);

        assertThat(existingUser.getPassword()).isEqualTo(encodedPassword);

        verify(passwordManager).encodePassword(requestDTO.getNewPassword());
        verify(dao).save(existingUser);
    }

    @Test
    void changePasswordShouldRejectBlankCurrentPassword() {
        User existingUser = firstUser;

        PasswordRequestDTO requestDTO = new PasswordRequestDTO();
        requestDTO.setCurrentPassword(" ");
        requestDTO.setNewPassword(secondUser.getPassword());
        requestDTO.setReNewPassword(secondUser.getPassword());

        UserPrincipal principal = new UserPrincipal(existingUser);

        when(dao.findById(principal.getUserId())).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> service.changePassword(principal, requestDTO))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Old password does not match");

        verifyNoInteractions(passwordManager);
        verify(dao, never()).save(any(User.class));
    }

    @Test
    void changePasswordShouldRejectIncorrectCurrentPassword() {
        User existingUser = firstUser;

        PasswordRequestDTO requestDTO = new PasswordRequestDTO();
        requestDTO.setCurrentPassword(secondUser.getPassword());
        requestDTO.setNewPassword(encodedPassword);
        requestDTO.setReNewPassword(encodedPassword);

        UserPrincipal principal = new UserPrincipal(existingUser);

        when(dao.findById(principal.getUserId())).thenReturn(Optional.of(existingUser));
        when(passwordManager.matches(requestDTO.getCurrentPassword(), existingUser.getPassword()))
                .thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(principal, requestDTO))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Old password does not match");

        verify(passwordManager)
                .matches(requestDTO.getCurrentPassword(), existingUser.getPassword());
        verify(dao, never()).save(any(User.class));
    }

    @Test
    void changePasswordShouldRejectPreviouslyUsedPassword() {
        User existingUser = firstUser;

        String newPassword = "new-password";
        PasswordRequestDTO requestDTO = new PasswordRequestDTO();
        requestDTO.setCurrentPassword(firstUser.getPassword());
        requestDTO.setNewPassword(newPassword);
        requestDTO.setReNewPassword(newPassword);

        UserPrincipal principal = new UserPrincipal(existingUser);

        when(dao.findById(principal.getUserId())).thenReturn(Optional.of(existingUser));
        when(passwordManager.matches(requestDTO.getCurrentPassword(), existingUser.getPassword()))
                .thenReturn(true);
        when(passwordManager.matches(requestDTO.getNewPassword(), existingUser.getPassword()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.changePassword(principal, requestDTO))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Choose a password you haven't used before.");

        verify(passwordManager)
                .matches(requestDTO.getCurrentPassword(), existingUser.getPassword());
        verify(passwordManager).matches(requestDTO.getNewPassword(), existingUser.getPassword());
        verify(dao, never()).save(any(User.class));
    }

    @Test
    void changePasswordShouldUpdatePasswordSuccessfully() {
        User existingUser = firstUser;
        String existingPassword = existingUser.getPassword();
        String newPassword = "new-password";

        PasswordRequestDTO requestDTO = new PasswordRequestDTO();
        requestDTO.setCurrentPassword(existingPassword);
        requestDTO.setNewPassword(newPassword);
        requestDTO.setReNewPassword(newPassword);

        UserPrincipal principal = new UserPrincipal(existingUser);

        when(dao.findById(principal.getUserId())).thenReturn(Optional.of(existingUser));
        when(passwordManager.matches(requestDTO.getCurrentPassword(), existingPassword))
                .thenReturn(true);
        when(passwordManager.matches(requestDTO.getNewPassword(), existingPassword))
                .thenReturn(false);
        when(passwordManager.encodePassword(requestDTO.getNewPassword()))
                .thenReturn(encodedPassword);
        when(dao.save(existingUser)).thenReturn(existingUser);

        service.changePassword(principal, requestDTO);

        assertThat(existingUser.getPassword()).isEqualTo(encodedPassword);

        verify(passwordManager).matches(requestDTO.getCurrentPassword(), existingPassword);
        verify(passwordManager).matches(requestDTO.getNewPassword(), existingPassword);
        verify(passwordManager).encodePassword(requestDTO.getNewPassword());
        verify(dao).save(existingUser);
    }
}
