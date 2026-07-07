package com.alpaca.repository;

import com.alpaca.entity.UserRole;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link UserRole} entities.
 *
 * <p>Extends {@link CustomRepo} to inherit common CRUD operations and defines additional queries
 * for userRole-specific operations.
 *
 * @see CustomRepo
 */
@Repository
public interface UserRoleRepo extends CustomRepo<UserRole, UUID> {}
