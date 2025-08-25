package com.alpaca.repository;

import com.alpaca.entity.RolePermission;
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
