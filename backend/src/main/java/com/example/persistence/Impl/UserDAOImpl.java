package com.example.persistence.impl;

import com.example.entity.User;
import com.example.exception.NotFoundException;
import com.example.persistence.IUserDAO;
import com.example.repository.GenericRepo;
import com.example.repository.UserRepo;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

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
        User existingUser = findById(id).orElseThrow(() -> new NotFoundException(
                        String.format("%s with ID %s not found",
                                getEntity().getName(), id.toString())));
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            existingUser.setEmail(user.getEmail());
        }
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            existingUser.setPassword(user.getPassword());
        }
        if (user.getUserRoles() != null && !user.getUserRoles().isEmpty()) {
            existingUser.setUserRoles(user.getUserRoles());
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
