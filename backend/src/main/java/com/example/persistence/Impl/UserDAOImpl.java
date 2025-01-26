package com.example.persistence.Impl;

import com.example.entity.User;
import com.example.exception.NotFoundException;
import com.example.persistence.IProfileDAO;
import com.example.persistence.IUserDAO;
import com.example.repository.GenericRepo;
import com.example.repository.UserRepo;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserDAOImpl extends GenericDAOImpl<User, UUID> implements IUserDAO {

    private final UserRepo repo;
    private final EntityManager entityManager;
    private final IProfileDAO profileDAO;

    @Override
    protected GenericRepo<User, UUID> getRepo() {
        return repo;
    }

    @Override
    protected EntityManager getEntityManager() {
        return entityManager;
    }

    @Override
    protected Class<User> getEntity() {
        return User.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) return Optional.empty();
        return Optional.ofNullable((User) entityManager.createNativeQuery(
                        "SELECT * FROM users WHERE username = :username", User.class)
                .setParameter("username", username)
                .getResultList().stream().findFirst().orElse(null));
    }

    @Override
    public void deleteById(UUID id) {
        entityManager.createNativeQuery("DELETE FROM profiles WHERE user_id = :id")
                .setParameter("id", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM user_roles WHERE user_id = :id")
                .setParameter("id", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM users WHERE user_id = :id")
                .setParameter("id", id).executeUpdate();
    }

    @Override
    public User updateById(User user, UUID id) {
        User existingUser = findById(id).orElseThrow(() -> new NotFoundException(
                        String.format("%s Object with ID %s not found",
                                getEntity().getName(), id.toString())));
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            existingUser.setUsername(user.getUsername());
        }
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            existingUser.setPassword(user.getPassword());
        }
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            existingUser.setRoles(user.getRoles());
        }
        if(user.isEnabled() != existingUser.isEnabled()) {
            existingUser.setEnabled(user.isEnabled());
        }
        if(user.isAccountNoLocked() != existingUser.isAccountNoLocked()) {
            existingUser.setEnabled(user.isAccountNoLocked());
        }
        if(user.isAccountNoExpired() != existingUser.isAccountNoExpired()) {
            existingUser.setEnabled(user.isAccountNoExpired());
        }
        if(user.isCredentialNoExpired() != existingUser.isCredentialNoExpired()) {
            existingUser.setEnabled(user.isCredentialNoExpired());
        }
        return super.save(existingUser);
    }

    @Override
    public boolean existsByUniqueProperties(User user) {
        return existsByUsername(user.getUsername());
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean existsByUsername(String username) {
        if (username == null || username.isBlank()) return false;
        return (Long) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM users WHERE username = :username")
                .setParameter("username", username)
                .getResultList().stream().findFirst().orElse(0L) > 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<User> findUsersByRoleId(UUID id) {
        if (id == null) return List.of();
        return (List<User>) entityManager.createNativeQuery(
                "SELECT u.* FROM users u INNER JOIN user_roles ur ON u.user_id = ur.user_id " +
                        "WHERE ur.role_id = :roleId", User.class)
                .setParameter("roleId", id).getResultList();
    }

}
