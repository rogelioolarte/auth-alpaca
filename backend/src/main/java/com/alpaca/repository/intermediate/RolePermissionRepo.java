package com.alpaca.repository.intermediate;

import com.alpaca.entity.intermediate.RolePermission;
import com.alpaca.repository.GenericRepo;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link RolePermission} entities.
 *
 * <p>Extends {@link GenericRepo} to inherit common CRUD operations and defines additional queries
 * for user-specific operations.
 *
 * @see GenericRepo
 */
@Repository
public interface RolePermissionRepo extends GenericRepo<RolePermission, UUID> {}
