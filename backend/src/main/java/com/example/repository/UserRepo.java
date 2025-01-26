package com.example.repository;

import com.example.entity.User;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepo extends GenericRepo<User, UUID> {
}
