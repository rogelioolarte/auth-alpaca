package com.alpaca.repository;

import com.alpaca.entity.RolePermission;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository interface for managing {@link RolePermission} entities.
 *
 * <p>Extends {@link CustomRepo} to inherit common CRUD operations and defines additional queries
 * for rolePermission-specific operations.
 *
 * @see CustomRepo
 */
@Repository
public interface RolePermissionRepo extends CustomRepo<RolePermission, UUID> {}
