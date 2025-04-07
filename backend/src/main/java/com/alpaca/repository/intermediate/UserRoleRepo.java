package com.alpaca.repository.intermediate;

import com.alpaca.entity.intermediate.UserRole;
import com.alpaca.repository.GenericRepo;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRoleRepo extends GenericRepo<UserRole, UUID> {}
