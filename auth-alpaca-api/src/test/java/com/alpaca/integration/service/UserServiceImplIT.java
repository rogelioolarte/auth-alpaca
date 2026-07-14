package com.alpaca.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.alpaca.dto.request.PasswordRequestDTO;
import com.alpaca.entity.Profile;
import com.alpaca.entity.Role;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.persistence.IProfileDAO;
import com.alpaca.persistence.IRoleDAO;
import com.alpaca.persistence.IUserDAO;
import com.alpaca.resources.provider.ProfileProvider;
import com.alpaca.resources.provider.RoleProvider;
import com.alpaca.resources.provider.UserProvider;
import com.alpaca.resources.utility.BaseIntegrationTests;
import com.alpaca.security.oauth2.userinfo.OAuth2UserInfo;
import com.alpaca.security.oauth2.userinfo.OAuth2UserInfoFactory;
import com.alpaca.service.impl.UserServiceImpl;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link UserServiceImpl}. */
@DisplayName("UserServiceImpl Integration Tests")
class UserServiceImplIT extends BaseIntegrationTests {

    @Autowired private UserServiceImpl service;
    @Autowired private IProfileDAO profileDAO;
    @Autowired private IUserDAO userDAO;
    @Autowired private IRoleDAO roleDAO;

    private Instant now;

    @BeforeEach
    void setup() {
        now = Instant.now();
    }

    private User buildSingleUser() {
        User user = UserProvider.singleTemplate();
        user.setCreatedAt(now);
        return user;
    }

    private Profile buildSingleProfile(User user) {
        Profile profile = ProfileProvider.singleTemplate();
        if (user != null) {
            profile.setUser(user);
        }
        profile.setCreatedAt(now);
        return profile;
    }

    private User buildAlternativeUser() {
        User user = UserProvider.alternativeTemplate();
        user.setCreatedAt(now);
        return user;
    }

    private Role buildRole() {
        Role role = RoleProvider.singleTemplate();
        role.setCreatedAt(now);
        return role;
    }

    @Test
    @Transactional
    @DisplayName("save throws BadRequestException when user is null")
    void save_ShouldThrowBadRequest_WhenUserIsNull() {
        assertThatThrownBy(() -> service.save(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("User cannot be created");
    }

    @Test
    @Transactional
    @DisplayName("save encodes password and persists user")
    void save_ShouldEncodePassword_AndPersistUser() {
        User user = buildSingleUser();
        String rawPassword = user.getPassword();

        User saved = service.save(user);

        assertAll(
                () -> assertThat(saved.getId()).isNotNull(),
                () -> assertThat(saved.getPassword()).isNotBlank(),
                () -> assertThat(saved.getPassword()).isNotEqualTo(rawPassword));
    }

    @Test
    @Transactional
    @DisplayName("registerOAuth2User creates a new OAuth2 user and profile")
    void registerOAuth2User_ShouldCreateUserAndProfile_WhenEmailDoesNotExist() {
        User template = buildSingleUser();
        Profile profile = buildSingleProfile(template);

        OAuth2UserInfo userInfo =
                OAuth2UserInfoFactory.getOAuth2UserInfo(
                        "google",
                        Map.of(
                                "email", template.getEmail(),
                                "given_name", profile.getFirstName(),
                                "family_name", profile.getLastName(),
                                "picture", profile.getAvatarUrl(),
                                "email_verified", template.isEmailVerified()));

        User result = service.registerOAuth2User(userInfo);

        assertAll(
                () -> assertThat(result.getId()).isNotNull(),
                () -> assertThat(result.getEmail()).isEqualTo(userInfo.getEmail()),
                () -> assertThat(result.isEmailVerified()).isEqualTo(userInfo.getEmailVerified()),
                () -> assertThat(result.isGoogleConnected()).isTrue());
    }

    @Test
    @Transactional
    @DisplayName("registerOAuth2User returns and updates an existing OAuth2 user")
    void registerOAuth2User_ShouldUpdateExistingUser_WhenEmailExists() {
        User user = buildSingleUser();
        user.setGoogleConnected(false);
        user.setEmailVerified(false);
        User saved = service.save(user);
        Profile profile = profileDAO.save(buildSingleProfile(saved));
        saved.setProfile(profile);

        OAuth2UserInfo userInfo =
                OAuth2UserInfoFactory.getOAuth2UserInfo(
                        "google",
                        Map.of(
                                "email", saved.getEmail(),
                                "given_name", saved.getProfile().getFirstName(),
                                "family_name", saved.getProfile().getLastName(),
                                "picture", saved.getProfile().getAvatarUrl(),
                                "email_verified", true));

        User result = service.registerOAuth2User(userInfo);

        assertAll(
                () -> assertThat(result.getId()).isEqualTo(saved.getId()),
                () -> assertThat(result.isGoogleConnected()).isTrue(),
                () -> assertThat(result.isEmailVerified()).isTrue());
    }

    @Test
    @Transactional
    @DisplayName("findById validates null id")
    void findById_ShouldValidateNullId() {
        assertThatThrownBy(() -> service.findById(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("User cannot be found");
    }

    @Test
    @Transactional
    @DisplayName("findById throws NotFoundException when user does not exist")
    void findById_ShouldThrowNotFound_WhenUserDoesNotExist() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User with ID " + id + " not found");
    }

    @Test
    @Transactional
    @DisplayName("findById returns existing user")
    void findById_ShouldReturnExistingUser() {
        User saved = service.save(buildSingleUser());

        User result = service.findById(saved.getId());

        assertThat(result.getId()).isEqualTo(saved.getId());
    }

    @Test
    @Transactional
    @DisplayName("updateById validates null inputs")
    void updateById_ShouldValidateNullInputs() {
        User update = buildSingleUser();
        UUID id = UUID.randomUUID();

        assertAll(
                () ->
                        assertThatThrownBy(() -> service.updateById(null, id))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("User cannot be created"),
                () ->
                        assertThatThrownBy(() -> service.updateById(update, null))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("User cannot be created"));
    }

    @Test
    @Transactional
    @DisplayName("updateById throws NotFoundException when user does not exist")
    void updateById_ShouldThrowNotFound_WhenUserDoesNotExist() {
        User update = buildAlternativeUser();
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.updateById(update, id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User with ID " + id + " not found");
    }

    @Test
    @Transactional
    @DisplayName("updateById updates mutable fields")
    void updateById_ShouldUpdateMutableFields() {
        User saved = service.save(buildSingleUser());
        User update = buildAlternativeUser();

        update.setEnabled(!saved.isEnabled());
        update.setAccountNonExpired(!saved.isAccountNonExpired());
        update.setAccountNonLocked(!saved.isAccountNonLocked());
        update.setCredentialNonExpired(!saved.isCredentialNonExpired());
        update.setEmailVerified(!saved.isEmailVerified());
        update.setGoogleConnected(!saved.isGoogleConnected());

        User result = service.updateById(update, saved.getId());

        assertAll(
                () -> assertThat(result.getEmail()).isEqualTo(update.getEmail()),
                () -> assertThat(result.isEnabled()).isEqualTo(update.isEnabled()),
                () ->
                        assertThat(result.isAccountNonExpired())
                                .isEqualTo(update.isAccountNonExpired()),
                () ->
                        assertThat(result.isAccountNonLocked())
                                .isEqualTo(update.isAccountNonLocked()),
                () ->
                        assertThat(result.isCredentialNonExpired())
                                .isEqualTo(update.isCredentialNonExpired()),
                () -> assertThat(result.isEmailVerified()).isEqualTo(update.isEmailVerified()),
                () -> assertThat(result.isGoogleConnected()).isEqualTo(update.isGoogleConnected()));
    }

    @Test
    @Transactional
    @DisplayName("updateById updates password when password differs")
    void updateById_ShouldUpdatePassword_WhenPasswordDiffers() {
        User saved = service.save(buildSingleUser());
        String originalPassword = saved.getPassword();
        User update = buildAlternativeUser();

        User result = service.updateById(update, saved.getId());

        assertThat(result.getPassword()).isNotEqualTo(originalPassword);
    }

    @Test
    @Transactional
    @DisplayName("updateById does not update password when raw password matches existing password")
    void updateById_ShouldNotUpdatePassword_WhenPasswordMatches() {
        User user = buildSingleUser();
        String rawPassword = user.getPassword();
        User saved = service.save(user);
        String encodedPassword = saved.getPassword();

        User update = buildAlternativeUser();
        update.setPassword(rawPassword);

        User result = service.updateById(update, saved.getId());

        assertThat(result.getPassword()).isEqualTo(encodedPassword);
    }

    @Test
    @Transactional
    @DisplayName("updateById ignores blank password")
    void updateById_ShouldIgnoreBlankPassword() {
        User saved = service.save(buildSingleUser());
        String encodedPassword = saved.getPassword();
        User update = buildAlternativeUser();
        update.setPassword(" ");

        User result = service.updateById(update, saved.getId());

        assertThat(result.getPassword()).isEqualTo(encodedPassword);
    }

    @Test
    @Transactional
    @DisplayName("updateById updates roles when roles differ")
    void updateById_ShouldUpdateRoles_WhenRolesDiffer() {
        User saved = service.save(buildSingleUser());
        Role role = roleDAO.save(buildRole());
        User update = buildAlternativeUser();
        update.setRoles(Set.of(role));

        User result = service.updateById(update, saved.getId());

        assertThat(result.getRoles()).containsExactly(role);
    }

    @Test
    @Transactional
    @DisplayName("updateById preserves roles when incoming roles are null")
    void updateById_ShouldPreserveRoles_WhenIncomingRolesAreNull() {
        Role role = roleDAO.save(buildRole());
        User user = buildSingleUser();
        user.setRoles(Set.of(role));
        User saved = service.save(user);

        User update = buildAlternativeUser();
        update.setRoles(null);

        User result = service.updateById(update, saved.getId());

        assertThat(result.getRoles()).containsExactly(role);
    }

    @Test
    @Transactional
    @DisplayName("updateById ignores blank email")
    void updateById_ShouldIgnoreBlankEmail() {
        User saved = service.save(buildSingleUser());
        String originalEmail = saved.getEmail();
        User update = buildAlternativeUser();
        update.setEmail(" ");

        User result = service.updateById(update, saved.getId());

        assertThat(result.getEmail()).isEqualTo(originalEmail);
    }

    @Test
    @Transactional
    @DisplayName("existsByEmail returns correct existence result")
    void existsByEmail_ShouldReturnCorrectExistenceResult() {
        User saved = service.save(buildSingleUser());

        assertAll(
                () -> assertThat(service.existsByEmail(saved.getEmail())).isTrue(),
                () ->
                        assertThat(service.existsByEmail(buildAlternativeUser().getEmail()))
                                .isFalse());
    }

    @Test
    @Transactional
    @DisplayName("findByEmail validates invalid emails")
    void findByEmail_ShouldValidateInvalidEmails() {
        assertAll(
                () ->
                        assertThatThrownBy(() -> service.findByEmail(null))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("Email must not be null or blank"),
                () ->
                        assertThatThrownBy(() -> service.findByEmail(" "))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("Email must not be null or blank"));
    }

    @Test
    @Transactional
    @DisplayName("findByEmail throws UsernameNotFoundException when email does not exist")
    void findByEmail_ShouldThrowUsernameNotFound_WhenEmailDoesNotExist() {
        User user = buildSingleUser();
        String email = user.getEmail();

        assertThatThrownBy(() -> service.findByEmail(email))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("The email does not match any account");
    }

    @Test
    @Transactional
    @DisplayName("findByEmail returns existing user")
    void findByEmail_ShouldReturnExistingUser() {
        User saved = service.save(buildSingleUser());

        User result = service.findByEmail(saved.getEmail());

        assertAll(
                () -> assertThat(result.getId()).isEqualTo(saved.getId()),
                () -> assertThat(result.getEmail()).isEqualTo(saved.getEmail()));
    }

    @Test
    @Transactional
    @DisplayName("changePassword throws exception when new passwords mismatch")
    void changePassword_ShouldThrowBadRequest_WhenPasswordsMismatch() {
        PasswordRequestDTO request = new PasswordRequestDTO();
        request.setNewPassword("newPassword");
        request.setReNewPassword("otherPassword");

        UserPrincipal principal = new UserPrincipal();
        principal.setUserId(UUID.randomUUID());

        assertThatThrownBy(() -> service.changePassword(principal, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("New password mismatch the ReType password");
    }

    @Test
    @Transactional
    @DisplayName("changePassword throws UsernameNotFoundException when user does not exist")
    void changePassword_ShouldThrowUsernameNotFound_WhenUserDoesNotExist() {
        PasswordRequestDTO request = new PasswordRequestDTO();
        request.setNewPassword("newPassword");
        request.setReNewPassword(request.getNewPassword());

        UserPrincipal principal = new UserPrincipal();
        principal.setUserId(UUID.randomUUID());

        assertThatThrownBy(() -> service.changePassword(principal, request))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("The User does not exist.");
    }

    @Test
    @Transactional
    @DisplayName("changePassword rejects password creation for non Google account")
    void changePassword_ShouldThrowBadRequest_WhenPasswordNullAndNotGoogleConnected() {
        User user = buildSingleUser();
        user.setPassword(null);
        user.setGoogleConnected(false);
        User saved = userDAO.save(user);

        PasswordRequestDTO request = new PasswordRequestDTO();
        request.setNewPassword("newPassword");
        request.setReNewPassword(request.getNewPassword());

        UserPrincipal principal = new UserPrincipal(saved);

        assertThatThrownBy(() -> service.changePassword(principal, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot change the password. Contact the Administrator");
    }

    @Test
    @Transactional
    @DisplayName("changePassword creates password for Google connected user")
    void changePassword_ShouldSetPassword_WhenGoogleConnectedAndPasswordNull() {
        User user = buildSingleUser();
        user.setPassword(null);
        user.setGoogleConnected(true);
        User saved = userDAO.save(user);

        PasswordRequestDTO request = new PasswordRequestDTO();
        request.setNewPassword("newPassword");
        request.setReNewPassword(request.getNewPassword());

        UserPrincipal principal = new UserPrincipal(saved);

        service.changePassword(principal, request);

        User updated = service.findById(saved.getId());

        assertAll(
                () -> assertThat(updated.getPassword()).isNotNull(),
                () -> assertThat(updated.getPassword()).isNotEqualTo(request.getNewPassword()));
    }

    @Test
    @Transactional
    @DisplayName("changePassword rejects blank current password")
    void changePassword_ShouldThrowBadRequest_WhenCurrentPasswordIsBlank() {
        User user = buildSingleUser();
        User saved = service.save(user);

        PasswordRequestDTO request = new PasswordRequestDTO();
        request.setCurrentPassword(" ");
        request.setNewPassword("newPassword");
        request.setReNewPassword(request.getNewPassword());

        UserPrincipal principal = new UserPrincipal(saved);

        assertThatThrownBy(() -> service.changePassword(principal, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Old password does not match");
    }

    @Test
    @Transactional
    @DisplayName("changePassword rejects invalid current password")
    void changePassword_ShouldThrowBadRequest_WhenCurrentPasswordInvalid() {
        User user = buildSingleUser();
        User saved = service.save(user);

        PasswordRequestDTO request = new PasswordRequestDTO();
        request.setCurrentPassword("wrongPassword");
        request.setNewPassword("newPassword");
        request.setReNewPassword(request.getNewPassword());

        UserPrincipal principal = new UserPrincipal(saved);

        assertThatThrownBy(() -> service.changePassword(principal, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Old password does not match");
    }

    @Test
    @Transactional
    @DisplayName("changePassword rejects a previously used password")
    void changePassword_ShouldThrowBadRequest_WhenPasswordAlreadyUsed() {
        User user = buildSingleUser();
        String rawPassword = user.getPassword();
        User saved = service.save(user);

        PasswordRequestDTO request = new PasswordRequestDTO();
        request.setCurrentPassword(rawPassword);
        request.setNewPassword(rawPassword);
        request.setReNewPassword(request.getNewPassword());

        UserPrincipal principal = new UserPrincipal(saved);

        assertThatThrownBy(() -> service.changePassword(principal, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Choose a password you haven't used before.");
    }

    @Test
    @Transactional
    @DisplayName("changePassword updates password successfully")
    void changePassword_ShouldUpdatePasswordSuccessfully() {
        User user = buildSingleUser();
        String rawPassword = user.getPassword();
        User saved = service.save(user);
        String previousEncodedPassword = saved.getPassword();

        PasswordRequestDTO request = new PasswordRequestDTO();
        request.setCurrentPassword(rawPassword);
        request.setNewPassword("brandNewPassword");
        request.setReNewPassword(request.getNewPassword());

        UserPrincipal principal = new UserPrincipal(saved);

        service.changePassword(principal, request);

        User updated = service.findById(saved.getId());

        assertThat(updated.getPassword()).isNotEqualTo(previousEncodedPassword);
    }

    @Test
    @Transactional
    @DisplayName("findAll returns persisted users")
    void findAll_ShouldReturnPersistedUsers() {
        service.save(buildSingleUser());
        service.save(buildAlternativeUser());

        List<User> result = service.findAll();

        assertThat(result).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @Transactional
    @DisplayName("findAllPage validates null pageable")
    void findAllPage_ShouldValidateNullPageable() {
        assertThatThrownBy(() -> service.findAllPage(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("User(s) Page cannot be created");
    }

    @Test
    @Transactional
    @DisplayName("findAllPage returns paginated users")
    void findAllPage_ShouldReturnPaginatedUsers() {
        service.save(buildSingleUser());

        Page<User> result = service.findAllPage(Pageable.ofSize(10));

        assertThat(result.getContent()).isNotEmpty();
    }

    @Test
    @Transactional
    @DisplayName("existsById returns correct existence result")
    void existsById_ShouldReturnCorrectExistenceResult() {
        User saved = service.save(buildSingleUser());

        assertAll(
                () -> assertThat(service.existsById(saved.getId())).isTrue(),
                () -> assertThat(service.existsById(UUID.randomUUID())).isFalse(),
                () -> assertThat(service.existsById(null)).isFalse());
    }

    @Test
    @Transactional
    @DisplayName("existsAllByIds handles edge cases")
    void existsAllByIds_ShouldHandleEdgeCases() {
        List<UUID> idsWithNull = new ArrayList<>();
        idsWithNull.add(null);

        assertAll(
                () -> assertThat(service.existsAllByIds(null)).isFalse(),
                () -> assertThat(service.existsAllByIds(Collections.emptyList())).isFalse(),
                () -> assertThat(service.existsAllByIds(idsWithNull)).isFalse());
    }

    @Test
    @Transactional
    @DisplayName("existsByUniqueProperties handles null")
    void existsByUniqueProperties_ShouldHandleNull() {
        assertThat(service.existsByUniqueProperties(null)).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("checkExistingUser throws UnauthorizedException when account is blocked")
    void checkExistingUser_ShouldThrow_WhenUserBlocked() {
        User user = buildSingleUser();
        user.setAllowed(false);
        boolean isEmailVerified = user.isEmailVerified();

        assertThatThrownBy(() -> service.checkExistingUser(user, isEmailVerified))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("The account has been deactivated or blocked");
    }

    @Test
    @Transactional
    @DisplayName("checkExistingUser updates Google connection")
    void checkExistingUser_ShouldUpdateConnection_WhenPreviouslyFalse() {
        User user = buildSingleUser();
        user.setGoogleConnected(false);
        user.setAllowed(true);
        User saved = service.save(user);

        User updated = service.checkExistingUser(saved, saved.isEmailVerified());

        assertAll(
                () -> assertThat(updated.isGoogleConnected()).isTrue(),
                () ->
                        assertThat(service.findByEmail(saved.getEmail()).isGoogleConnected())
                                .isTrue());
    }

    @Test
    @Transactional
    @DisplayName("checkExistingUser updates email verification status")
    void checkExistingUser_ShouldUpdateVerification_WhenStatusChanges() {
        User user = buildAlternativeUser();
        user.setEmailVerified(false);
        user.setGoogleConnected(true);
        user.setAllowed(true);
        User saved = service.save(user);

        User updated = service.checkExistingUser(saved, true);

        assertThat(updated.isEmailVerified()).isTrue();
    }

    @Test
    @Transactional
    @DisplayName("checkExistingUser returns unchanged user when OAuth2 state is current")
    void checkExistingUser_ShouldReturnUnchangedUser_WhenStateIsCurrent() {
        User user = buildSingleUser();
        user.setAllowed(true);
        user.setGoogleConnected(true);
        User saved = service.save(user);

        User result = service.checkExistingUser(saved, saved.isEmailVerified());

        assertAll(
                () -> assertThat(result.getId()).isEqualTo(saved.getId()),
                () -> assertThat(result.isGoogleConnected()).isTrue(),
                () -> assertThat(result.isEmailVerified()).isEqualTo(saved.isEmailVerified()));
    }
}
