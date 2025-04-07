package com.alpaca.repository.intermediate;

import com.alpaca.entity.intermediate.UserRole;
import com.alpaca.repository.GenericRepo;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleRepo extends GenericRepo<UserRole, UUID> {}
