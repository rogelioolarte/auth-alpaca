package com.alpaca.persistence.impl;

import com.alpaca.entity.User;
import com.alpaca.exception.NotFoundException;
import com.alpaca.persistence.IUserDAO;
import com.alpaca.repository.GenericRepo;
import com.alpaca.repository.UserRepo;
import java.util.Optional;
import java.util.UUID;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserDAOImpl extends GenericDAOImpl<User, UUID> implements IUserDAO {

    private final UserRepo repo;

    @Override
    @Generated
    protected GenericRepo<User, UUID> getRepo() {
        return repo;
    }

    @Override
    @Generated
    protected Class<User> getEntity() {
        return User.class;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        return repo.findByEmail(email);
    }

    @Override
    public User updateById(User user, UUID id) {
        User existingUser =
                findById(id)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                String.format(
                                                        "%s with ID %s not found",
                                                        getEntity().getName(), id.toString())));
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            existingUser.setEmail(user.getEmail());
        }
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            existingUser.setPassword(user.getPassword());
        }
        if (user.getUserRoles() != null && !user.getUserRoles().isEmpty()) {
            existingUser.setUserRoles(user.getRoles());
        }
        if (user.getProfile() != null && user.getProfile().getId() != null) {
            existingUser.setProfile(user.getProfile());
        }
        if (user.getAdvertiser() != null && user.getAdvertiser().getId() != null) {
            existingUser.setAdvertiser(user.getAdvertiser());
        }
        if (existingUser.isEnabled() != user.isEnabled()) {
            existingUser.setEnabled(user.isEnabled());
        }
        if (existingUser.isAccountNoLocked() != user.isAccountNoLocked()) {
            existingUser.setAccountNoLocked(user.isAccountNoLocked());
        }
        if (existingUser.isAccountNoExpired() != user.isAccountNoExpired()) {
            existingUser.setAccountNoExpired(user.isAccountNoExpired());
        }
        if (existingUser.isCredentialNoExpired() != user.isCredentialNoExpired()) {
            existingUser.setCredentialNoExpired(user.isCredentialNoExpired());
        }
        if (existingUser.isEmailVerified() != user.isEmailVerified()) {
            existingUser.setEmailVerified(user.isEmailVerified());
        }
        if (existingUser.isGoogleConnected() != user.isGoogleConnected()) {
            existingUser.setGoogleConnected(user.isGoogleConnected());
        }
        return save(existingUser);
    }

    @Override
    public boolean existsByUniqueProperties(User user) {
        return existsByEmail(user.getEmail());
    }

    @Override
    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) return false;
        return repo.existsByEmail(email);
    }
}
