package com.example.persistence;

import com.example.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IUserDAO extends IGenericDAO<User, UUID> {

    List<User> findUsersByRoleId(UUID id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
