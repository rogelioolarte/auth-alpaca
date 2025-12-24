package com.alpaca.repository;

import com.alpaca.entity.RolePermission;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository interface for managing {@link RolePermission} entities.
 *
 * <p>Extends {@link GenericRepo} to inherit common CRUD operations and defines additional queries
 * for rolePermission-specific operations.
 *
 * @see GenericRepo
 */
@Repository
public interface RolePermissionRepo extends GenericRepo<RolePermission, UUID> {}
