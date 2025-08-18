package com.alpaca.repository.intermediate;

import com.alpaca.entity.intermediate.UserRole;
import com.alpaca.repository.GenericRepo;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link UserRole} entities.
 *
 * <p>Extends {@link GenericRepo} to inherit common CRUD operations and defines additional queries
 * for user-specific operations.
 *
 * @see GenericRepo
 */
@Repository
public interface UserRoleRepo extends GenericRepo<UserRole, UUID> {}
