package com.example.repository.intermediate;

import com.example.entity.intermediate.UserRole;
import com.example.repository.GenericRepo;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRoleRepo extends GenericRepo<UserRole, UUID> {
}
