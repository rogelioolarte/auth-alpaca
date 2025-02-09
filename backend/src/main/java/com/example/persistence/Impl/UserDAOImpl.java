package com.example.persistence.Impl;

import com.example.entity.User;
import com.example.exception.NotFoundException;
import com.example.persistence.IUserDAO;
import com.example.repository.GenericRepo;
import com.example.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserDAOImpl extends GenericDAOImpl<User, UUID> implements IUserDAO {

    private final UserRepo repo;

    @Override
    protected GenericRepo<User, UUID> getRepo() {
        return repo;
    }

    @Override
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
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            existingUser.setRoles(user.getRoles());
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

    @Override
    public List<User> findUsersByRoleId(UUID id) {
        if (id == null) return List.of();
        return repo.findUsersByRoleId(id);
    }

}
