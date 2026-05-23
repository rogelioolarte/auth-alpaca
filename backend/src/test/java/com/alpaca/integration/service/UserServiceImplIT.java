package com.alpaca.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.alpaca.dto.request.PasswordRequestDTO;
import com.alpaca.entity.Role;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.persistence.IRoleDAO;
import com.alpaca.resources.RoleProvider;
import com.alpaca.resources.UserProvider;
import com.alpaca.service.impl.UserServiceImpl;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link UserServiceImpl} */
@SpringBootTest
@Transactional
@DisplayName("UserServiceImpl Integration Tests")
class UserServiceImplIT {

    @Autowired private UserServiceImpl service;
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

    // ------------------------------------------------
    // save
    // ------------------------------------------------

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

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPassword()).isNotEqualTo(rawPassword);
        assertThat(saved.getPassword()).isNotBlank();
    }

    // ------------------------------------------------
    // findById
    // ------------------------------------------------

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

    // ------------------------------------------------
    // updateById
    // ------------------------------------------------

    @Test
    @Transactional
    @DisplayName("updateById validates null inputs")
    void updateById_ShouldValidateNullInputs() {

        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.updateById(null, id))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("User cannot be created");

        Throwable thrown = catchThrowable(() -> service.updateById(buildSingleUser(), null));
        assertThat(thrown).as("User cannot be created").isInstanceOf(BadRequestException.class);
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
    @DisplayName("updateById updates email and boolean fields")
    void updateById_ShouldUpdateEmailAndBooleanFields() {

        User saved = service.save(buildSingleUser());

        User update = buildAlternativeUser();
        update.setEnabled(!saved.isEnabled());
        update.setAccountNonExpired(!saved.isAccountNonExpired());
        update.setAccountNonLocked(!saved.isAccountNonLocked());
        update.setCredentialNonExpired(!saved.isCredentialNonExpired());
        update.setEmailVerified(!saved.isEmailVerified());
        update.setGoogleConnected(!saved.isGoogleConnected());

        User result = service.updateById(update, saved.getId());

        assertThat(result.getEmail()).isEqualTo(update.getEmail());
        assertThat(result.isEnabled()).isEqualTo(update.isEnabled());
        assertThat(result.isAccountNonExpired()).isEqualTo(update.isAccountNonExpired());
        assertThat(result.isAccountNonLocked()).isEqualTo(update.isAccountNonLocked());
        assertThat(result.isCredentialNonExpired()).isEqualTo(update.isCredentialNonExpired());
        assertThat(result.isEmailVerified()).isEqualTo(update.isEmailVerified());
        assertThat(result.isGoogleConnected()).isEqualTo(update.isGoogleConnected());
    }

    @Test
    @Transactional
    @DisplayName("updateById updates password when password differs")
    void updateById_ShouldUpdatePassword_WhenPasswordDiffers() {

        User saved = service.save(buildSingleUser());

        String originalPassword = saved.getPassword();

        User update = buildAlternativeUser();
        update.setPassword("newPassword123");

        User result = service.updateById(update, saved.getId());

        assertThat(result.getPassword()).isNotEqualTo(originalPassword);
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

        assertThat(result.getRoles()).isEqualTo(update.getRoles());
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
    @DisplayName("updateById updates roles when incoming roles differ from existing roles")
    void updateById_ShouldUpdateRoles_WhenIncomingRolesAreDifferent() {
        User user = buildSingleUser();

        Role originalRole = RoleProvider.singleTemplate();
        originalRole.setName("USER");
        originalRole.setCreatedAt(now);

        Role persistedOriginalRole = roleDAO.save(originalRole);

        user.setRoles(Set.of(persistedOriginalRole));

        User savedUser = service.save(user);

        Role updatedRole = RoleProvider.alternativeTemplate();
        updatedRole.setName("ADMIN");
        updatedRole.setCreatedAt(now);

        Role persistedUpdatedRole = roleDAO.save(updatedRole);

        User updateRequest = buildAlternativeUser();
        updateRequest.setRoles(Set.of(persistedUpdatedRole));

        User updatedUser = service.updateById(updateRequest, savedUser.getId());

        assertThat(updatedUser.getRoles()).isNotNull();
        assertThat(updatedUser.getRoles()).hasSize(1);

        Role resultRole = updatedUser.getRoles().getFirst();

        assertThat(resultRole.getId()).isEqualTo(persistedUpdatedRole.getId());
        assertThat(resultRole.getName()).isEqualTo(persistedUpdatedRole.getName());
        assertThat(resultRole.getName()).isEqualTo("ADMIN");
    }

    // ------------------------------------------------
    // existsByEmail
    // ------------------------------------------------

    @Test
    @Transactional
    @DisplayName("existsByEmail returns correct existence result")
    void existsByEmail_ShouldReturnCorrectExistenceResult() {

        User saved = service.save(buildSingleUser());

        assertThat(service.existsByEmail(saved.getEmail())).isTrue();

        assertThat(service.existsByEmail("missing@alpaca.com")).isFalse();
    }

    // ------------------------------------------------
    // findByEmail
    // ------------------------------------------------

    @Test
    @Transactional
    @DisplayName("findByEmail validates invalid emails")
    void findByEmail_ShouldValidateInvalidEmails() {

        assertThatThrownBy(() -> service.findByEmail(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email must not be null or blank");

        assertThatThrownBy(() -> service.findByEmail(" "))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email must not be null or blank");
    }

    @Test
    @Transactional
    @DisplayName("findByEmail throws UsernameNotFoundException when email does not exist")
    void findByEmail_ShouldThrowUsernameNotFound_WhenEmailDoesNotExist() {

        assertThatThrownBy(() -> service.findByEmail("ghost@alpaca.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("The email does not match any account");
    }

    @Test
    @Transactional
    @DisplayName("findByEmail returns existing user")
    void findByEmail_ShouldReturnExistingUser() {

        User saved = service.save(buildSingleUser());

        User result = service.findByEmail(saved.getEmail());

        assertThat(result.getId()).isEqualTo(saved.getId());
        assertThat(result.getEmail()).isEqualTo(saved.getEmail());
    }

    // ------------------------------------------------
    // changePassword
    // ------------------------------------------------

    @Test
    @Transactional
    @DisplayName("changePassword throws exception when new passwords mismatch")
    void changePassword_ShouldThrowBadRequest_WhenPasswordsMismatch() {

        PasswordRequestDTO dto = new PasswordRequestDTO();
        dto.setNewPassword("newPassword");
        dto.setReNewPassword("otherPassword");

        UserPrincipal principal = new UserPrincipal();
        principal.setUserId(UUID.randomUUID());

        assertThatThrownBy(() -> service.changePassword(principal, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("New password mismatch the ReType password");
    }

    @Test
    @Transactional
    @DisplayName("changePassword throws UsernameNotFoundException when user does not exist")
    void changePassword_ShouldThrowUsernameNotFound_WhenUserDoesNotExist() {

        PasswordRequestDTO dto = new PasswordRequestDTO();
        dto.setNewPassword("newPassword");
        dto.setReNewPassword("newPassword");

        UserPrincipal principal = new UserPrincipal();
        principal.setUserId(UUID.randomUUID());

        assertThatThrownBy(() -> service.changePassword(principal, dto))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("The User does not exist.");
    }

    @Test
    @Transactional
    @DisplayName(
            "changePassword throws exception when user has null password and is not google"
                    + " connected")
    void changePassword_ShouldThrowBadRequest_WhenPasswordNullAndNotGoogleConnected() {

        User user = buildSingleUser();
        user.setPassword(null);
        user.setGoogleConnected(false);

        User saved = service.save(user);

        PasswordRequestDTO dto = new PasswordRequestDTO();
        dto.setNewPassword("newPassword");
        dto.setReNewPassword("newPassword");

        UserPrincipal principal = new UserPrincipal(saved);

        assertThatThrownBy(() -> service.changePassword(principal, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot change the password. Contact the Administrator");
    }

    @Test
    @Transactional
    @DisplayName("changePassword sets password when google connected user has null password")
    void changePassword_ShouldSetPassword_WhenGoogleConnectedAndPasswordNull() {

        User user = buildSingleUser();
        user.setPassword(null);
        user.setGoogleConnected(true);

        User saved = service.save(user);

        PasswordRequestDTO dto = new PasswordRequestDTO();
        dto.setNewPassword("newPassword");
        dto.setReNewPassword("newPassword");

        UserPrincipal principal = new UserPrincipal(saved);

        service.changePassword(principal, dto);

        User updated = service.findById(saved.getId());

        assertThat(updated.getPassword()).isNotNull();
        assertThat(updated.getPassword()).isNotEqualTo("newPassword");
    }

    @Test
    @Transactional
    @DisplayName("changePassword throws exception when current password is invalid")
    void changePassword_ShouldThrowBadRequest_WhenCurrentPasswordInvalid() {

        User user = buildSingleUser();

        String originalPassword = user.getPassword();

        User saved = service.save(user);

        PasswordRequestDTO dto = new PasswordRequestDTO();
        dto.setCurrentPassword("wrongPassword");
        dto.setNewPassword("newPassword");
        dto.setReNewPassword("newPassword");

        UserPrincipal principal = new UserPrincipal(saved);

        assertThatThrownBy(() -> service.changePassword(principal, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Old password does not match");

        User persisted = service.findById(saved.getId());

        assertThat(persisted.getPassword()).isNotEqualTo(originalPassword);
    }

    @Test
    @Transactional
    @DisplayName("changePassword throws exception when new password matches old password")
    void changePassword_ShouldThrowBadRequest_WhenPasswordAlreadyUsed() {

        User user = buildSingleUser();

        String rawPassword = user.getPassword();

        User saved = service.save(user);

        PasswordRequestDTO dto = new PasswordRequestDTO();
        dto.setCurrentPassword(rawPassword);
        dto.setNewPassword(rawPassword);
        dto.setReNewPassword(rawPassword);

        UserPrincipal principal = new UserPrincipal(saved);

        assertThatThrownBy(() -> service.changePassword(principal, dto))
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

        PasswordRequestDTO dto = new PasswordRequestDTO();
        dto.setCurrentPassword(rawPassword);
        dto.setNewPassword("brandNewPassword");
        dto.setReNewPassword("brandNewPassword");

        UserPrincipal principal = new UserPrincipal(saved);

        service.changePassword(principal, dto);

        User updated = service.findById(saved.getId());

        assertThat(updated.getPassword()).isNotEqualTo(previousEncodedPassword);
    }

    // ------------------------------------------------
    // Generic inherited methods
    // ------------------------------------------------

    @Test
    @Transactional
    @DisplayName("findAll returns persisted users")
    void findAll_ShouldReturnPersistedUsers() {

        service.save(buildSingleUser());

        service.save(buildAlternativeUser());

        List<User> result = service.findAll();

        assertAll(
                () -> assertThat(result).isNotEmpty(),
                () -> assertThat(result).hasSizeGreaterThanOrEqualTo(2));
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

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotEmpty();
    }

    @Test
    @Transactional
    @DisplayName("existsById returns correct existence result")
    void existsById_ShouldReturnCorrectExistenceResult() {

        User saved = service.save(buildSingleUser());

        assertThat(service.existsById(saved.getId())).isTrue();

        assertThat(service.existsById(UUID.randomUUID())).isFalse();

        assertThat(service.existsById(null)).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("existsAllByIds handles edge cases")
    void existsAllByIds_ShouldHandleEdgeCases() {

        assertThat(service.existsAllByIds(null)).isFalse();

        assertThat(service.existsAllByIds(Collections.emptyList())).isFalse();
        List<UUID> ids = new ArrayList<>();
        ids.add(null);
        assertThat(service.existsAllByIds(ids)).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("existsByUniqueProperties handles null")
    void existsByUniqueProperties_ShouldHandleNull() {

        assertThat(service.existsByUniqueProperties(null)).isFalse();
    }
}
