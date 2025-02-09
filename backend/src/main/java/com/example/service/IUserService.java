package com.example.service;

import com.example.entity.User;

import java.util.UUID;

public interface IUserService extends IGenericService<User, UUID> {
    User findByEmail(String email);
    User register(User user);
    boolean existsByEmail(String email);
}
