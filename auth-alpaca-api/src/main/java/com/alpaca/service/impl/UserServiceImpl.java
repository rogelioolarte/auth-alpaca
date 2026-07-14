package com.alpaca.service.impl;

import com.alpaca.dto.request.PasswordRequestDTO;
import com.alpaca.entity.Profile;
import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.exception.NotFoundException;
import com.alpaca.exception.UnauthorizedException;
import com.alpaca.model.UserPrincipal;
import com.alpaca.persistence.IGenericDAO;
import com.alpaca.persistence.IProfileDAO;
import com.alpaca.persistence.IUserDAO;
import com.alpaca.security.manager.PasswordManager;
import com.alpaca.security.oauth2.userinfo.OAuth2UserInfo;
import com.alpaca.service.IGenericService;
import com.alpaca.service.IRoleService;
import com.alpaca.service.IUserService;
import java.util.UUID;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Service layer implementation for managing {@link User} entities and encapsulating business logic
 * beyond simple CRUD operations inherited from {@link IGenericService}.
 *
 * <p>Provides transactional operations for user registration, verification, and search by email.
 * Error handling is included for invalid inputs and missing users.
 *
 * @see IGenericService
 * @see IUserService
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends GenericServiceImpl<User, UUID> implements IUserService {

    private final IUserDAO dao;
    private final IProfileDAO profileService;
    private final IRoleService roleService;

    private final PasswordManager passwordManager;
    private static final String ERROR_CREATED_MESS = "%s cannot be created";

    /**
     * Provides the DAO component used by inherited service operations.
     *
     * @return the {@link IGenericDAO} implementation managing {@link User} persistence
     */
    @Override
    @Generated
    protected IGenericDAO<User, UUID> getDAO() {
        return dao;
    }

    /**
     * Supplies a human-readable entity name used in exception messages.
     *
     * @return the name of the entity ("User")
     */
    @Override
    @Generated
    protected String getEntityName() {
        return "User";
    }

    /**
     * Registers a new {@link User} in the system. The provided plaintext password is encoded via
     * {@link PasswordManager} before persisting — the raw password is never stored.
     *
     * @param user the user to register; must not be {@code null}
     * @return the saved {@link User} instance
     * @throws BadRequestException if the provided user is {@code null}
     */
    @Override
    public User save(User user) {
        if (user == null)
            throw new BadRequestException(String.format(ERROR_CREATED_MESS, getEntityName()));
        user.setPassword(passwordManager.encodePassword(user.getPassword()));
        return super.save(user);
    }

    /**
     * Registers or updates a user based on OAuth2 provider information.
     *
     * <p>If a user with the email already exists, their OAuth2 connection and email verification
     * are updated via {@link #checkExistingUser(User, boolean)}. Otherwise, a new {@link User} and
     * associated {@link Profile} are created within the same transaction.
     *
     * @param userInfo the OAuth2 provider user information; must not be {@code null}
     * @return the existing or newly registered {@link User}
     * @throws UnauthorizedException if the existing account is disabled or locked
     */
    @Override
    @Transactional
    public User registerOAuth2User(OAuth2UserInfo userInfo) {
        String email = userInfo.getEmail();
        String firstName = userInfo.getFirstName();
        String lastName = userInfo.getLastName();
        String imageURL = userInfo.getImageUrl();
        boolean emailVerified = userInfo.getEmailVerified();
        if (existsByEmail(email)) {
            return checkExistingUser(findByEmail(email), emailVerified);
        } else {
            User user = new User(email, null, emailVerified, true, roleService.getUserRoles());
            Profile profile = new Profile(firstName, lastName, "", imageURL, null);
            User newUser = dao.save(user);
            profile.setUser(user);
            profileService.save(profile);
            return newUser;
        }
    }

    /**
     * Updates an existing user's OAuth2 connection status and email verification flag.
     *
     * @param user the existing user
     * @param emailVerified OAuth2-provided email verification status
     * @return updated {@link User}
     * @throws UnauthorizedException if the user account is disabled or locked
     */
    public User checkExistingUser(User user, boolean emailVerified) {
        if (!user.isAllowUser()) {
            throw new UnauthorizedException("The account has been deactivated or blocked");
        }
        if (!user.isGoogleConnected()) {
            user.setGoogleConnected(true);
            dao.save(user);
        }
        if (user.isEmailVerified() != emailVerified) {
            user.setEmailVerified(emailVerified);
            dao.save(user);
        }
        return user;
    }

    /**
     * Updates an existing {@link User} identified by the given ID with the non-null and non-blank
     * properties provided in the supplied {@code user} object. Only fields that are different,
     * non-null, and non-blank are updated. Throws a {@link NotFoundException} if no user with the
     * specified ID exists.
     *
     * @param user user object containing updated values
     * @param id the unique identifier of the user to update
     * @return the updated and saved {@link User} instance
     * @throws NotFoundException if no existing user is found with the given ID
     */
    @Override
    public User updateById(User user, UUID id) {
        if (user == null || id == null)
            throw new BadRequestException(String.format(ERROR_CREATED_MESS, getEntityName()));

        User existingUser =
                dao.findById(id)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                String.format(
                                                        "%s with ID %s not found",
                                                        getEntityName(), id)));

        if (existingUser.getPassword() != null
                && StringUtils.hasText(user.getPassword())
                && !passwordManager.matches(user.getPassword(), existingUser.getPassword())) {
            existingUser.setPassword(user.getPassword());
        }

        if (user.getRoles() != null && !user.getRoles().equals(existingUser.getRoles())) {
            existingUser.setRoles(user.getRoles());
        }

        existingUser.updateProfile(user);
        existingUser.updateAdvertiser(user);

        updateTextIfExists(existingUser.getEmail(), user.getEmail(), existingUser::setEmail);
        updateIfDifferent(existingUser.isEnabled(), user.isEnabled(), existingUser::setEnabled);
        updateIfDifferent(
                existingUser.isAccountNonLocked(),
                user.isAccountNonLocked(),
                existingUser::setAccountNonLocked);
        updateIfDifferent(
                existingUser.isAccountNonExpired(),
                user.isAccountNonExpired(),
                existingUser::setAccountNonExpired);
        updateIfDifferent(
                existingUser.isCredentialNonExpired(),
                user.isCredentialNonExpired(),
                existingUser::setCredentialNonExpired);
        updateIfDifferent(
                existingUser.isEmailVerified(),
                user.isEmailVerified(),
                existingUser::setEmailVerified);
        updateIfDifferent(
                existingUser.isGoogleConnected(),
                user.isGoogleConnected(),
                existingUser::setGoogleConnected);
        return super.save(existingUser);
    }

    /**
     * Checks whether a user exists based on their email address.
     *
     * @param email the email to check; may be {@code null} or blank
     * @return {@code true} if a user with the specified email exists; {@code false} otherwise
     */
    @Override
    public boolean existsByEmail(String email) {
        return dao.existsByEmail(email);
    }

    /**
     * Retrieves a {@link User} by their email address.
     *
     * @param email the email of the user to find; must not be {@code null} or blank
     * @return the found {@link User}
     * @throws BadRequestException if the email is {@code null} or blank
     * @throws NotFoundException if no user is found with the given email
     */
    @Override
    public User findByEmail(String email) {
        if (email == null || email.isBlank())
            throw new BadRequestException("Email must not be null or blank");
        // Retrieves a User with Roles and Permissions
        return dao.findByEmail(email)
                .orElseThrow(
                        () ->
                                new UsernameNotFoundException(
                                        "The email does not match any account"));
    }

    /**
     * Changes the password for the authenticated user identified by {@link UserPrincipal}.
     *
     * <p>This method enforces several business rules:
     *
     * <ul>
     *   <li>The new password and confirmation must match.
     *   <li>If the user has no password set (OAuth2-only account), a password is created only if
     *       the account is Google-connected; otherwise the operation is rejected.
     *   <li>If a password already exists, the current password must be provided and match.
     *   <li>The new password must differ from the current one.
     * </ul>
     *
     * @param principal the currently authenticated user
     * @param requestDTO the request containing current, new, and confirmation passwords
     * @throws BadRequestException if validation fails for any of the above rules
     */
    @Override
    public void changePassword(UserPrincipal principal, PasswordRequestDTO requestDTO) {
        if (!requestDTO.getNewPassword().equals(requestDTO.getReNewPassword())) {
            throw new BadRequestException("New password mismatch the ReType password");
        }

        User user =
                dao.findById(principal.getUserId())
                        .orElseThrow(
                                () -> new UsernameNotFoundException("The User does not exist."));

        if (user.getPassword() == null) {
            if (!user.isGoogleConnected()) {
                throw new BadRequestException(
                        "Cannot change the password. Contact the Administrator");
            }
            user.setPassword(passwordManager.encodePassword(requestDTO.getNewPassword()));
            super.save(user);
            return;
        }

        if (!StringUtils.hasText(requestDTO.getCurrentPassword())
                || !passwordManager.matches(requestDTO.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Old password does not match");
        }

        if (passwordManager.matches(requestDTO.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("Choose a password you haven't used before.");
        }

        user.setPassword(passwordManager.encodePassword(requestDTO.getNewPassword()));
        super.save(user);
    }
}
